package javassist.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationsWriter;

public class ParameterAnnotationsAttribute extends AttributeInfo {
   public static final String visibleTag = "RuntimeVisibleParameterAnnotations";
   public static final String invisibleTag = "RuntimeInvisibleParameterAnnotations";

   public ParameterAnnotationsAttribute(ConstPool cp, String attrname, byte[] info) {
      super(cp, attrname, info);
   }

   public ParameterAnnotationsAttribute(ConstPool cp, String attrname) {
      this(cp, attrname, new byte[]{0});
   }

   ParameterAnnotationsAttribute(ConstPool cp, int n, DataInputStream in) throws IOException {
      super(cp, n, in);
   }

   public int numParameters() {
      return this.info[0] & 255;
   }

   public AttributeInfo copy(ConstPool newCp, Map<String, String> classnames) {
      AnnotationsAttribute.Copier copier = new AnnotationsAttribute.Copier(this.info, this.constPool, newCp, classnames);

      try {
         copier.parameters();
         return new ParameterAnnotationsAttribute(newCp, this.getName(), copier.close());
      } catch (Exception var5) {
         throw new RuntimeException(var5.toString());
      }
   }

   public Annotation[][] getAnnotations() {
      try {
         return (new AnnotationsAttribute.Parser(this.info, this.constPool)).parseParameters();
      } catch (Exception var2) {
         throw new RuntimeException(var2.toString());
      }
   }

   public void setAnnotations(Annotation[][] params) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      AnnotationsWriter writer = new AnnotationsWriter(output, this.constPool);

      try {
         writer.numParameters(params.length);
         Annotation[][] var4 = params;
         int var5 = params.length;
         int var6 = 0;

         while(true) {
            if (var6 >= var5) {
               writer.close();
               break;
            }

            Annotation[] anno = var4[var6];
            writer.numAnnotations(anno.length);

            for(int j = 0; j < anno.length; ++j) {
               anno[j].write(writer);
            }

            ++var6;
         }
      } catch (IOException var9) {
         throw new RuntimeException(var9);
      }

      this.set(output.toByteArray());
   }

   void renameClass(String oldname, String newname) {
      Map<String, String> map = new HashMap();
      map.put(oldname, newname);
      this.renameClass(map);
   }

   void renameClass(Map<String, String> classnames) {
      AnnotationsAttribute.Renamer renamer = new AnnotationsAttribute.Renamer(this.info, this.getConstPool(), classnames);

      try {
         renamer.parameters();
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }
   }

   void getRefClasses(Map<String, String> classnames) {
      this.renameClass(classnames);
   }

   public String toString() {
      Annotation[][] aa = this.getAnnotations();
      StringBuilder sbuf = new StringBuilder();
      Annotation[][] var3 = aa;
      int var4 = aa.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Annotation[] a = var3[var5];
         Annotation[] var7 = a;
         int var8 = a.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            Annotation i = var7[var9];
            sbuf.append(i.toString()).append(" ");
         }

         sbuf.append(", ");
      }

      return sbuf.toString().replaceAll(" (?=,)|, $", "");
   }
}
