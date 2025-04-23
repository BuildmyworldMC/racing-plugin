package com.neovisionaries.ws.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

class SNIHelper {
   private static Constructor<?> sSNIHostNameConstructor;
   private static Method sSetServerNamesMethod;

   private static void initialize() throws Exception {
      sSNIHostNameConstructor = Misc.getConstructor("javax.net.ssl.SNIHostName", new Class[]{String.class});
      sSetServerNamesMethod = Misc.getMethod("javax.net.ssl.SSLParameters", "setServerNames", new Class[]{List.class});
   }

   private static Object createSNIHostName(String hostname) {
      return Misc.newInstance(sSNIHostNameConstructor, hostname);
   }

   private static List<Object> createSNIHostNames(String[] hostnames) {
      List<Object> list = new ArrayList(hostnames.length);
      String[] var2 = hostnames;
      int var3 = hostnames.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String hostname = var2[var4];
         list.add(createSNIHostName(hostname));
      }

      return list;
   }

   private static void setServerNames(SSLParameters parameters, String[] hostnames) {
      Misc.invoke(sSetServerNamesMethod, parameters, createSNIHostNames(hostnames));
   }

   static void setServerNames(Socket socket, String[] hostnames) {
      if (socket instanceof SSLSocket) {
         if (hostnames != null) {
            SSLParameters parameters = ((SSLSocket)socket).getSSLParameters();
            if (parameters != null) {
               setServerNames(parameters, hostnames);
            }
         }
      }
   }

   static {
      try {
         initialize();
      } catch (Exception var1) {
         var1.printStackTrace();
      }

   }
}
