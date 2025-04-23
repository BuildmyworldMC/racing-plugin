package javassist;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class JarClassPath implements ClassPath {
   List<String> jarfileEntries;
   String jarfileURL;

   JarClassPath(String pathname) throws NotFoundException {
      JarFile jarfile = null;

      try {
         jarfile = new JarFile(pathname);
         this.jarfileEntries = new ArrayList();
         Iterator var3 = Collections.list(jarfile.entries()).iterator();

         while(var3.hasNext()) {
            JarEntry je = (JarEntry)var3.next();
            if (je.getName().endsWith(".class")) {
               this.jarfileEntries.add(je.getName());
            }
         }

         this.jarfileURL = (new File(pathname)).getCanonicalFile().toURI().toURL().toString();
         return;
      } catch (IOException var13) {
      } finally {
         if (null != jarfile) {
            try {
               jarfile.close();
            } catch (IOException var12) {
            }
         }

      }

      throw new NotFoundException(pathname);
   }

   public InputStream openClassfile(String classname) throws NotFoundException {
      URL jarURL = this.find(classname);
      if (null != jarURL) {
         try {
            if (ClassPool.cacheOpenedJarFile) {
               return jarURL.openConnection().getInputStream();
            } else {
               URLConnection con = jarURL.openConnection();
               con.setUseCaches(false);
               return con.getInputStream();
            }
         } catch (IOException var4) {
            throw new NotFoundException("broken jar file?: " + classname);
         }
      } else {
         return null;
      }
   }

   public URL find(String classname) {
      String jarname = classname.replace('.', '/') + ".class";
      if (this.jarfileEntries.contains(jarname)) {
         try {
            return new URL(String.format("jar:%s!/%s", this.jarfileURL, jarname));
         } catch (MalformedURLException var4) {
         }
      }

      return null;
   }

   public String toString() {
      return this.jarfileURL == null ? "<null>" : this.jarfileURL.toString();
   }
}
