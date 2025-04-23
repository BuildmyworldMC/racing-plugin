package com.github.hornta.racing.messenger.org.reflections.adapters;

import com.github.hornta.racing.messenger.org.reflections.ReflectionsException;
import com.github.hornta.racing.messenger.org.reflections.util.Utils;
import com.github.hornta.racing.messenger.org.reflections.vfs.Vfs;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.Descriptor;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;

public class JavassistAdapter implements MetadataAdapter<ClassFile, FieldInfo, MethodInfo> {
   public static boolean includeInvisibleTag = true;

   public List<FieldInfo> getFields(ClassFile cls) {
      return cls.getFields();
   }

   public List<MethodInfo> getMethods(ClassFile cls) {
      return cls.getMethods();
   }

   public String getMethodName(MethodInfo method) {
      return method.getName();
   }

   public List<String> getParameterNames(MethodInfo method) {
      String descriptor = method.getDescriptor();
      descriptor = descriptor.substring(descriptor.indexOf("(") + 1, descriptor.lastIndexOf(")"));
      return this.splitDescriptorToTypeNames(descriptor);
   }

   public List<String> getClassAnnotationNames(ClassFile aClass) {
      return this.getAnnotationNames((AnnotationsAttribute)aClass.getAttribute("RuntimeVisibleAnnotations"), includeInvisibleTag ? (AnnotationsAttribute)aClass.getAttribute("RuntimeInvisibleAnnotations") : null);
   }

   public List<String> getFieldAnnotationNames(FieldInfo field) {
      return this.getAnnotationNames((AnnotationsAttribute)field.getAttribute("RuntimeVisibleAnnotations"), includeInvisibleTag ? (AnnotationsAttribute)field.getAttribute("RuntimeInvisibleAnnotations") : null);
   }

   public List<String> getMethodAnnotationNames(MethodInfo method) {
      return this.getAnnotationNames((AnnotationsAttribute)method.getAttribute("RuntimeVisibleAnnotations"), includeInvisibleTag ? (AnnotationsAttribute)method.getAttribute("RuntimeInvisibleAnnotations") : null);
   }

   public List<String> getParameterAnnotationNames(MethodInfo method, int parameterIndex) {
      List<String> result = new ArrayList();
      List<ParameterAnnotationsAttribute> parameterAnnotationsAttributes = Arrays.asList((ParameterAnnotationsAttribute)method.getAttribute("RuntimeVisibleParameterAnnotations"), (ParameterAnnotationsAttribute)method.getAttribute("RuntimeInvisibleParameterAnnotations"));
      Iterator var5 = parameterAnnotationsAttributes.iterator();

      while(var5.hasNext()) {
         ParameterAnnotationsAttribute parameterAnnotationsAttribute = (ParameterAnnotationsAttribute)var5.next();
         if (parameterAnnotationsAttribute != null) {
            Annotation[][] annotations = parameterAnnotationsAttribute.getAnnotations();
            if (parameterIndex < annotations.length) {
               Annotation[] annotation = annotations[parameterIndex];
               result.addAll(this.getAnnotationNames(annotation));
            }
         }
      }

      return result;
   }

   public String getReturnTypeName(MethodInfo method) {
      String descriptor = method.getDescriptor();
      descriptor = descriptor.substring(descriptor.lastIndexOf(")") + 1);
      return (String)this.splitDescriptorToTypeNames(descriptor).get(0);
   }

   public String getFieldName(FieldInfo field) {
      return field.getName();
   }

   public ClassFile getOrCreateClassObject(Vfs.File file) {
      InputStream inputStream = null;

      ClassFile var4;
      try {
         inputStream = file.openInputStream();
         DataInputStream dis = new DataInputStream(new BufferedInputStream(inputStream));
         var4 = new ClassFile(dis);
      } catch (IOException var8) {
         throw new ReflectionsException("could not create class file from " + file.getName(), var8);
      } finally {
         Utils.close(inputStream);
      }

      return var4;
   }

   public String getMethodModifier(MethodInfo method) {
      int accessFlags = method.getAccessFlags();
      return AccessFlag.isPrivate(accessFlags) ? "private" : (AccessFlag.isProtected(accessFlags) ? "protected" : (this.isPublic(accessFlags) ? "public" : ""));
   }

   public String getMethodKey(ClassFile cls, MethodInfo method) {
      return this.getMethodName(method) + "(" + Utils.join(this.getParameterNames(method), ", ") + ")";
   }

   public String getMethodFullKey(ClassFile cls, MethodInfo method) {
      return this.getClassName(cls) + "." + this.getMethodKey(cls, method);
   }

   public boolean isPublic(Object o) {
      Integer accessFlags = o instanceof ClassFile ? ((ClassFile)o).getAccessFlags() : (o instanceof FieldInfo ? ((FieldInfo)o).getAccessFlags() : o instanceof MethodInfo ? ((MethodInfo)o).getAccessFlags() : null);
      return accessFlags != null && AccessFlag.isPublic(accessFlags);
   }

   public String getClassName(ClassFile cls) {
      return cls.getName();
   }

   public String getSuperclassName(ClassFile cls) {
      return cls.getSuperclass();
   }

   public List<String> getInterfacesNames(ClassFile cls) {
      return Arrays.asList(cls.getInterfaces());
   }

   public boolean acceptsInput(String file) {
      return file.endsWith(".class");
   }

   private List<String> getAnnotationNames(AnnotationsAttribute... annotationsAttributes) {
      return annotationsAttributes != null ? (List)Arrays.stream(annotationsAttributes).filter(Objects::nonNull).flatMap((annotationsAttribute) -> {
         return Arrays.stream(annotationsAttribute.getAnnotations());
      }).map(Annotation::getTypeName).collect(Collectors.toList()) : Collections.emptyList();
   }

   private List<String> getAnnotationNames(Annotation[] annotations) {
      return (List)Arrays.stream(annotations).map(Annotation::getTypeName).collect(Collectors.toList());
   }

   private List<String> splitDescriptorToTypeNames(String descriptors) {
      List<String> result = new ArrayList();
      if (descriptors != null && descriptors.length() != 0) {
         List<Integer> indices = new ArrayList();
         Descriptor.Iterator iterator = new Descriptor.Iterator(descriptors);

         while(iterator.hasNext()) {
            indices.add(iterator.next());
         }

         indices.add(descriptors.length());
         result = (List)IntStream.range(0, indices.size() - 1).mapToObj((i) -> {
            return Descriptor.toString(descriptors.substring((Integer)indices.get(i), (Integer)indices.get(i + 1)));
         }).collect(Collectors.toList());
      }

      return (List)result;
   }
}
