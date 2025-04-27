package com.github.hornta.racing.messenger.org.reflections.vfs;

import com.github.hornta.racing.messenger.org.reflections.Reflections;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ZipDir implements Vfs.Dir {
   final java.util.zip.ZipFile jarFile;

   public ZipDir(JarFile jarFile) {
      this.jarFile = jarFile;
   }

   public String getPath() {
      return this.jarFile.getName();
   }

   public Iterable<Vfs.File> getFiles() {
      return this.jarFile.stream()
              .filter((entry) -> !entry.isDirectory())
              .map((entry) -> new ZipFile(this, entry))
              .collect(Collectors.toUnmodifiableList());
   }

   public void close() {
      try {
         this.jarFile.close();
      } catch (IOException var2) {
         if (Reflections.log != null) {
            Reflections.log.warn((String)"Could not close JarFile", (Throwable)var2);
         }
      }

   }

   public String toString() {
      return this.jarFile.getName();
   }
}
