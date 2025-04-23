package com.neovisionaries.ws.client;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

class SocketConnector {
   private final SocketFactory mSocketFactory;
   private final Address mAddress;
   private final int mConnectionTimeout;
   private final String[] mServerNames;
   private final ProxyHandshaker mProxyHandshaker;
   private final SSLSocketFactory mSSLSocketFactory;
   private final String mHost;
   private final int mPort;
   private DualStackMode mDualStackMode;
   private int mDualStackFallbackDelay;
   private boolean mVerifyHostname;
   private Socket mSocket;

   SocketConnector(SocketFactory socketFactory, Address address, int timeout, String[] serverNames) {
      this(socketFactory, address, timeout, serverNames, (ProxyHandshaker)null, (SSLSocketFactory)null, (String)null, 0);
   }

   SocketConnector(SocketFactory socketFactory, Address address, int timeout, String[] serverNames, ProxyHandshaker handshaker, SSLSocketFactory sslSocketFactory, String host, int port) {
      this.mDualStackMode = DualStackMode.BOTH;
      this.mDualStackFallbackDelay = 250;
      this.mSocketFactory = socketFactory;
      this.mAddress = address;
      this.mConnectionTimeout = timeout;
      this.mServerNames = serverNames;
      this.mProxyHandshaker = handshaker;
      this.mSSLSocketFactory = sslSocketFactory;
      this.mHost = host;
      this.mPort = port;
   }

   public int getConnectionTimeout() {
      return this.mConnectionTimeout;
   }

   public Socket getSocket() {
      return this.mSocket;
   }

   public Socket getConnectedSocket() throws WebSocketException {
      if (this.mSocket == null) {
         this.connectSocket();
      }

      return this.mSocket;
   }

   private void connectSocket() throws WebSocketException {
      SocketInitiator socketInitiator = new SocketInitiator(this.mSocketFactory, this.mAddress, this.mConnectionTimeout, this.mServerNames, this.mDualStackMode, this.mDualStackFallbackDelay);
      InetAddress[] addresses = this.resolveHostname();

      try {
         this.mSocket = socketInitiator.establish(addresses);
      } catch (Exception var6) {
         boolean proxied = this.mProxyHandshaker != null;
         String message = String.format("Failed to connect to %s'%s': %s", proxied ? "the proxy " : "", this.mAddress, var6.getMessage());
         throw new WebSocketException(WebSocketError.SOCKET_CONNECT_ERROR, message, var6);
      }
   }

   private InetAddress[] resolveHostname() throws WebSocketException {
      InetAddress[] addresses = null;
      UnknownHostException exception = null;

      try {
         addresses = InetAddress.getAllByName(this.mAddress.getHostname());
         Arrays.sort(addresses, new Comparator<InetAddress>() {
            public int compare(InetAddress left, InetAddress right) {
               if (left.getClass() == right.getClass()) {
                  return 0;
               } else {
                  return left instanceof Inet6Address ? -1 : 1;
               }
            }
         });
      } catch (UnknownHostException var4) {
         exception = var4;
      }

      if (addresses != null && addresses.length > 0) {
         return addresses;
      } else {
         if (exception == null) {
            exception = new UnknownHostException("No IP addresses found");
         }

         String message = String.format("Failed to resolve hostname %s: %s", this.mAddress, exception.getMessage());
         throw new WebSocketException(WebSocketError.SOCKET_CONNECT_ERROR, message, exception);
      }
   }

   public Socket connect() throws WebSocketException {
      try {
         this.doConnect();

         assert this.mSocket != null;

         return this.mSocket;
      } catch (WebSocketException var4) {
         if (this.mSocket != null) {
            try {
               this.mSocket.close();
            } catch (IOException var3) {
            }
         }

         throw var4;
      }
   }

   SocketConnector setDualStackSettings(DualStackMode mode, int fallbackDelay) {
      this.mDualStackMode = mode;
      this.mDualStackFallbackDelay = fallbackDelay;
      return this;
   }

   SocketConnector setVerifyHostname(boolean verifyHostname) {
      this.mVerifyHostname = verifyHostname;
      return this;
   }

   private void doConnect() throws WebSocketException {
      boolean proxied = this.mProxyHandshaker != null;
      this.connectSocket();

      assert this.mSocket != null;

      if (this.mSocket instanceof SSLSocket) {
         this.verifyHostname((SSLSocket)this.mSocket, this.mAddress.getHostname());
      }

      if (proxied) {
         this.handshake();
      }

   }

   private void verifyHostname(SSLSocket socket, String hostname) throws HostnameUnverifiedException {
      if (this.mVerifyHostname) {
         OkHostnameVerifier verifier = OkHostnameVerifier.INSTANCE;
         SSLSession session = socket.getSession();
         if (!verifier.verify(hostname, session)) {
            throw new HostnameUnverifiedException(socket, hostname);
         }
      }
   }

   private void handshake() throws WebSocketException {
      assert this.mSocket != null;

      String message;
      try {
         this.mProxyHandshaker.perform(this.mSocket);
      } catch (IOException var5) {
         message = String.format("Handshake with the proxy server (%s) failed: %s", this.mAddress, var5.getMessage());
         throw new WebSocketException(WebSocketError.PROXY_HANDSHAKE_ERROR, message, var5);
      }

      if (this.mSSLSocketFactory != null) {
         try {
            this.mSocket = this.mSSLSocketFactory.createSocket(this.mSocket, this.mHost, this.mPort, true);
         } catch (IOException var4) {
            message = "Failed to overlay an existing socket: " + var4.getMessage();
            throw new WebSocketException(WebSocketError.SOCKET_OVERLAY_ERROR, message, var4);
         }

         try {
            ((SSLSocket)this.mSocket).startHandshake();
            this.verifyHostname((SSLSocket)this.mSocket, this.mProxyHandshaker.getProxiedHostname());
         } catch (IOException var3) {
            message = String.format("SSL handshake with the WebSocket endpoint (%s) failed: %s", this.mAddress, var3.getMessage());
            throw new WebSocketException(WebSocketError.SSL_HANDSHAKE_ERROR, message, var3);
         }
      }
   }

   void closeSilently() {
      if (this.mSocket != null) {
         try {
            this.mSocket.close();
         } catch (Throwable var2) {
         }
      }

   }
}
