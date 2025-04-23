package net.dv8tion.jda.internal.audio;

import com.iwebpp.crypto.TweetNaclFast;
import com.neovisionaries.ws.client.WebSocket;
import com.sun.jna.ptr.PointerByReference;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.audio.AudioNatives;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import net.dv8tion.jda.api.audio.OpusPacket;
import net.dv8tion.jda.api.audio.SpeakingMode;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.api.audio.factory.IAudioSendSystem;
import net.dv8tion.jda.api.audio.factory.IPacketProvider;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.managers.AudioManagerImpl;
import net.dv8tion.jda.internal.utils.IOUtil;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;
import tomp2p.opuswrapper.Opus;

public class AudioConnection {
   public static final Logger LOG = JDALogger.getLog(AudioConnection.class);
   public static final long MAX_UINT_32 = 4294967295L;
   private static final int NOT_SPEAKING = 0;
   private static final ByteBuffer silenceBytes = ByteBuffer.wrap(new byte[]{-8, -1, -2});
   private static boolean printedError = false;
   protected volatile DatagramSocket udpSocket;
   private final TIntLongMap ssrcMap = new TIntLongHashMap();
   private final TIntObjectMap<Decoder> opusDecoders = new TIntObjectHashMap();
   private final HashMap<User, Queue<AudioConnection.AudioData>> combinedQueue = new HashMap();
   private final String threadIdentifier;
   private final AudioWebSocket webSocket;
   private final JDAImpl api;
   private VoiceChannel channel;
   private PointerByReference opusEncoder;
   private ScheduledExecutorService combinedAudioExecutor;
   private IAudioSendSystem sendSystem;
   private Thread receiveThread;
   private long queueTimeout;
   private boolean sentSilenceOnConnect = false;
   private int speakingDelay = 10;
   private volatile AudioSendHandler sendHandler = null;
   private volatile AudioReceiveHandler receiveHandler = null;
   private volatile boolean couldReceive = false;
   private volatile boolean speaking = false;
   private volatile int speakingMode;
   private volatile int silenceCounter;

   public AudioConnection(AudioManagerImpl manager, String endpoint, String sessionId, String token, VoiceChannel channel) {
      this.speakingMode = SpeakingMode.VOICE.getRaw();
      this.silenceCounter = 0;
      this.api = (JDAImpl)channel.getJDA();
      this.channel = channel;
      JDAImpl api = (JDAImpl)channel.getJDA();
      this.threadIdentifier = api.getIdentifierString() + " AudioConnection Guild: " + channel.getGuild().getId();
      this.webSocket = new AudioWebSocket(this, manager.getListenerProxy(), endpoint, channel.getGuild(), sessionId, token, manager.isAutoReconnect());
   }

   public void startConnection() {
      this.webSocket.startConnection();
   }

   public ConnectionStatus getConnectionStatus() {
      return this.webSocket.getConnectionStatus();
   }

   public void setAutoReconnect(boolean shouldReconnect) {
      this.webSocket.setAutoReconnect(shouldReconnect);
   }

   public void setSpeakingDelay(int millis) {
      this.speakingDelay = Math.max(millis / 20, 10);
   }

   public void setSendingHandler(AudioSendHandler handler) {
      this.sendHandler = handler;
      if (this.webSocket.isReady()) {
         this.setupSendSystem();
      }

   }

   public void setReceivingHandler(AudioReceiveHandler handler) {
      this.receiveHandler = handler;
      if (this.webSocket.isReady()) {
         this.setupReceiveSystem();
      }

   }

   public void setSpeakingMode(EnumSet<SpeakingMode> mode) {
      int raw = SpeakingMode.getRaw((Collection)mode);
      if (raw != this.speakingMode && this.speaking) {
         this.setSpeaking(raw);
      }

      this.speakingMode = raw;
   }

   public void setQueueTimeout(long queueTimeout) {
      this.queueTimeout = queueTimeout;
   }

   public VoiceChannel getChannel() {
      return this.channel;
   }

   public void setChannel(VoiceChannel channel) {
      this.channel = channel;
   }

   public JDAImpl getJDA() {
      return this.api;
   }

   public Guild getGuild() {
      return this.getChannel().getGuild();
   }

   public void close(ConnectionStatus closeStatus) {
      this.shutdown();
      this.webSocket.close(closeStatus);
   }

   public synchronized void shutdown() {
      if (this.sendSystem != null) {
         this.sendSystem.shutdown();
         this.sendSystem = null;
      }

      if (this.receiveThread != null) {
         this.receiveThread.interrupt();
         this.receiveThread = null;
      }

      if (this.combinedAudioExecutor != null) {
         this.combinedAudioExecutor.shutdownNow();
         this.combinedAudioExecutor = null;
      }

      if (this.opusEncoder != null) {
         Opus.INSTANCE.opus_encoder_destroy(this.opusEncoder);
         this.opusEncoder = null;
      }

      this.opusDecoders.valueCollection().forEach(Decoder::close);
      this.opusDecoders.clear();
   }

   public WebSocket getWebSocket() {
      return this.webSocket.socket;
   }

   protected void prepareReady() {
      Thread readyThread = new Thread(() -> {
         this.getJDA().setContext();
         long timeout = this.getGuild().getAudioManager().getConnectTimeout();
         long started = System.currentTimeMillis();

         while(!this.webSocket.isReady() && (timeout <= 0L || System.currentTimeMillis() - started <= timeout)) {
            try {
               Thread.sleep(10L);
            } catch (InterruptedException var6) {
               LOG.error((String)"AudioConnection ready thread got interrupted while sleeping", (Throwable)var6);
               Thread.currentThread().interrupt();
            }
         }

         if (this.webSocket.isReady()) {
            this.setupSendSystem();
            this.setupReceiveSystem();
         } else {
            this.webSocket.close(ConnectionStatus.ERROR_CONNECTION_TIMEOUT);
         }

      });
      readyThread.setUncaughtExceptionHandler((thread, throwable) -> {
         LOG.error("Uncaught exception in Audio ready-thread", throwable);
         JDAImpl api = this.getJDA();
         api.handleEvent(new ExceptionEvent(api, throwable, true));
      });
      readyThread.setDaemon(true);
      readyThread.setName(this.threadIdentifier + " Ready Thread");
      readyThread.start();
   }

   protected void removeUserSSRC(long userId) {
      AtomicInteger ssrcRef = new AtomicInteger(0);
      boolean modified = this.ssrcMap.retainEntries((ssrc, id) -> {
         boolean isEntry = id == userId;
         if (isEntry) {
            ssrcRef.set(ssrc);
         }

         return !isEntry;
      });
      if (modified) {
         Decoder decoder = (Decoder)this.opusDecoders.remove(ssrcRef.get());
         if (decoder != null) {
            decoder.close();
         }

      }
   }

   protected void updateUserSSRC(int ssrc, long userId) {
      if (this.ssrcMap.containsKey(ssrc)) {
         long previousId = this.ssrcMap.get(ssrc);
         if (previousId != userId) {
            LOG.error("Yeah.. So.. JDA received a UserSSRC update for an ssrc that already had a User set. Inform DV8FromTheWorld.\nChannelId: {} SSRC: {} oldId: {} newId: {}", this.channel.getId(), ssrc, previousId, userId);
         }
      } else {
         this.ssrcMap.put(ssrc, userId);
         if (this.receiveThread != null && AudioNatives.ensureOpus()) {
            this.opusDecoders.put(ssrc, new Decoder(ssrc));
         }
      }

   }

   private synchronized void setupSendSystem() {
      if (this.udpSocket != null && !this.udpSocket.isClosed() && this.sendHandler != null && this.sendSystem == null) {
         IAudioSendFactory factory = this.getJDA().getAudioSendFactory();
         this.sendSystem = factory.createSendSystem(new AudioConnection.PacketProvider(new TweetNaclFast.SecretBox(this.webSocket.getSecretKey())));
         this.sendSystem.setContextMap(this.getJDA().getContextMap());
         this.sendSystem.start();
      } else if (this.sendHandler == null && this.sendSystem != null) {
         this.sendSystem.shutdown();
         this.sendSystem = null;
         if (this.opusEncoder != null) {
            Opus.INSTANCE.opus_encoder_destroy(this.opusEncoder);
            this.opusEncoder = null;
         }
      }

   }

   private synchronized void setupReceiveSystem() {
      if (this.udpSocket != null && !this.udpSocket.isClosed() && this.receiveHandler != null && this.receiveThread == null) {
         this.setupReceiveThread();
      } else if (this.receiveHandler == null && this.receiveThread != null) {
         this.receiveThread.interrupt();
         this.receiveThread = null;
         if (this.combinedAudioExecutor != null) {
            this.combinedAudioExecutor.shutdownNow();
            this.combinedAudioExecutor = null;
         }

         this.opusDecoders.valueCollection().forEach(Decoder::close);
         this.opusDecoders.clear();
      } else if (this.receiveHandler != null && !this.receiveHandler.canReceiveCombined() && this.combinedAudioExecutor != null) {
         this.combinedAudioExecutor.shutdownNow();
         this.combinedAudioExecutor = null;
      }

   }

   private synchronized void setupReceiveThread() {
      if (this.receiveThread == null) {
         this.receiveThread = new Thread(() -> {
            this.getJDA().setContext();

            try {
               this.udpSocket.setSoTimeout(1000);
            } catch (SocketException var13) {
               LOG.error((String)"Couldn't set SO_TIMEOUT for UDP socket", (Throwable)var13);
            }

            while(!this.udpSocket.isClosed() && !Thread.currentThread().isInterrupted()) {
               DatagramPacket receivedPacket = new DatagramPacket(new byte[1920], 1920);

               try {
                  this.udpSocket.receive(receivedPacket);
                  boolean shouldDecode = this.receiveHandler != null && (this.receiveHandler.canReceiveUser() || this.receiveHandler.canReceiveCombined());
                  boolean canReceive = this.receiveHandler != null && (this.receiveHandler.canReceiveUser() || this.receiveHandler.canReceiveCombined() || this.receiveHandler.canReceiveEncoded());
                  if (canReceive && this.webSocket.getSecretKey() != null) {
                     if (!this.couldReceive) {
                        this.couldReceive = true;
                        this.sendSilentPackets();
                     }

                     AudioPacket decryptedPacket = AudioPacket.decryptAudioPacket(this.webSocket.encryption, receivedPacket, this.webSocket.getSecretKey());
                     if (decryptedPacket != null) {
                        int ssrc = decryptedPacket.getSSRC();
                        long userId = this.ssrcMap.get(ssrc);
                        Decoder decoder = (Decoder)this.opusDecoders.get(ssrc);
                        if (userId == this.ssrcMap.getNoEntryValue()) {
                           ByteBuffer audio = decryptedPacket.getEncodedAudio();
                           if (!audio.equals(silenceBytes)) {
                              LOG.debug("Received audio data with an unknown SSRC id. Ignoring");
                           }
                        } else {
                           if (decoder == null) {
                              if (AudioNatives.ensureOpus()) {
                                 this.opusDecoders.put(ssrc, decoder = new Decoder(ssrc));
                              } else if (!this.receiveHandler.canReceiveEncoded()) {
                                 LOG.error("Unable to decode audio due to missing opus binaries!");
                                 break;
                              }
                           }

                           OpusPacket opusPacket = new OpusPacket(decryptedPacket, userId, decoder);
                           if (this.receiveHandler.canReceiveEncoded()) {
                              this.receiveHandler.handleEncodedAudio(opusPacket);
                           }

                           if (shouldDecode && opusPacket.canDecode()) {
                              User user = this.getJDA().getUserById(userId);
                              if (user == null) {
                                 LOG.warn("Received audio data with a known SSRC, but the userId associate with the SSRC is unknown to JDA!");
                              } else {
                                 short[] decodedAudio = opusPacket.decode();
                                 if (decodedAudio != null) {
                                    if (this.receiveHandler.canReceiveUser()) {
                                       this.receiveHandler.handleUserAudio(new UserAudio(user, decodedAudio));
                                    }

                                    if (this.receiveHandler.canReceiveCombined() && this.receiveHandler.includeUserInCombinedAudio(user)) {
                                       Queue<AudioConnection.AudioData> queue = (Queue)this.combinedQueue.get(user);
                                       if (queue == null) {
                                          queue = new ConcurrentLinkedQueue();
                                          this.combinedQueue.put(user, queue);
                                       }

                                       ((Queue)queue).add(new AudioConnection.AudioData(decodedAudio));
                                    }
                                 }
                              }
                           }
                        }
                     }
                  } else if (this.couldReceive) {
                     this.couldReceive = false;
                     this.sendSilentPackets();
                  }
               } catch (SocketTimeoutException var14) {
               } catch (SocketException var15) {
               } catch (Exception var16) {
                  LOG.error((String)"There was some random exception while waiting for udp packets", (Throwable)var16);
               }
            }

         });
         this.receiveThread.setUncaughtExceptionHandler((thread, throwable) -> {
            LOG.error("There was some uncaught exception in the audio receive thread", throwable);
            JDAImpl api = this.getJDA();
            api.handleEvent(new ExceptionEvent(api, throwable, true));
         });
         this.receiveThread.setDaemon(true);
         this.receiveThread.setName(this.threadIdentifier + " Receiving Thread");
         this.receiveThread.start();
      }

      if (this.receiveHandler.canReceiveCombined()) {
         this.setupCombinedExecutor();
      }

   }

   private synchronized void setupCombinedExecutor() {
      if (this.combinedAudioExecutor == null) {
         this.combinedAudioExecutor = Executors.newSingleThreadScheduledExecutor((task) -> {
            Thread t = new Thread(task, this.threadIdentifier + " Combined Thread");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, throwable) -> {
               LOG.error("I have no idea how, but there was an uncaught exception in the combinedAudioExecutor", throwable);
               JDAImpl api = this.getJDA();
               api.handleEvent(new ExceptionEvent(api, throwable, true));
            });
            return t;
         });
         this.combinedAudioExecutor.scheduleAtFixedRate(() -> {
            this.getJDA().setContext();

            try {
               List<User> users = new LinkedList();
               List<short[]> audioParts = new LinkedList();
               if (this.receiveHandler != null && this.receiveHandler.canReceiveCombined()) {
                  long currentTime = System.currentTimeMillis();
                  Iterator var5 = this.combinedQueue.entrySet().iterator();

                  while(true) {
                     User user;
                     Queue queue;
                     do {
                        if (!var5.hasNext()) {
                           if (!audioParts.isEmpty()) {
                              int audioLength = audioParts.stream().mapToInt((it) -> {
                                 return it.length;
                              }).max().getAsInt();
                              short[] mix = new short[1920];

                              for(int i = 0; i < audioLength; ++i) {
                                 int sample = 0;
                                 Iterator iterator = audioParts.iterator();

                                 while(iterator.hasNext()) {
                                    short[] audio = (short[])iterator.next();
                                    if (i < audio.length) {
                                       sample += audio[i];
                                    } else {
                                       iterator.remove();
                                    }
                                 }

                                 if (sample > 32767) {
                                    mix[i] = 32767;
                                 } else if (sample < -32768) {
                                    mix[i] = -32768;
                                 } else {
                                    mix[i] = (short)sample;
                                 }
                              }

                              this.receiveHandler.handleCombinedAudio(new CombinedAudio(users, mix));
                           } else {
                              this.receiveHandler.handleCombinedAudio(new CombinedAudio(Collections.emptyList(), new short[1920]));
                           }

                           return;
                        }

                        Entry<User, Queue<AudioConnection.AudioData>> entry = (Entry)var5.next();
                        user = (User)entry.getKey();
                        queue = (Queue)entry.getValue();
                     } while(queue.isEmpty());

                     AudioConnection.AudioData audioData;
                     for(audioData = (AudioConnection.AudioData)queue.poll(); audioData != null && currentTime - audioData.time > this.queueTimeout; audioData = (AudioConnection.AudioData)queue.poll()) {
                     }

                     if (audioData != null) {
                        users.add(user);
                        audioParts.add(audioData.data);
                     }
                  }
               }
            } catch (Exception var11) {
               LOG.error((String)"There was some unexpected exception in the combinedAudioExecutor!", (Throwable)var11);
            }

         }, 0L, 20L, TimeUnit.MILLISECONDS);
      }

   }

   private ByteBuffer encodeToOpus(ByteBuffer rawAudio) {
      ShortBuffer nonEncodedBuffer = ShortBuffer.allocate(rawAudio.remaining() / 2);
      ByteBuffer encoded = ByteBuffer.allocate(4096);

      int result;
      for(result = rawAudio.position(); result < rawAudio.limit(); result += 2) {
         int firstByte = 255 & rawAudio.get(result);
         int secondByte = 255 & rawAudio.get(result + 1);
         short toShort = (short)(firstByte << 8 | secondByte);
         nonEncodedBuffer.put(toShort);
      }

      nonEncodedBuffer.flip();
      result = Opus.INSTANCE.opus_encode(this.opusEncoder, nonEncodedBuffer, 960, encoded, encoded.capacity());
      if (result <= 0) {
         LOG.error((String)"Received error code from opus_encode(...): {}", (Object)result);
         return null;
      } else {
         encoded.position(0).limit(result);
         return encoded;
      }
   }

   private void setSpeaking(int raw) {
      this.speaking = raw != 0;
      DataObject obj = DataObject.empty().put("speaking", raw).put("ssrc", this.webSocket.getSSRC()).put("delay", 0);
      this.webSocket.send(5, obj);
   }

   private void sendSilentPackets() {
      this.silenceCounter = 0;
   }

   protected void finalize() {
      this.shutdown();
   }

   private static class AudioData {
      private final long time = System.currentTimeMillis();
      private final short[] data;

      public AudioData(short[] data) {
         this.data = data;
      }
   }

   private class PacketProvider implements IPacketProvider {
      private char seq = 0;
      private int timestamp = 0;
      private TweetNaclFast.SecretBox boxer;
      private long nonce = 0L;
      private ByteBuffer buffer = ByteBuffer.allocate(512);
      private ByteBuffer encryptionBuffer = ByteBuffer.allocate(512);
      private final byte[] nonceBuffer = new byte[24];

      public PacketProvider(TweetNaclFast.SecretBox boxer) {
         this.boxer = boxer;
      }

      @Nonnull
      public String getIdentifier() {
         return AudioConnection.this.threadIdentifier;
      }

      @Nonnull
      public VoiceChannel getConnectedChannel() {
         return AudioConnection.this.getChannel();
      }

      @Nonnull
      public DatagramSocket getUdpSocket() {
         return AudioConnection.this.udpSocket;
      }

      @Nonnull
      public InetSocketAddress getSocketAddress() {
         return AudioConnection.this.webSocket.getAddress();
      }

      public DatagramPacket getNextPacket(boolean changeTalking) {
         ByteBuffer buffer = this.getNextPacketRaw(changeTalking);
         return buffer == null ? null : this.getDatagramPacket(buffer);
      }

      public ByteBuffer getNextPacketRaw(boolean changeTalking) {
         ByteBuffer nextPacket = null;

         try {
            if (AudioConnection.this.sentSilenceOnConnect && AudioConnection.this.sendHandler != null && AudioConnection.this.sendHandler.canProvide()) {
               AudioConnection.this.silenceCounter = -1;
               ByteBuffer rawAudio = AudioConnection.this.sendHandler.provide20MsAudio();
               if (rawAudio != null && !rawAudio.hasArray()) {
                  AudioConnection.LOG.error("AudioSendHandler provided ByteBuffer without a backing array! This is unsupported.");
               }

               if (rawAudio != null && rawAudio.hasRemaining() && rawAudio.hasArray()) {
                  label89: {
                     if (!AudioConnection.this.sendHandler.isOpus()) {
                        rawAudio = this.encodeAudio(rawAudio);
                        if (rawAudio == null) {
                           break label89;
                        }
                     }

                     nextPacket = this.getPacketData(rawAudio);
                     if (!AudioConnection.this.speaking) {
                        AudioConnection.this.setSpeaking(AudioConnection.this.speakingMode);
                     }

                     if (this.seq + 1 > 65535) {
                        this.seq = 0;
                     } else {
                        ++this.seq;
                     }
                  }
               } else if (AudioConnection.this.speaking && changeTalking) {
                  AudioConnection.this.sendSilentPackets();
               }
            } else if (AudioConnection.this.silenceCounter > -1) {
               nextPacket = this.getPacketData(AudioConnection.silenceBytes);
               if (this.seq + 1 > 65535) {
                  this.seq = 0;
               } else {
                  ++this.seq;
               }

               AudioConnection.this.silenceCounter++;
               if (!AudioConnection.this.sentSilenceOnConnect && AudioConnection.this.silenceCounter > 10 || AudioConnection.this.silenceCounter > AudioConnection.this.speakingDelay) {
                  if (AudioConnection.this.sentSilenceOnConnect) {
                     AudioConnection.this.setSpeaking(0);
                  }

                  AudioConnection.this.silenceCounter = -1;
                  AudioConnection.this.sentSilenceOnConnect = true;
               }
            } else if (AudioConnection.this.speaking && changeTalking) {
               AudioConnection.this.sendSilentPackets();
            }
         } catch (Exception var4) {
            AudioConnection.LOG.error((String)"There was an error while getting next audio packet", (Throwable)var4);
         }

         if (nextPacket != null) {
            this.timestamp += 960;
         }

         return nextPacket;
      }

      private ByteBuffer encodeAudio(ByteBuffer rawAudio) {
         if (AudioConnection.this.opusEncoder == null) {
            if (!AudioNatives.ensureOpus()) {
               if (!AudioConnection.printedError) {
                  AudioConnection.LOG.error("Unable to process PCM audio without opus binaries!");
               }

               AudioConnection.printedError = true;
               return null;
            }

            IntBuffer error = IntBuffer.allocate(1);
            AudioConnection.this.opusEncoder = Opus.INSTANCE.opus_encoder_create(48000, 2, 2049, error);
            if (error.get() != 0 && AudioConnection.this.opusEncoder == null) {
               AudioConnection.LOG.error((String)"Received error status from opus_encoder_create(...): {}", (Object)error.get());
               return null;
            }
         }

         return AudioConnection.this.encodeToOpus(rawAudio);
      }

      private DatagramPacket getDatagramPacket(ByteBuffer b) {
         byte[] data = b.array();
         int offset = b.arrayOffset() + b.position();
         int length = b.remaining();
         return new DatagramPacket(data, offset, length, AudioConnection.this.webSocket.getAddress());
      }

      private ByteBuffer getPacketData(ByteBuffer rawAudio) {
         this.ensureEncryptionBuffer(rawAudio);
         AudioPacket packet = new AudioPacket(this.encryptionBuffer, this.seq, this.timestamp, AudioConnection.this.webSocket.getSSRC(), rawAudio);
         byte nlen;
         switch(AudioConnection.this.webSocket.encryption) {
         case XSALSA20_POLY1305:
            nlen = 0;
            break;
         case XSALSA20_POLY1305_LITE:
            if (this.nonce >= 4294967295L) {
               this.loadNextNonce(this.nonce = 0L);
            } else {
               this.loadNextNonce(++this.nonce);
            }

            nlen = 4;
            break;
         case XSALSA20_POLY1305_SUFFIX:
            ThreadLocalRandom.current().nextBytes(this.nonceBuffer);
            nlen = 24;
            break;
         default:
            throw new IllegalStateException("Encryption mode [" + AudioConnection.this.webSocket.encryption + "] is not supported!");
         }

         return this.buffer = packet.asEncryptedPacket(this.boxer, this.buffer, this.nonceBuffer, nlen);
      }

      private void ensureEncryptionBuffer(ByteBuffer data) {
         this.encryptionBuffer.clear();
         int currentCapacity = this.encryptionBuffer.remaining();
         int requiredCapacity = 12 + data.remaining();
         if (currentCapacity < requiredCapacity) {
            this.encryptionBuffer = ByteBuffer.allocate(requiredCapacity);
         }

      }

      private void loadNextNonce(long nonce) {
         IOUtil.setIntBigEndian(this.nonceBuffer, 0, (int)nonce);
      }

      public void onConnectionError(@Nonnull ConnectionStatus status) {
         AudioConnection.LOG.warn((String)"IAudioSendSystem reported a connection error of: {}", (Object)status);
         AudioConnection.LOG.warn("Shutting down AudioConnection.");
         AudioConnection.this.webSocket.close(status);
      }

      public void onConnectionLost() {
         AudioConnection.LOG.warn("Closing AudioConnection due to inability to send audio packets.");
         AudioConnection.LOG.warn("Cannot send audio packet because JDA cannot navigate the route to Discord.\nAre you sure you have internet connection? It is likely that you've lost connection.");
         AudioConnection.this.webSocket.close(ConnectionStatus.ERROR_LOST_CONNECTION);
      }
   }
}
