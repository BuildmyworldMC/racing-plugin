package okhttp3.internal.cache;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import okhttp3.internal.Util;
import okhttp3.internal.io.FileSystem;
import okhttp3.internal.platform.Platform;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import okio.Source;

public final class DiskLruCache implements Closeable, Flushable {
   static final String JOURNAL_FILE = "journal";
   static final String JOURNAL_FILE_TEMP = "journal.tmp";
   static final String JOURNAL_FILE_BACKUP = "journal.bkp";
   static final String MAGIC = "libcore.io.DiskLruCache";
   static final String VERSION_1 = "1";
   static final long ANY_SEQUENCE_NUMBER = -1L;
   static final Pattern LEGAL_KEY_PATTERN = Pattern.compile("[a-z0-9_-]{1,120}");
   private static final String CLEAN = "CLEAN";
   private static final String DIRTY = "DIRTY";
   private static final String REMOVE = "REMOVE";
   private static final String READ = "READ";
   final FileSystem fileSystem;
   final File directory;
   private final File journalFile;
   private final File journalFileTmp;
   private final File journalFileBackup;
   private final int appVersion;
   private long maxSize;
   final int valueCount;
   private long size = 0L;
   BufferedSink journalWriter;
   final LinkedHashMap<String, DiskLruCache.Entry> lruEntries = new LinkedHashMap(0, 0.75F, true);
   int redundantOpCount;
   boolean hasJournalErrors;
   boolean initialized;
   boolean closed;
   boolean mostRecentTrimFailed;
   boolean mostRecentRebuildFailed;
   private long nextSequenceNumber = 0L;
   private final Executor executor;
   private final Runnable cleanupRunnable = new Runnable() {
      public void run() {
         synchronized(DiskLruCache.this) {
            if (!(!DiskLruCache.this.initialized | DiskLruCache.this.closed)) {
               try {
                  DiskLruCache.this.trimToSize();
               } catch (IOException var5) {
                  DiskLruCache.this.mostRecentTrimFailed = true;
               }

               try {
                  if (DiskLruCache.this.journalRebuildRequired()) {
                     DiskLruCache.this.rebuildJournal();
                     DiskLruCache.this.redundantOpCount = 0;
                  }
               } catch (IOException var4) {
                  DiskLruCache.this.mostRecentRebuildFailed = true;
                  DiskLruCache.this.journalWriter = Okio.buffer(Okio.blackhole());
               }

            }
         }
      }
   };

   DiskLruCache(FileSystem fileSystem, File directory, int appVersion, int valueCount, long maxSize, Executor executor) {
      this.fileSystem = fileSystem;
      this.directory = directory;
      this.appVersion = appVersion;
      this.journalFile = new File(directory, "journal");
      this.journalFileTmp = new File(directory, "journal.tmp");
      this.journalFileBackup = new File(directory, "journal.bkp");
      this.valueCount = valueCount;
      this.maxSize = maxSize;
      this.executor = executor;
   }

   public synchronized void initialize() throws IOException {
      assert Thread.holdsLock(this);

      if (!this.initialized) {
         if (this.fileSystem.exists(this.journalFileBackup)) {
            if (this.fileSystem.exists(this.journalFile)) {
               this.fileSystem.delete(this.journalFileBackup);
            } else {
               this.fileSystem.rename(this.journalFileBackup, this.journalFile);
            }
         }

         if (this.fileSystem.exists(this.journalFile)) {
            try {
               this.readJournal();
               this.processJournal();
               this.initialized = true;
               return;
            } catch (IOException var6) {
               Platform.get().log(5, "DiskLruCache " + this.directory + " is corrupt: " + var6.getMessage() + ", removing", var6);

               try {
                  this.delete();
               } finally {
                  this.closed = false;
               }
            }
         }

         this.rebuildJournal();
         this.initialized = true;
      }
   }

   public static DiskLruCache create(FileSystem fileSystem, File directory, int appVersion, int valueCount, long maxSize) {
      if (maxSize <= 0L) {
         throw new IllegalArgumentException("maxSize <= 0");
      } else if (valueCount <= 0) {
         throw new IllegalArgumentException("valueCount <= 0");
      } else {
         Executor executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(), Util.threadFactory("OkHttp DiskLruCache", true));
         return new DiskLruCache(fileSystem, directory, appVersion, valueCount, maxSize, executor);
      }
   }

   private void readJournal() throws IOException {
      BufferedSource source = Okio.buffer(this.fileSystem.source(this.journalFile));
      Throwable var2 = null;

      try {
         String magic = source.readUtf8LineStrict();
         String version = source.readUtf8LineStrict();
         String appVersionString = source.readUtf8LineStrict();
         String valueCountString = source.readUtf8LineStrict();
         String blank = source.readUtf8LineStrict();
         if (!"libcore.io.DiskLruCache".equals(magic) || !"1".equals(version) || !Integer.toString(this.appVersion).equals(appVersionString) || !Integer.toString(this.valueCount).equals(valueCountString) || !"".equals(blank)) {
            throw new IOException("unexpected journal header: [" + magic + ", " + version + ", " + valueCountString + ", " + blank + "]");
         }

         int lineCount = 0;

         while(true) {
            try {
               this.readJournalLine(source.readUtf8LineStrict());
               ++lineCount;
            } catch (EOFException var14) {
               this.redundantOpCount = lineCount - this.lruEntries.size();
               if (!source.exhausted()) {
                  this.rebuildJournal();
                  break;
               }

               this.journalWriter = this.newJournalWriter();
               break;
            }
         }
      } catch (Throwable var15) {
         var2 = var15;
         throw var15;
      } finally {
         if (source != null) {
            $closeResource(var2, source);
         }

      }

   }

   private BufferedSink newJournalWriter() throws FileNotFoundException {
      Sink fileSink = this.fileSystem.appendingSink(this.journalFile);
      Sink faultHidingSink = new FaultHidingSink(fileSink) {
         protected void onException(IOException e) {
            assert Thread.holdsLock(DiskLruCache.this);

            DiskLruCache.this.hasJournalErrors = true;
         }
      };
      return Okio.buffer((Sink)faultHidingSink);
   }

   private void readJournalLine(String line) throws IOException {
      int firstSpace = line.indexOf(32);
      if (firstSpace == -1) {
         throw new IOException("unexpected journal line: " + line);
      } else {
         int keyBegin = firstSpace + 1;
         int secondSpace = line.indexOf(32, keyBegin);
         String key;
         if (secondSpace == -1) {
            key = line.substring(keyBegin);
            if (firstSpace == "REMOVE".length() && line.startsWith("REMOVE")) {
               this.lruEntries.remove(key);
               return;
            }
         } else {
            key = line.substring(keyBegin, secondSpace);
         }

         DiskLruCache.Entry entry = (DiskLruCache.Entry)this.lruEntries.get(key);
         if (entry == null) {
            entry = new DiskLruCache.Entry(key);
            this.lruEntries.put(key, entry);
         }

         if (secondSpace != -1 && firstSpace == "CLEAN".length() && line.startsWith("CLEAN")) {
            String[] parts = line.substring(secondSpace + 1).split(" ");
            entry.readable = true;
            entry.currentEditor = null;
            entry.setLengths(parts);
         } else if (secondSpace == -1 && firstSpace == "DIRTY".length() && line.startsWith("DIRTY")) {
            entry.currentEditor = new DiskLruCache.Editor(entry);
         } else if (secondSpace != -1 || firstSpace != "READ".length() || !line.startsWith("READ")) {
            throw new IOException("unexpected journal line: " + line);
         }

      }
   }

   private void processJournal() throws IOException {
      this.fileSystem.delete(this.journalFileTmp);
      Iterator i = this.lruEntries.values().iterator();

      while(true) {
         while(i.hasNext()) {
            DiskLruCache.Entry entry = (DiskLruCache.Entry)i.next();
            int t;
            if (entry.currentEditor == null) {
               for(t = 0; t < this.valueCount; ++t) {
                  this.size += entry.lengths[t];
               }
            } else {
               entry.currentEditor = null;

               for(t = 0; t < this.valueCount; ++t) {
                  this.fileSystem.delete(entry.cleanFiles[t]);
                  this.fileSystem.delete(entry.dirtyFiles[t]);
               }

               i.remove();
            }
         }

         return;
      }
   }

   synchronized void rebuildJournal() throws IOException {
      if (this.journalWriter != null) {
         this.journalWriter.close();
      }

      BufferedSink writer = Okio.buffer(this.fileSystem.sink(this.journalFileTmp));
      Throwable var2 = null;

      try {
         writer.writeUtf8("libcore.io.DiskLruCache").writeByte(10);
         writer.writeUtf8("1").writeByte(10);
         writer.writeDecimalLong((long)this.appVersion).writeByte(10);
         writer.writeDecimalLong((long)this.valueCount).writeByte(10);
         writer.writeByte(10);
         Iterator var3 = this.lruEntries.values().iterator();

         while(var3.hasNext()) {
            DiskLruCache.Entry entry = (DiskLruCache.Entry)var3.next();
            if (entry.currentEditor != null) {
               writer.writeUtf8("DIRTY").writeByte(32);
               writer.writeUtf8(entry.key);
               writer.writeByte(10);
            } else {
               writer.writeUtf8("CLEAN").writeByte(32);
               writer.writeUtf8(entry.key);
               entry.writeLengths(writer);
               writer.writeByte(10);
            }
         }
      } catch (Throwable var8) {
         var2 = var8;
         throw var8;
      } finally {
         if (writer != null) {
            $closeResource(var2, writer);
         }

      }

      if (this.fileSystem.exists(this.journalFile)) {
         this.fileSystem.rename(this.journalFile, this.journalFileBackup);
      }

      this.fileSystem.rename(this.journalFileTmp, this.journalFile);
      this.fileSystem.delete(this.journalFileBackup);
      this.journalWriter = this.newJournalWriter();
      this.hasJournalErrors = false;
      this.mostRecentRebuildFailed = false;
   }

   public synchronized DiskLruCache.Snapshot get(String key) throws IOException {
      this.initialize();
      this.checkNotClosed();
      this.validateKey(key);
      DiskLruCache.Entry entry = (DiskLruCache.Entry)this.lruEntries.get(key);
      if (entry != null && entry.readable) {
         DiskLruCache.Snapshot snapshot = entry.snapshot();
         if (snapshot == null) {
            return null;
         } else {
            ++this.redundantOpCount;
            this.journalWriter.writeUtf8("READ").writeByte(32).writeUtf8(key).writeByte(10);
            if (this.journalRebuildRequired()) {
               this.executor.execute(this.cleanupRunnable);
            }

            return snapshot;
         }
      } else {
         return null;
      }
   }

   @Nullable
   public DiskLruCache.Editor edit(String key) throws IOException {
      return this.edit(key, -1L);
   }

   synchronized DiskLruCache.Editor edit(String key, long expectedSequenceNumber) throws IOException {
      this.initialize();
      this.checkNotClosed();
      this.validateKey(key);
      DiskLruCache.Entry entry = (DiskLruCache.Entry)this.lruEntries.get(key);
      if (expectedSequenceNumber == -1L || entry != null && entry.sequenceNumber == expectedSequenceNumber) {
         if (entry != null && entry.currentEditor != null) {
            return null;
         } else if (!this.mostRecentTrimFailed && !this.mostRecentRebuildFailed) {
            this.journalWriter.writeUtf8("DIRTY").writeByte(32).writeUtf8(key).writeByte(10);
            this.journalWriter.flush();
            if (this.hasJournalErrors) {
               return null;
            } else {
               if (entry == null) {
                  entry = new DiskLruCache.Entry(key);
                  this.lruEntries.put(key, entry);
               }

               DiskLruCache.Editor editor = new DiskLruCache.Editor(entry);
               entry.currentEditor = editor;
               return editor;
            }
         } else {
            this.executor.execute(this.cleanupRunnable);
            return null;
         }
      } else {
         return null;
      }
   }

   public File getDirectory() {
      return this.directory;
   }

   public synchronized long getMaxSize() {
      return this.maxSize;
   }

   public synchronized void setMaxSize(long maxSize) {
      this.maxSize = maxSize;
      if (this.initialized) {
         this.executor.execute(this.cleanupRunnable);
      }

   }

   public synchronized long size() throws IOException {
      this.initialize();
      return this.size;
   }

   synchronized void completeEdit(DiskLruCache.Editor editor, boolean success) throws IOException {
      DiskLruCache.Entry entry = editor.entry;
      if (entry.currentEditor != editor) {
         throw new IllegalStateException();
      } else {
         int i;
         if (success && !entry.readable) {
            for(i = 0; i < this.valueCount; ++i) {
               if (!editor.written[i]) {
                  editor.abort();
                  throw new IllegalStateException("Newly created entry didn't create value for index " + i);
               }

               if (!this.fileSystem.exists(entry.dirtyFiles[i])) {
                  editor.abort();
                  return;
               }
            }
         }

         for(i = 0; i < this.valueCount; ++i) {
            File dirty = entry.dirtyFiles[i];
            if (success) {
               if (this.fileSystem.exists(dirty)) {
                  File clean = entry.cleanFiles[i];
                  this.fileSystem.rename(dirty, clean);
                  long oldLength = entry.lengths[i];
                  long newLength = this.fileSystem.size(clean);
                  entry.lengths[i] = newLength;
                  this.size = this.size - oldLength + newLength;
               }
            } else {
               this.fileSystem.delete(dirty);
            }
         }

         ++this.redundantOpCount;
         entry.currentEditor = null;
         if (entry.readable | success) {
            entry.readable = true;
            this.journalWriter.writeUtf8("CLEAN").writeByte(32);
            this.journalWriter.writeUtf8(entry.key);
            entry.writeLengths(this.journalWriter);
            this.journalWriter.writeByte(10);
            if (success) {
               entry.sequenceNumber = (long)(this.nextSequenceNumber++);
            }
         } else {
            this.lruEntries.remove(entry.key);
            this.journalWriter.writeUtf8("REMOVE").writeByte(32);
            this.journalWriter.writeUtf8(entry.key);
            this.journalWriter.writeByte(10);
         }

         this.journalWriter.flush();
         if (this.size > this.maxSize || this.journalRebuildRequired()) {
            this.executor.execute(this.cleanupRunnable);
         }

      }
   }

   boolean journalRebuildRequired() {
      int redundantOpCompactThreshold = true;
      return this.redundantOpCount >= 2000 && this.redundantOpCount >= this.lruEntries.size();
   }

   public synchronized boolean remove(String key) throws IOException {
      this.initialize();
      this.checkNotClosed();
      this.validateKey(key);
      DiskLruCache.Entry entry = (DiskLruCache.Entry)this.lruEntries.get(key);
      if (entry == null) {
         return false;
      } else {
         boolean removed = this.removeEntry(entry);
         if (removed && this.size <= this.maxSize) {
            this.mostRecentTrimFailed = false;
         }

         return removed;
      }
   }

   boolean removeEntry(DiskLruCache.Entry entry) throws IOException {
      if (entry.currentEditor != null) {
         entry.currentEditor.detach();
      }

      for(int i = 0; i < this.valueCount; ++i) {
         this.fileSystem.delete(entry.cleanFiles[i]);
         this.size -= entry.lengths[i];
         entry.lengths[i] = 0L;
      }

      ++this.redundantOpCount;
      this.journalWriter.writeUtf8("REMOVE").writeByte(32).writeUtf8(entry.key).writeByte(10);
      this.lruEntries.remove(entry.key);
      if (this.journalRebuildRequired()) {
         this.executor.execute(this.cleanupRunnable);
      }

      return true;
   }

   public synchronized boolean isClosed() {
      return this.closed;
   }

   private synchronized void checkNotClosed() {
      if (this.isClosed()) {
         throw new IllegalStateException("cache is closed");
      }
   }

   public synchronized void flush() throws IOException {
      if (this.initialized) {
         this.checkNotClosed();
         this.trimToSize();
         this.journalWriter.flush();
      }
   }

   public synchronized void close() throws IOException {
      if (this.initialized && !this.closed) {
         DiskLruCache.Entry[] var1 = (DiskLruCache.Entry[])this.lruEntries.values().toArray(new DiskLruCache.Entry[this.lruEntries.size()]);
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            DiskLruCache.Entry entry = var1[var3];
            if (entry.currentEditor != null) {
               entry.currentEditor.abort();
            }
         }

         this.trimToSize();
         this.journalWriter.close();
         this.journalWriter = null;
         this.closed = true;
      } else {
         this.closed = true;
      }
   }

   void trimToSize() throws IOException {
      while(this.size > this.maxSize) {
         DiskLruCache.Entry toEvict = (DiskLruCache.Entry)this.lruEntries.values().iterator().next();
         this.removeEntry(toEvict);
      }

      this.mostRecentTrimFailed = false;
   }

   public void delete() throws IOException {
      this.close();
      this.fileSystem.deleteContents(this.directory);
   }

   public synchronized void evictAll() throws IOException {
      this.initialize();
      DiskLruCache.Entry[] var1 = (DiskLruCache.Entry[])this.lruEntries.values().toArray(new DiskLruCache.Entry[this.lruEntries.size()]);
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         DiskLruCache.Entry entry = var1[var3];
         this.removeEntry(entry);
      }

      this.mostRecentTrimFailed = false;
   }

   private void validateKey(String key) {
      Matcher matcher = LEGAL_KEY_PATTERN.matcher(key);
      if (!matcher.matches()) {
         throw new IllegalArgumentException("keys must match regex [a-z0-9_-]{1,120}: \"" + key + "\"");
      }
   }

   public synchronized Iterator<DiskLruCache.Snapshot> snapshots() throws IOException {
      this.initialize();
      return new Iterator<DiskLruCache.Snapshot>() {
         final Iterator<DiskLruCache.Entry> delegate;
         DiskLruCache.Snapshot nextSnapshot;
         DiskLruCache.Snapshot removeSnapshot;

         {
            this.delegate = (new ArrayList(DiskLruCache.this.lruEntries.values())).iterator();
         }

         public boolean hasNext() {
            if (this.nextSnapshot != null) {
               return true;
            } else {
               synchronized(DiskLruCache.this) {
                  if (DiskLruCache.this.closed) {
                     return false;
                  } else {
                     DiskLruCache.Snapshot snapshot;
                     do {
                        if (!this.delegate.hasNext()) {
                           return false;
                        }

                        DiskLruCache.Entry entry = (DiskLruCache.Entry)this.delegate.next();
                        snapshot = entry.snapshot();
                     } while(snapshot == null);

                     this.nextSnapshot = snapshot;
                     return true;
                  }
               }
            }
         }

         public DiskLruCache.Snapshot next() {
            if (!this.hasNext()) {
               throw new NoSuchElementException();
            } else {
               this.removeSnapshot = this.nextSnapshot;
               this.nextSnapshot = null;
               return this.removeSnapshot;
            }
         }

         public void remove() {
            if (this.removeSnapshot == null) {
               throw new IllegalStateException("remove() before next()");
            } else {
               try {
                  DiskLruCache.this.remove(this.removeSnapshot.key);
               } catch (IOException var5) {
               } finally {
                  this.removeSnapshot = null;
               }

            }
         }
      };
   }

   // $FF: synthetic method
   private static void $closeResource(Throwable x0, AutoCloseable x1) {
      if (x0 != null) {
         try {
            x1.close();
         } catch (Throwable var3) {
            x0.addSuppressed(var3);
         }
      } else {
         x1.close();
      }

   }

   private final class Entry {
      final String key;
      final long[] lengths;
      final File[] cleanFiles;
      final File[] dirtyFiles;
      boolean readable;
      DiskLruCache.Editor currentEditor;
      long sequenceNumber;

      Entry(String key) {
         this.key = key;
         this.lengths = new long[DiskLruCache.this.valueCount];
         this.cleanFiles = new File[DiskLruCache.this.valueCount];
         this.dirtyFiles = new File[DiskLruCache.this.valueCount];
         StringBuilder fileBuilder = (new StringBuilder(key)).append('.');
         int truncateTo = fileBuilder.length();

         for(int i = 0; i < DiskLruCache.this.valueCount; ++i) {
            fileBuilder.append(i);
            this.cleanFiles[i] = new File(DiskLruCache.this.directory, fileBuilder.toString());
            fileBuilder.append(".tmp");
            this.dirtyFiles[i] = new File(DiskLruCache.this.directory, fileBuilder.toString());
            fileBuilder.setLength(truncateTo);
         }

      }

      void setLengths(String[] strings) throws IOException {
         if (strings.length != DiskLruCache.this.valueCount) {
            throw this.invalidLengths(strings);
         } else {
            try {
               for(int i = 0; i < strings.length; ++i) {
                  this.lengths[i] = Long.parseLong(strings[i]);
               }

            } catch (NumberFormatException var3) {
               throw this.invalidLengths(strings);
            }
         }
      }

      void writeLengths(BufferedSink writer) throws IOException {
         long[] var2 = this.lengths;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            long length = var2[var4];
            writer.writeByte(32).writeDecimalLong(length);
         }

      }

      private IOException invalidLengths(String[] strings) throws IOException {
         throw new IOException("unexpected journal line: " + Arrays.toString(strings));
      }

      DiskLruCache.Snapshot snapshot() {
         if (!Thread.holdsLock(DiskLruCache.this)) {
            throw new AssertionError();
         } else {
            Source[] sources = new Source[DiskLruCache.this.valueCount];
            long[] lengths = (long[])this.lengths.clone();

            try {
               for(int i = 0; i < DiskLruCache.this.valueCount; ++i) {
                  sources[i] = DiskLruCache.this.fileSystem.source(this.cleanFiles[i]);
               }

               return DiskLruCache.this.new Snapshot(this.key, this.sequenceNumber, sources, lengths);
            } catch (FileNotFoundException var6) {
               for(int ix = 0; ix < DiskLruCache.this.valueCount && sources[ix] != null; ++ix) {
                  Util.closeQuietly((Closeable)sources[ix]);
               }

               try {
                  DiskLruCache.this.removeEntry(this);
               } catch (IOException var5) {
               }

               return null;
            }
         }
      }
   }

   public final class Editor {
      final DiskLruCache.Entry entry;
      final boolean[] written;
      private boolean done;

      Editor(DiskLruCache.Entry entry) {
         this.entry = entry;
         this.written = entry.readable ? null : new boolean[DiskLruCache.this.valueCount];
      }

      void detach() {
         if (this.entry.currentEditor == this) {
            for(int i = 0; i < DiskLruCache.this.valueCount; ++i) {
               try {
                  DiskLruCache.this.fileSystem.delete(this.entry.dirtyFiles[i]);
               } catch (IOException var3) {
               }
            }

            this.entry.currentEditor = null;
         }

      }

      public Source newSource(int index) {
         synchronized(DiskLruCache.this) {
            if (this.done) {
               throw new IllegalStateException();
            } else if (this.entry.readable && this.entry.currentEditor == this) {
               Source var10000;
               try {
                  var10000 = DiskLruCache.this.fileSystem.source(this.entry.cleanFiles[index]);
               } catch (FileNotFoundException var5) {
                  return null;
               }

               return var10000;
            } else {
               return null;
            }
         }
      }

      public Sink newSink(int index) {
         synchronized(DiskLruCache.this) {
            if (this.done) {
               throw new IllegalStateException();
            } else if (this.entry.currentEditor != this) {
               return Okio.blackhole();
            } else {
               if (!this.entry.readable) {
                  this.written[index] = true;
               }

               File dirtyFile = this.entry.dirtyFiles[index];

               Sink sink;
               try {
                  sink = DiskLruCache.this.fileSystem.sink(dirtyFile);
               } catch (FileNotFoundException var7) {
                  return Okio.blackhole();
               }

               return new FaultHidingSink(sink) {
                  protected void onException(IOException e) {
                     synchronized(DiskLruCache.this) {
                        Editor.this.detach();
                     }
                  }
               };
            }
         }
      }

      public void commit() throws IOException {
         synchronized(DiskLruCache.this) {
            if (this.done) {
               throw new IllegalStateException();
            } else {
               if (this.entry.currentEditor == this) {
                  DiskLruCache.this.completeEdit(this, true);
               }

               this.done = true;
            }
         }
      }

      public void abort() throws IOException {
         synchronized(DiskLruCache.this) {
            if (this.done) {
               throw new IllegalStateException();
            } else {
               if (this.entry.currentEditor == this) {
                  DiskLruCache.this.completeEdit(this, false);
               }

               this.done = true;
            }
         }
      }

      public void abortUnlessCommitted() {
         synchronized(DiskLruCache.this) {
            if (!this.done && this.entry.currentEditor == this) {
               try {
                  DiskLruCache.this.completeEdit(this, false);
               } catch (IOException var4) {
               }
            }

         }
      }
   }

   public final class Snapshot implements Closeable {
      private final String key;
      private final long sequenceNumber;
      private final Source[] sources;
      private final long[] lengths;

      Snapshot(String key, long sequenceNumber, Source[] sources, long[] lengths) {
         this.key = key;
         this.sequenceNumber = sequenceNumber;
         this.sources = sources;
         this.lengths = lengths;
      }

      public String key() {
         return this.key;
      }

      @Nullable
      public DiskLruCache.Editor edit() throws IOException {
         return DiskLruCache.this.edit(this.key, this.sequenceNumber);
      }

      public Source getSource(int index) {
         return this.sources[index];
      }

      public long getLength(int index) {
         return this.lengths[index];
      }

      public void close() {
         Source[] var1 = this.sources;
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Source in = var1[var3];
            Util.closeQuietly((Closeable)in);
         }

      }
   }
}
