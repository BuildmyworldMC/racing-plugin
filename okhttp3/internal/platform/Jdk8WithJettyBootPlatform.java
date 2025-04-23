package okhttp3.internal.platform;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocket;
import okhttp3.Protocol;
import okhttp3.internal.Util;

class Jdk8WithJettyBootPlatform extends Platform {
   private final Method putMethod;
   private final Method getMethod;
   private final Method removeMethod;
   private final Class<?> clientProviderClass;
   private final Class<?> serverProviderClass;

   Jdk8WithJettyBootPlatform(Method putMethod, Method getMethod, Method removeMethod, Class<?> clientProviderClass, Class<?> serverProviderClass) {
      this.putMethod = putMethod;
      this.getMethod = getMethod;
      this.removeMethod = removeMethod;
      this.clientProviderClass = clientProviderClass;
      this.serverProviderClass = serverProviderClass;
   }

   public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols) {
      List names = alpnProtocolNames(protocols);

      try {
         Object alpnProvider = Proxy.newProxyInstance(Platform.class.getClassLoader(), new Class[]{this.clientProviderClass, this.serverProviderClass}, new Jdk8WithJettyBootPlatform.AlpnProvider(names));
         this.putMethod.invoke((Object)null, sslSocket, alpnProvider);
      } catch (IllegalAccessException | InvocationTargetException var6) {
         throw new AssertionError("failed to set ALPN", var6);
      }
   }

   public void afterHandshake(SSLSocket sslSocket) {
      try {
         this.removeMethod.invoke((Object)null, sslSocket);
      } catch (InvocationTargetException | IllegalAccessException var3) {
         throw new AssertionError("failed to remove ALPN", var3);
      }
   }

   @Nullable
   public String getSelectedProtocol(SSLSocket socket) {
      try {
         Jdk8WithJettyBootPlatform.AlpnProvider provider = (Jdk8WithJettyBootPlatform.AlpnProvider)Proxy.getInvocationHandler(this.getMethod.invoke((Object)null, socket));
         if (!provider.unsupported && provider.selected == null) {
            Platform.get().log(4, "ALPN callback dropped: HTTP/2 is disabled. Is alpn-boot on the boot class path?", (Throwable)null);
            return null;
         } else {
            return provider.unsupported ? null : provider.selected;
         }
      } catch (IllegalAccessException | InvocationTargetException var3) {
         throw new AssertionError("failed to get ALPN selected protocol", var3);
      }
   }

   public static Platform buildIfSupported() {
      try {
         String alpnClassName = "org.eclipse.jetty.alpn.ALPN";
         Class<?> alpnClass = Class.forName(alpnClassName);
         Class<?> providerClass = Class.forName(alpnClassName + "$Provider");
         Class<?> clientProviderClass = Class.forName(alpnClassName + "$ClientProvider");
         Class<?> serverProviderClass = Class.forName(alpnClassName + "$ServerProvider");
         Method putMethod = alpnClass.getMethod("put", SSLSocket.class, providerClass);
         Method getMethod = alpnClass.getMethod("get", SSLSocket.class);
         Method removeMethod = alpnClass.getMethod("remove", SSLSocket.class);
         return new Jdk8WithJettyBootPlatform(putMethod, getMethod, removeMethod, clientProviderClass, serverProviderClass);
      } catch (NoSuchMethodException | ClassNotFoundException var8) {
         return null;
      }
   }

   private static class AlpnProvider implements InvocationHandler {
      private final List<String> protocols;
      boolean unsupported;
      String selected;

      AlpnProvider(List<String> protocols) {
         this.protocols = protocols;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         String methodName = method.getName();
         Class<?> returnType = method.getReturnType();
         if (args == null) {
            args = Util.EMPTY_STRING_ARRAY;
         }

         if (methodName.equals("supports") && Boolean.TYPE == returnType) {
            return true;
         } else if (methodName.equals("unsupported") && Void.TYPE == returnType) {
            this.unsupported = true;
            return null;
         } else if (methodName.equals("protocols") && ((Object[])args).length == 0) {
            return this.protocols;
         } else if ((methodName.equals("selectProtocol") || methodName.equals("select")) && String.class == returnType && ((Object[])args).length == 1 && ((Object[])args)[0] instanceof List) {
            List<?> peerProtocols = (List)((Object[])args)[0];
            int i = 0;

            for(int size = peerProtocols.size(); i < size; ++i) {
               String protocol = (String)peerProtocols.get(i);
               if (this.protocols.contains(protocol)) {
                  return this.selected = protocol;
               }
            }

            return this.selected = (String)this.protocols.get(0);
         } else if ((methodName.equals("protocolSelected") || methodName.equals("selected")) && ((Object[])args).length == 1) {
            this.selected = (String)((Object[])args)[0];
            return null;
         } else {
            return method.invoke(this, (Object[])args);
         }
      }
   }
}
