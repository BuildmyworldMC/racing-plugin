package com.neovisionaries.ws.client;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class WebSocketOutputStream extends FilterOutputStream {
   public WebSocketOutputStream(OutputStream out) {
      super(out);
   }

   public void write(String string) throws IOException {
      byte[] bytes = Misc.getBytesUTF8(string);
      this.write((byte[])bytes);
   }

   public void write(WebSocketFrame frame) throws IOException {
      this.writeFrame0(frame);
      this.writeFrame1(frame);
      this.writeFrameExtendedPayloadLength(frame);
      byte[] maskingKey = Misc.nextBytes(4);
      this.write((byte[])maskingKey);
      this.writeFramePayload(frame, maskingKey);
   }

   private void writeFrame0(WebSocketFrame frame) throws IOException {
      int b = (frame.getFin() ? 128 : 0) | (frame.getRsv1() ? 64 : 0) | (frame.getRsv2() ? 32 : 0) | (frame.getRsv3() ? 16 : 0) | frame.getOpcode() & 15;
      this.write(b);
   }

   private void writeFrame1(WebSocketFrame frame) throws IOException {
      int b = 128;
      int len = frame.getPayloadLength();
      int b;
      if (len <= 125) {
         b = b | len;
      } else if (len <= 65535) {
         b = b | 126;
      } else {
         b = b | 127;
      }

      this.write(b);
   }

   private void writeFrameExtendedPayloadLength(WebSocketFrame frame) throws IOException {
      int len = frame.getPayloadLength();
      if (len > 125) {
         if (len <= 65535) {
            this.write(len >> 8 & 255);
            this.write(len & 255);
         } else {
            this.write(0);
            this.write(0);
            this.write(0);
            this.write(0);
            this.write(len >> 24 & 255);
            this.write(len >> 16 & 255);
            this.write(len >> 8 & 255);
            this.write(len & 255);
         }
      }
   }

   private void writeFramePayload(WebSocketFrame frame, byte[] maskingKey) throws IOException {
      byte[] payload = frame.getPayload();
      if (payload != null) {
         for(int i = 0; i < payload.length; ++i) {
            int b = (payload[i] ^ maskingKey[i % 4]) & 255;
            this.write(b);
         }

      }
   }
}
