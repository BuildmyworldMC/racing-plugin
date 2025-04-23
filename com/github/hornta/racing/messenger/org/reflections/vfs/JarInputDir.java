package com.github.hornta.racing.messenger.org.reflections.vfs;

import com.github.hornta.racing.messenger.org.reflections.ReflectionsException;
import com.github.hornta.racing.messenger.org.reflections.util.Utils;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

public class JarInputDir implements Vfs.Dir {
   private final URL url;
   JarInputStream jarInputStream;
   long cursor = 0L;
   long nextCursor = 0L;

   public JarInputDir(URL url) {
      this.url = url;
   }

   public String getPath() {
      return this.url.getPath();
   }

   public Iterable<Vfs.File> getFiles() {
      return () -> {
         return new Iterator<Vfs.File>() {
            Vfs.File entry;

            {
               try {
                  JarInputDir.this.jarInputStream = new JarInputStream(JarInputDir.this.url.openConnection().getInputStream());
               } catch (Exception var3) {
                  throw new ReflectionsException("Could not open url connection", var3);
               }

               this.entry = null;
            }

            public boolean hasNext() {
               return this.entry != null || (this.entry = this.computeNext()) != null;
            }

            public Vfs.File next() {
               Vfs.File next = this.entry;
               this.entry = null;
               return next;
            }

            private Vfs.File computeNext() {
               while(true) {
                  try {
                     ZipEntry entry = JarInputDir.this.jarInputStream.getNextJarEntry();
                     if (entry == null) {
                        return null;
                     }

                     long size = entry.getSize();
                     if (size < 0L) {
                        size += 4294967295L;
                     }

                     JarInputDir var10000 = JarInputDir.this;
                     var10000.nextCursor += size;
                     if (!entry.isDirectory()) {
                        return new JarInputFile(entry, JarInputDir.this, JarInputDir.this.cursor, JarInputDir.this.nextCursor);
                     }
                  } catch (IOException var4) {
                     throw new ReflectionsException("could not get next zip entry", var4);
                  }
               }
            }
         };
      };
   }

   public void close() {
      Utils.close(this.jarInputStream);
   }
}
