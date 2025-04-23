package okio;

import java.io.IOException;

final class PeekSource implements Source {
   private final BufferedSource upstream;
   private final Buffer buffer;
   private Segment expectedSegment;
   private int expectedPos;
   private boolean closed;
   private long pos;

   PeekSource(BufferedSource upstream) {
      this.upstream = upstream;
      this.buffer = upstream.buffer();
      this.expectedSegment = this.buffer.head;
      this.expectedPos = this.expectedSegment != null ? this.expectedSegment.pos : -1;
   }

   public long read(Buffer sink, long byteCount) throws IOException {
      if (this.closed) {
         throw new IllegalStateException("closed");
      } else if (this.expectedSegment != null && (this.expectedSegment != this.buffer.head || this.expectedPos != this.buffer.head.pos)) {
         throw new IllegalStateException("Peek source is invalid because upstream source was used");
      } else {
         this.upstream.request(this.pos + byteCount);
         if (this.expectedSegment == null && this.buffer.head != null) {
            this.expectedSegment = this.buffer.head;
            this.expectedPos = this.buffer.head.pos;
         }

         long toCopy = Math.min(byteCount, this.buffer.size - this.pos);
         if (toCopy <= 0L) {
            return -1L;
         } else {
            this.buffer.copyTo(sink, this.pos, toCopy);
            this.pos += toCopy;
            return toCopy;
         }
      }
   }

   public Timeout timeout() {
      return this.upstream.timeout();
   }

   public void close() throws IOException {
      this.closed = true;
   }
}
