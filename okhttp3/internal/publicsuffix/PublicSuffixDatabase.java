package okhttp3.internal.publicsuffix;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.internal.platform.Platform;
import okio.BufferedSource;
import okio.GzipSource;
import okio.Okio;
import okio.Source;

public final class PublicSuffixDatabase {
   public static final String PUBLIC_SUFFIX_RESOURCE = "publicsuffixes.gz";
   private static final byte[] WILDCARD_LABEL = new byte[]{42};
   private static final String[] EMPTY_RULE = new String[0];
   private static final String[] PREVAILING_RULE = new String[]{"*"};
   private static final byte EXCEPTION_MARKER = 33;
   private static final PublicSuffixDatabase instance = new PublicSuffixDatabase();
   private final AtomicBoolean listRead = new AtomicBoolean(false);
   private final CountDownLatch readCompleteLatch = new CountDownLatch(1);
   private byte[] publicSuffixListBytes;
   private byte[] publicSuffixExceptionListBytes;

   public static PublicSuffixDatabase get() {
      return instance;
   }

   public String getEffectiveTldPlusOne(String domain) {
      if (domain == null) {
         throw new NullPointerException("domain == null");
      } else {
         String unicodeDomain = IDN.toUnicode(domain);
         String[] domainLabels = unicodeDomain.split("\\.");
         String[] rule = this.findMatchingRule(domainLabels);
         if (domainLabels.length == rule.length && rule[0].charAt(0) != '!') {
            return null;
         } else {
            int firstLabelOffset;
            if (rule[0].charAt(0) == '!') {
               firstLabelOffset = domainLabels.length - rule.length;
            } else {
               firstLabelOffset = domainLabels.length - (rule.length + 1);
            }

            StringBuilder effectiveTldPlusOne = new StringBuilder();
            String[] punycodeLabels = domain.split("\\.");

            for(int i = firstLabelOffset; i < punycodeLabels.length; ++i) {
               effectiveTldPlusOne.append(punycodeLabels[i]).append('.');
            }

            effectiveTldPlusOne.deleteCharAt(effectiveTldPlusOne.length() - 1);
            return effectiveTldPlusOne.toString();
         }
      }
   }

   private String[] findMatchingRule(String[] domainLabels) {
      if (!this.listRead.get() && this.listRead.compareAndSet(false, true)) {
         this.readTheListUninterruptibly();
      } else {
         try {
            this.readCompleteLatch.await();
         } catch (InterruptedException var8) {
            Thread.currentThread().interrupt();
         }
      }

      synchronized(this) {
         if (this.publicSuffixListBytes == null) {
            throw new IllegalStateException("Unable to load publicsuffixes.gz resource from the classpath.");
         }
      }

      byte[][] domainLabelsUtf8Bytes = new byte[domainLabels.length][];

      for(int i = 0; i < domainLabels.length; ++i) {
         domainLabelsUtf8Bytes[i] = domainLabels[i].getBytes(StandardCharsets.UTF_8);
      }

      String exactMatch = null;

      String exception;
      for(int i = 0; i < domainLabelsUtf8Bytes.length; ++i) {
         exception = binarySearchBytes(this.publicSuffixListBytes, domainLabelsUtf8Bytes, i);
         if (exception != null) {
            exactMatch = exception;
            break;
         }
      }

      String wildcardMatch = null;
      int labelIndex;
      String rule;
      if (domainLabelsUtf8Bytes.length > 1) {
         byte[][] labelsWithWildcard = (byte[][])domainLabelsUtf8Bytes.clone();

         for(labelIndex = 0; labelIndex < labelsWithWildcard.length - 1; ++labelIndex) {
            labelsWithWildcard[labelIndex] = WILDCARD_LABEL;
            rule = binarySearchBytes(this.publicSuffixListBytes, labelsWithWildcard, labelIndex);
            if (rule != null) {
               wildcardMatch = rule;
               break;
            }
         }
      }

      exception = null;
      if (wildcardMatch != null) {
         for(labelIndex = 0; labelIndex < domainLabelsUtf8Bytes.length - 1; ++labelIndex) {
            rule = binarySearchBytes(this.publicSuffixExceptionListBytes, domainLabelsUtf8Bytes, labelIndex);
            if (rule != null) {
               exception = rule;
               break;
            }
         }
      }

      if (exception != null) {
         exception = "!" + exception;
         return exception.split("\\.");
      } else if (exactMatch == null && wildcardMatch == null) {
         return PREVAILING_RULE;
      } else {
         String[] exactRuleLabels = exactMatch != null ? exactMatch.split("\\.") : EMPTY_RULE;
         String[] wildcardRuleLabels = wildcardMatch != null ? wildcardMatch.split("\\.") : EMPTY_RULE;
         return exactRuleLabels.length > wildcardRuleLabels.length ? exactRuleLabels : wildcardRuleLabels;
      }
   }

   private static String binarySearchBytes(byte[] bytesToSearch, byte[][] labels, int labelIndex) {
      int low = 0;
      int high = bytesToSearch.length;
      String match = null;

      while(low < high) {
         int mid;
         for(mid = (low + high) / 2; mid > -1 && bytesToSearch[mid] != 10; --mid) {
         }

         ++mid;

         int end;
         for(end = 1; bytesToSearch[mid + end] != 10; ++end) {
         }

         int publicSuffixLength = mid + end - mid;
         int currentLabelIndex = labelIndex;
         int currentLabelByteIndex = 0;
         int publicSuffixByteIndex = 0;
         boolean expectDot = false;

         int compareResult;
         int publicSuffixBytesLeft;
         int labelBytesLeft;
         while(true) {
            if (expectDot) {
               publicSuffixBytesLeft = 46;
               expectDot = false;
            } else {
               publicSuffixBytesLeft = labels[currentLabelIndex][currentLabelByteIndex] & 255;
            }

            labelBytesLeft = bytesToSearch[mid + publicSuffixByteIndex] & 255;
            compareResult = publicSuffixBytesLeft - labelBytesLeft;
            if (compareResult != 0) {
               break;
            }

            ++publicSuffixByteIndex;
            ++currentLabelByteIndex;
            if (publicSuffixByteIndex == publicSuffixLength) {
               break;
            }

            if (labels[currentLabelIndex].length == currentLabelByteIndex) {
               if (currentLabelIndex == labels.length - 1) {
                  break;
               }

               ++currentLabelIndex;
               currentLabelByteIndex = -1;
               expectDot = true;
            }
         }

         if (compareResult < 0) {
            high = mid - 1;
         } else if (compareResult > 0) {
            low = mid + end + 1;
         } else {
            publicSuffixBytesLeft = publicSuffixLength - publicSuffixByteIndex;
            labelBytesLeft = labels[currentLabelIndex].length - currentLabelByteIndex;

            for(int i = currentLabelIndex + 1; i < labels.length; ++i) {
               labelBytesLeft += labels[i].length;
            }

            if (labelBytesLeft < publicSuffixBytesLeft) {
               high = mid - 1;
            } else {
               if (labelBytesLeft <= publicSuffixBytesLeft) {
                  match = new String(bytesToSearch, mid, publicSuffixLength, StandardCharsets.UTF_8);
                  break;
               }

               low = mid + end + 1;
            }
         }
      }

      return match;
   }

   private void readTheListUninterruptibly() {
      boolean interrupted = false;

      try {
         while(true) {
            try {
               this.readTheList();
               return;
            } catch (InterruptedIOException var7) {
               Thread.interrupted();
               interrupted = true;
            } catch (IOException var8) {
               Platform.get().log(5, "Failed to read public suffix list", var8);
               return;
            }
         }
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt();
         }

      }
   }

   private void readTheList() throws IOException {
      InputStream resource = PublicSuffixDatabase.class.getResourceAsStream("publicsuffixes.gz");
      if (resource != null) {
         BufferedSource bufferedSource = Okio.buffer((Source)(new GzipSource(Okio.source(resource))));
         Throwable var5 = null;

         byte[] publicSuffixListBytes;
         byte[] publicSuffixExceptionListBytes;
         try {
            int totalBytes = bufferedSource.readInt();
            publicSuffixListBytes = new byte[totalBytes];
            bufferedSource.readFully(publicSuffixListBytes);
            int totalExceptionBytes = bufferedSource.readInt();
            publicSuffixExceptionListBytes = new byte[totalExceptionBytes];
            bufferedSource.readFully(publicSuffixExceptionListBytes);
         } catch (Throwable var18) {
            var5 = var18;
            throw var18;
         } finally {
            if (bufferedSource != null) {
               if (var5 != null) {
                  try {
                     bufferedSource.close();
                  } catch (Throwable var16) {
                     var5.addSuppressed(var16);
                  }
               } else {
                  bufferedSource.close();
               }
            }

         }

         synchronized(this) {
            this.publicSuffixListBytes = publicSuffixListBytes;
            this.publicSuffixExceptionListBytes = publicSuffixExceptionListBytes;
         }

         this.readCompleteLatch.countDown();
      }
   }

   void setListBytes(byte[] publicSuffixListBytes, byte[] publicSuffixExceptionListBytes) {
      this.publicSuffixListBytes = publicSuffixListBytes;
      this.publicSuffixExceptionListBytes = publicSuffixExceptionListBytes;
      this.listRead.set(true);
      this.readCompleteLatch.countDown();
   }
}
