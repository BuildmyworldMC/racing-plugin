package com.github.hornta.racing.messenger.org.reflections.vfs;

import com.github.hornta.racing.messenger.org.reflections.ReflectionsException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Collections;

public class SystemDir implements Vfs.Dir {
   private final File file;

   public SystemDir(File file) {
      if (file == null || file.isDirectory() && file.canRead()) {
         this.file = file;
      } else {
         throw new RuntimeException("cannot use dir " + file);
      }
   }

   public String getPath() {
      return this.file == null ? "/NO-SUCH-DIRECTORY/" : this.file.getPath().replace("\\", "/");
   }

   public Iterable<Vfs.File> getFiles() {
      return (Iterable)(this.file != null && this.file.exists() ? () -> {
         try {
            return Files.walk(this.file.toPath()).filter((x$0) -> {
               return Files.isRegularFile(x$0, new LinkOption[0]);
            }).map((path) -> {
               return new SystemFile(this, path.toFile());
            }).iterator();
         } catch (IOException var2) {
            throw new ReflectionsException("could not get files for " + this.file, var2);
         }
      } : Collections.emptyList());
   }

   public void close() {
   }

   public String toString() {
      return this.getPath();
   }
}
