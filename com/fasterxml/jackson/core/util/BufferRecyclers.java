package com.fasterxml.jackson.core.util;

import java.lang.ref.SoftReference;

public class BufferRecyclers {
   public static final String SYSTEM_PROPERTY_TRACK_REUSABLE_BUFFERS = "com.fasterxml.jackson.core.util.BufferRecyclers.trackReusableBuffers";
   private static final ThreadLocalBufferManager _bufferRecyclerTracker = "true".equals(System.getProperty("com.fasterxml.jackson.core.util.BufferRecyclers.trackReusableBuffers")) ? ThreadLocalBufferManager.instance() : null;
   protected static final ThreadLocal<SoftReference<BufferRecycler>> _recyclerRef = new ThreadLocal();

   public static BufferRecycler getBufferRecycler() {
      SoftReference<BufferRecycler> ref = (SoftReference)_recyclerRef.get();
      BufferRecycler br = ref == null ? null : (BufferRecycler)ref.get();
      if (br == null) {
         br = new BufferRecycler();
         if (_bufferRecyclerTracker != null) {
            ref = _bufferRecyclerTracker.wrapAndTrack(br);
         } else {
            ref = new SoftReference(br);
         }

         _recyclerRef.set(ref);
      }

      return br;
   }

   public static int releaseBuffers() {
      return _bufferRecyclerTracker != null ? _bufferRecyclerTracker.releaseBuffers() : -1;
   }
}
