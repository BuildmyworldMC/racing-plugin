package com.fasterxml.jackson.databind.ext;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class OptionalHandlerFactory implements Serializable {
   private static final long serialVersionUID = 1L;
   private static final String PACKAGE_PREFIX_JAVAX_XML = "javax.xml.";
   private static final String SERIALIZERS_FOR_JAVAX_XML = "com.fasterxml.jackson.databind.ext.CoreXMLSerializers";
   private static final String DESERIALIZERS_FOR_JAVAX_XML = "com.fasterxml.jackson.databind.ext.CoreXMLDeserializers";
   private static final String SERIALIZER_FOR_DOM_NODE = "com.fasterxml.jackson.databind.ext.DOMSerializer";
   private static final String DESERIALIZER_FOR_DOM_DOCUMENT = "com.fasterxml.jackson.databind.ext.DOMDeserializer$DocumentDeserializer";
   private static final String DESERIALIZER_FOR_DOM_NODE = "com.fasterxml.jackson.databind.ext.DOMDeserializer$NodeDeserializer";
   private static final Class<?> CLASS_DOM_NODE;
   private static final Class<?> CLASS_DOM_DOCUMENT;
   private static final Java7Handlers _jdk7Helper;
   public static final OptionalHandlerFactory instance;

   protected OptionalHandlerFactory() {
   }

   public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription beanDesc) {
      Class<?> rawType = type.getRawClass();
      if (CLASS_DOM_NODE != null && CLASS_DOM_NODE.isAssignableFrom(rawType)) {
         return (JsonSerializer)this.instantiate("com.fasterxml.jackson.databind.ext.DOMSerializer");
      } else {
         if (_jdk7Helper != null) {
            JsonSerializer<?> ser = _jdk7Helper.getSerializerForJavaNioFilePath(rawType);
            if (ser != null) {
               return ser;
            }
         }

         String className = rawType.getName();
         if (!className.startsWith("javax.xml.") && !this.hasSuperClassStartingWith(rawType, "javax.xml.")) {
            return null;
         } else {
            String factoryName = "com.fasterxml.jackson.databind.ext.CoreXMLSerializers";
            Object ob = this.instantiate(factoryName);
            return ob == null ? null : ((Serializers)ob).findSerializer(config, type, beanDesc);
         }
      }
   }

   public JsonDeserializer<?> findDeserializer(JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
      Class<?> rawType = type.getRawClass();
      if (_jdk7Helper != null) {
         JsonDeserializer<?> deser = _jdk7Helper.getDeserializerForJavaNioFilePath(rawType);
         if (deser != null) {
            return deser;
         }
      }

      if (CLASS_DOM_NODE != null && CLASS_DOM_NODE.isAssignableFrom(rawType)) {
         return (JsonDeserializer)this.instantiate("com.fasterxml.jackson.databind.ext.DOMDeserializer$NodeDeserializer");
      } else if (CLASS_DOM_DOCUMENT != null && CLASS_DOM_DOCUMENT.isAssignableFrom(rawType)) {
         return (JsonDeserializer)this.instantiate("com.fasterxml.jackson.databind.ext.DOMDeserializer$DocumentDeserializer");
      } else {
         String className = rawType.getName();
         if (!className.startsWith("javax.xml.") && !this.hasSuperClassStartingWith(rawType, "javax.xml.")) {
            return null;
         } else {
            String factoryName = "com.fasterxml.jackson.databind.ext.CoreXMLDeserializers";
            Object ob = this.instantiate(factoryName);
            return ob == null ? null : ((Deserializers)ob).findBeanDeserializer(type, config, beanDesc);
         }
      }
   }

   private Object instantiate(String className) {
      try {
         return ClassUtil.createInstance(Class.forName(className), false);
      } catch (LinkageError var3) {
      } catch (Exception var4) {
      }

      return null;
   }

   private boolean hasSuperClassStartingWith(Class<?> rawType, String prefix) {
      for(Class supertype = rawType.getSuperclass(); supertype != null; supertype = supertype.getSuperclass()) {
         if (supertype == Object.class) {
            return false;
         }

         if (supertype.getName().startsWith(prefix)) {
            return true;
         }
      }

      return false;
   }

   static {
      Class<?> doc = null;
      Class node = null;

      try {
         node = Node.class;
         doc = Document.class;
      } catch (Exception var4) {
         Logger.getLogger(OptionalHandlerFactory.class.getName()).log(Level.INFO, "Could not load DOM `Node` and/or `Document` classes: no DOM support");
      }

      CLASS_DOM_NODE = node;
      CLASS_DOM_DOCUMENT = doc;
      Java7Handlers x = null;

      try {
         x = Java7Handlers.instance();
      } catch (Throwable var3) {
      }

      _jdk7Helper = x;
      instance = new OptionalHandlerFactory();
   }
}
