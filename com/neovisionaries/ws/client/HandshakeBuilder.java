package com.neovisionaries.ws.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class HandshakeBuilder {
   private static final String[] CONNECTION_HEADER = new String[]{"Connection", "Upgrade"};
   private static final String[] UPGRADE_HEADER = new String[]{"Upgrade", "websocket"};
   private static final String[] VERSION_HEADER = new String[]{"Sec-WebSocket-Version", "13"};
   private static final String RN = "\r\n";
   private boolean mSecure;
   private String mUserInfo;
   private final String mHost;
   private final String mPath;
   private final URI mUri;
   private String mKey;
   private Set<String> mProtocols;
   private List<WebSocketExtension> mExtensions;
   private List<String[]> mHeaders;

   public HandshakeBuilder(boolean secure, String userInfo, String host, String path) {
      this.mSecure = secure;
      this.mUserInfo = userInfo;
      this.mHost = host;
      this.mPath = path;
      this.mUri = URI.create(String.format("%s://%s%s", secure ? "wss" : "ws", host, path));
   }

   public HandshakeBuilder(HandshakeBuilder source) {
      this.mSecure = source.mSecure;
      this.mUserInfo = source.mUserInfo;
      this.mHost = source.mHost;
      this.mPath = source.mPath;
      this.mUri = source.mUri;
      this.mKey = source.mKey;
      this.mProtocols = copyProtocols(source.mProtocols);
      this.mExtensions = copyExtensions(source.mExtensions);
      this.mHeaders = copyHeaders(source.mHeaders);
   }

   public void addProtocol(String protocol) {
      if (!isValidProtocol(protocol)) {
         throw new IllegalArgumentException("'protocol' must be a non-empty string with characters in the range U+0021 to U+007E not including separator characters.");
      } else {
         synchronized(this) {
            if (this.mProtocols == null) {
               this.mProtocols = new LinkedHashSet();
            }

            this.mProtocols.add(protocol);
         }
      }
   }

   public void removeProtocol(String protocol) {
      if (protocol != null) {
         synchronized(this) {
            if (this.mProtocols != null) {
               this.mProtocols.remove(protocol);
               if (this.mProtocols.size() == 0) {
                  this.mProtocols = null;
               }

            }
         }
      }
   }

   public void clearProtocols() {
      synchronized(this) {
         this.mProtocols = null;
      }
   }

   private static boolean isValidProtocol(String protocol) {
      if (protocol != null && protocol.length() != 0) {
         int len = protocol.length();

         for(int i = 0; i < len; ++i) {
            char ch = protocol.charAt(i);
            if (ch < '!' || '~' < ch || Token.isSeparator(ch)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean containsProtocol(String protocol) {
      synchronized(this) {
         return this.mProtocols == null ? false : this.mProtocols.contains(protocol);
      }
   }

   public void addExtension(WebSocketExtension extension) {
      if (extension != null) {
         synchronized(this) {
            if (this.mExtensions == null) {
               this.mExtensions = new ArrayList();
            }

            this.mExtensions.add(extension);
         }
      }
   }

   public void addExtension(String extension) {
      this.addExtension(WebSocketExtension.parse(extension));
   }

   public void removeExtension(WebSocketExtension extension) {
      if (extension != null) {
         synchronized(this) {
            if (this.mExtensions != null) {
               this.mExtensions.remove(extension);
               if (this.mExtensions.size() == 0) {
                  this.mExtensions = null;
               }

            }
         }
      }
   }

   public void removeExtensions(String name) {
      if (name != null) {
         synchronized(this) {
            if (this.mExtensions != null) {
               List<WebSocketExtension> extensionsToRemove = new ArrayList();
               Iterator var4 = this.mExtensions.iterator();

               WebSocketExtension extension;
               while(var4.hasNext()) {
                  extension = (WebSocketExtension)var4.next();
                  if (extension.getName().equals(name)) {
                     extensionsToRemove.add(extension);
                  }
               }

               var4 = extensionsToRemove.iterator();

               while(var4.hasNext()) {
                  extension = (WebSocketExtension)var4.next();
                  this.mExtensions.remove(extension);
               }

               if (this.mExtensions.size() == 0) {
                  this.mExtensions = null;
               }

            }
         }
      }
   }

   public void clearExtensions() {
      synchronized(this) {
         this.mExtensions = null;
      }
   }

   public boolean containsExtension(WebSocketExtension extension) {
      if (extension == null) {
         return false;
      } else {
         synchronized(this) {
            return this.mExtensions == null ? false : this.mExtensions.contains(extension);
         }
      }
   }

   public boolean containsExtension(String name) {
      if (name == null) {
         return false;
      } else {
         synchronized(this) {
            if (this.mExtensions == null) {
               return false;
            } else {
               Iterator var3 = this.mExtensions.iterator();

               WebSocketExtension extension;
               do {
                  if (!var3.hasNext()) {
                     return false;
                  }

                  extension = (WebSocketExtension)var3.next();
               } while(!extension.getName().equals(name));

               return true;
            }
         }
      }
   }

   public void addHeader(String name, String value) {
      if (name != null && name.length() != 0) {
         if (value == null) {
            value = "";
         }

         synchronized(this) {
            if (this.mHeaders == null) {
               this.mHeaders = new ArrayList();
            }

            this.mHeaders.add(new String[]{name, value});
         }
      }
   }

   public void removeHeaders(String name) {
      if (name != null && name.length() != 0) {
         synchronized(this) {
            if (this.mHeaders != null) {
               List<String[]> headersToRemove = new ArrayList();
               Iterator var4 = this.mHeaders.iterator();

               String[] header;
               while(var4.hasNext()) {
                  header = (String[])var4.next();
                  if (header[0].equals(name)) {
                     headersToRemove.add(header);
                  }
               }

               var4 = headersToRemove.iterator();

               while(var4.hasNext()) {
                  header = (String[])var4.next();
                  this.mHeaders.remove(header);
               }

               if (this.mHeaders.size() == 0) {
                  this.mHeaders = null;
               }

            }
         }
      }
   }

   public void clearHeaders() {
      synchronized(this) {
         this.mHeaders = null;
      }
   }

   public void setUserInfo(String userInfo) {
      synchronized(this) {
         this.mUserInfo = userInfo;
      }
   }

   public void setUserInfo(String id, String password) {
      if (id == null) {
         id = "";
      }

      if (password == null) {
         password = "";
      }

      String userInfo = String.format("%s:%s", id, password);
      this.setUserInfo(userInfo);
   }

   public void clearUserInfo() {
      synchronized(this) {
         this.mUserInfo = null;
      }
   }

   public URI getURI() {
      return this.mUri;
   }

   public void setKey(String key) {
      this.mKey = key;
   }

   public String buildRequestLine() {
      return String.format("GET %s HTTP/1.1", this.mPath);
   }

   public List<String[]> buildHeaders() {
      List<String[]> headers = new ArrayList();
      headers.add(new String[]{"Host", this.mHost});
      headers.add(CONNECTION_HEADER);
      headers.add(UPGRADE_HEADER);
      headers.add(VERSION_HEADER);
      headers.add(new String[]{"Sec-WebSocket-Key", this.mKey});
      if (this.mProtocols != null && this.mProtocols.size() != 0) {
         headers.add(new String[]{"Sec-WebSocket-Protocol", Misc.join(this.mProtocols, ", ")});
      }

      if (this.mExtensions != null && this.mExtensions.size() != 0) {
         headers.add(new String[]{"Sec-WebSocket-Extensions", Misc.join(this.mExtensions, ", ")});
      }

      if (this.mUserInfo != null && this.mUserInfo.length() != 0) {
         headers.add(new String[]{"Authorization", "Basic " + Base64.encode(this.mUserInfo)});
      }

      if (this.mHeaders != null && this.mHeaders.size() != 0) {
         headers.addAll(this.mHeaders);
      }

      return headers;
   }

   public static String build(String requestLine, List<String[]> headers) {
      StringBuilder builder = new StringBuilder();
      builder.append(requestLine).append("\r\n");
      Iterator var3 = headers.iterator();

      while(var3.hasNext()) {
         String[] header = (String[])var3.next();
         builder.append(header[0]).append(": ").append(header[1]).append("\r\n");
      }

      builder.append("\r\n");
      return builder.toString();
   }

   private static Set<String> copyProtocols(Set<String> protocols) {
      if (protocols == null) {
         return null;
      } else {
         Set<String> newProtocols = new LinkedHashSet(protocols.size());
         newProtocols.addAll(protocols);
         return newProtocols;
      }
   }

   private static List<WebSocketExtension> copyExtensions(List<WebSocketExtension> extensions) {
      if (extensions == null) {
         return null;
      } else {
         List<WebSocketExtension> newExtensions = new ArrayList(extensions.size());
         Iterator var2 = extensions.iterator();

         while(var2.hasNext()) {
            WebSocketExtension extension = (WebSocketExtension)var2.next();
            newExtensions.add(new WebSocketExtension(extension));
         }

         return newExtensions;
      }
   }

   private static List<String[]> copyHeaders(List<String[]> headers) {
      if (headers == null) {
         return null;
      } else {
         List<String[]> newHeaders = new ArrayList(headers.size());
         Iterator var2 = headers.iterator();

         while(var2.hasNext()) {
            String[] header = (String[])var2.next();
            newHeaders.add(copyHeader(header));
         }

         return newHeaders;
      }
   }

   private static String[] copyHeader(String[] header) {
      String[] newHeader = new String[]{header[0], header[1]};
      return newHeader;
   }
}
