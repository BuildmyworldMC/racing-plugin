package com.fasterxml.jackson.databind.node;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

class NodeSerialization implements Serializable, Externalizable {
   private static final long serialVersionUID = 1L;
   public byte[] json;

   public NodeSerialization() {
   }

   public NodeSerialization(byte[] b) {
      this.json = b;
   }

   protected Object readResolve() {
      try {
         return InternalNodeMapper.bytesToNode(this.json);
      } catch (IOException var2) {
         throw new IllegalArgumentException("Failed to JDK deserialize `JsonNode` value: " + var2.getMessage(), var2);
      }
   }

   public static NodeSerialization from(Object o) {
      try {
         return new NodeSerialization(InternalNodeMapper.valueToBytes(o));
      } catch (IOException var2) {
         throw new IllegalArgumentException("Failed to JDK serialize `" + o.getClass().getSimpleName() + "` value: " + var2.getMessage(), var2);
      }
   }

   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeInt(this.json.length);
      out.write(this.json);
   }

   public void readExternal(ObjectInput in) throws IOException {
      int len = in.readInt();
      this.json = new byte[len];
      in.readFully(this.json, 0, len);
   }
}
