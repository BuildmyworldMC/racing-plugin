package com.neovisionaries.ws.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ListenerManager {
   private final WebSocket mWebSocket;
   private final List<WebSocketListener> mListeners = new ArrayList();
   private boolean mSyncNeeded = true;
   private List<WebSocketListener> mCopiedListeners;

   public ListenerManager(WebSocket websocket) {
      this.mWebSocket = websocket;
   }

   public List<WebSocketListener> getListeners() {
      return this.mListeners;
   }

   public void addListener(WebSocketListener listener) {
      if (listener != null) {
         synchronized(this.mListeners) {
            this.mListeners.add(listener);
            this.mSyncNeeded = true;
         }
      }
   }

   public void addListeners(List<WebSocketListener> listeners) {
      if (listeners != null) {
         synchronized(this.mListeners) {
            Iterator var3 = listeners.iterator();

            while(var3.hasNext()) {
               WebSocketListener listener = (WebSocketListener)var3.next();
               if (listener != null) {
                  this.mListeners.add(listener);
                  this.mSyncNeeded = true;
               }
            }

         }
      }
   }

   public void removeListener(WebSocketListener listener) {
      if (listener != null) {
         synchronized(this.mListeners) {
            if (this.mListeners.remove(listener)) {
               this.mSyncNeeded = true;
            }

         }
      }
   }

   public void removeListeners(List<WebSocketListener> listeners) {
      if (listeners != null) {
         synchronized(this.mListeners) {
            Iterator var3 = listeners.iterator();

            while(var3.hasNext()) {
               WebSocketListener listener = (WebSocketListener)var3.next();
               if (listener != null && this.mListeners.remove(listener)) {
                  this.mSyncNeeded = true;
               }
            }

         }
      }
   }

   public void clearListeners() {
      synchronized(this.mListeners) {
         if (this.mListeners.size() != 0) {
            this.mListeners.clear();
            this.mCopiedListeners = null;
            this.mSyncNeeded = true;
         }
      }
   }

   private List<WebSocketListener> getSynchronizedListeners() {
      synchronized(this.mListeners) {
         if (!this.mSyncNeeded) {
            return this.mCopiedListeners;
         } else {
            List<WebSocketListener> copiedListeners = new ArrayList(this.mListeners.size());
            Iterator var3 = this.mListeners.iterator();

            while(var3.hasNext()) {
               WebSocketListener listener = (WebSocketListener)var3.next();
               copiedListeners.add(listener);
            }

            this.mCopiedListeners = copiedListeners;
            this.mSyncNeeded = false;
            return copiedListeners;
         }
      }
   }

   public void callOnStateChanged(WebSocketState newState) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onStateChanged(this.mWebSocket, newState);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnConnected(Map<String, List<String>> headers) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onConnected(this.mWebSocket, headers);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnConnectError(WebSocketException cause) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onConnectError(this.mWebSocket, cause);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnDisconnected(WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
      Iterator var4 = this.getSynchronizedListeners().iterator();

      while(var4.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var4.next();

         try {
            listener.onDisconnected(this.mWebSocket, serverCloseFrame, clientCloseFrame, closedByServer);
         } catch (Throwable var7) {
            this.callHandleCallbackError(listener, var7);
         }
      }

   }

   public void callOnFrame(WebSocketFrame frame) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onFrame(this.mWebSocket, frame);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnContinuationFrame(WebSocketFrame frame) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onContinuationFrame(this.mWebSocket, frame);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnTextFrame(WebSocketFrame frame) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onTextFrame(this.mWebSocket, frame);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnBinaryFrame(WebSocketFrame frame) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onBinaryFrame(this.mWebSocket, frame);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnCloseFrame(WebSocketFrame frame) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onCloseFrame(this.mWebSocket, frame);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnPingFrame(WebSocketFrame frame) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onPingFrame(this.mWebSocket, frame);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnPongFrame(WebSocketFrame frame) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onPongFrame(this.mWebSocket, frame);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnTextMessage(String message) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onTextMessage(this.mWebSocket, message);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnTextMessage(byte[] data) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onTextMessage(this.mWebSocket, data);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnBinaryMessage(byte[] message) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onBinaryMessage(this.mWebSocket, message);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnSendingFrame(WebSocketFrame frame) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onSendingFrame(this.mWebSocket, frame);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnFrameSent(WebSocketFrame frame) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onFrameSent(this.mWebSocket, frame);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnFrameUnsent(WebSocketFrame frame) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onFrameUnsent(this.mWebSocket, frame);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnThreadCreated(ThreadType threadType, Thread thread) {
      Iterator var3 = this.getSynchronizedListeners().iterator();

      while(var3.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var3.next();

         try {
            listener.onThreadCreated(this.mWebSocket, threadType, thread);
         } catch (Throwable var6) {
            this.callHandleCallbackError(listener, var6);
         }
      }

   }

   public void callOnThreadStarted(ThreadType threadType, Thread thread) {
      Iterator var3 = this.getSynchronizedListeners().iterator();

      while(var3.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var3.next();

         try {
            listener.onThreadStarted(this.mWebSocket, threadType, thread);
         } catch (Throwable var6) {
            this.callHandleCallbackError(listener, var6);
         }
      }

   }

   public void callOnThreadStopping(ThreadType threadType, Thread thread) {
      Iterator var3 = this.getSynchronizedListeners().iterator();

      while(var3.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var3.next();

         try {
            listener.onThreadStopping(this.mWebSocket, threadType, thread);
         } catch (Throwable var6) {
            this.callHandleCallbackError(listener, var6);
         }
      }

   }

   public void callOnError(WebSocketException cause) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onError(this.mWebSocket, cause);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   public void callOnFrameError(WebSocketException cause, WebSocketFrame frame) {
      Iterator var3 = this.getSynchronizedListeners().iterator();

      while(var3.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var3.next();

         try {
            listener.onFrameError(this.mWebSocket, cause, frame);
         } catch (Throwable var6) {
            this.callHandleCallbackError(listener, var6);
         }
      }

   }

   public void callOnMessageError(WebSocketException cause, List<WebSocketFrame> frames) {
      Iterator var3 = this.getSynchronizedListeners().iterator();

      while(var3.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var3.next();

         try {
            listener.onMessageError(this.mWebSocket, cause, frames);
         } catch (Throwable var6) {
            this.callHandleCallbackError(listener, var6);
         }
      }

   }

   public void callOnMessageDecompressionError(WebSocketException cause, byte[] compressed) {
      Iterator var3 = this.getSynchronizedListeners().iterator();

      while(var3.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var3.next();

         try {
            listener.onMessageDecompressionError(this.mWebSocket, cause, compressed);
         } catch (Throwable var6) {
            this.callHandleCallbackError(listener, var6);
         }
      }

   }

   public void callOnTextMessageError(WebSocketException cause, byte[] data) {
      Iterator var3 = this.getSynchronizedListeners().iterator();

      while(var3.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var3.next();

         try {
            listener.onTextMessageError(this.mWebSocket, cause, data);
         } catch (Throwable var6) {
            this.callHandleCallbackError(listener, var6);
         }
      }

   }

   public void callOnSendError(WebSocketException cause, WebSocketFrame frame) {
      Iterator var3 = this.getSynchronizedListeners().iterator();

      while(var3.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var3.next();

         try {
            listener.onSendError(this.mWebSocket, cause, frame);
         } catch (Throwable var6) {
            this.callHandleCallbackError(listener, var6);
         }
      }

   }

   public void callOnUnexpectedError(WebSocketException cause) {
      Iterator var2 = this.getSynchronizedListeners().iterator();

      while(var2.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var2.next();

         try {
            listener.onUnexpectedError(this.mWebSocket, cause);
         } catch (Throwable var5) {
            this.callHandleCallbackError(listener, var5);
         }
      }

   }

   private void callHandleCallbackError(WebSocketListener listener, Throwable cause) {
      try {
         listener.handleCallbackError(this.mWebSocket, cause);
      } catch (Throwable var4) {
      }

   }

   public void callOnSendingHandshake(String requestLine, List<String[]> headers) {
      Iterator var3 = this.getSynchronizedListeners().iterator();

      while(var3.hasNext()) {
         WebSocketListener listener = (WebSocketListener)var3.next();

         try {
            listener.onSendingHandshake(this.mWebSocket, requestLine, headers);
         } catch (Throwable var6) {
            this.callHandleCallbackError(listener, var6);
         }
      }

   }
}
