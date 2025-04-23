package org.apache.commons.collections4.functors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.FunctorException;

public class PrototypeFactory {
   public static <T> Factory<T> prototypeFactory(T prototype) {
      if (prototype == null) {
         return ConstantFactory.constantFactory((Object)null);
      } else {
         try {
            Method method = prototype.getClass().getMethod("clone", (Class[])null);
            return new PrototypeFactory.PrototypeCloneFactory(prototype, method);
         } catch (NoSuchMethodException var4) {
            try {
               prototype.getClass().getConstructor(prototype.getClass());
               return new InstantiateFactory(prototype.getClass(), new Class[]{prototype.getClass()}, new Object[]{prototype});
            } catch (NoSuchMethodException var3) {
               if (prototype instanceof Serializable) {
                  return new PrototypeFactory.PrototypeSerializationFactory((Serializable)prototype);
               } else {
                  throw new IllegalArgumentException("The prototype must be cloneable via a public clone method");
               }
            }
         }
      }
   }

   private PrototypeFactory() {
   }

   static class PrototypeSerializationFactory<T extends Serializable> implements Factory<T> {
      private final T iPrototype;

      private PrototypeSerializationFactory(T prototype) {
         this.iPrototype = prototype;
      }

      public T create() {
         ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
         ByteArrayInputStream bais = null;

         Serializable var5;
         try {
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(this.iPrototype);
            bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bais);
            var5 = (Serializable)in.readObject();
         } catch (ClassNotFoundException var18) {
            throw new FunctorException(var18);
         } catch (IOException var19) {
            throw new FunctorException(var19);
         } finally {
            try {
               if (bais != null) {
                  bais.close();
               }
            } catch (IOException var17) {
            }

            try {
               baos.close();
            } catch (IOException var16) {
            }

         }

         return var5;
      }

      // $FF: synthetic method
      PrototypeSerializationFactory(Serializable x0, Object x1) {
         this(x0);
      }
   }

   static class PrototypeCloneFactory<T> implements Factory<T> {
      private final T iPrototype;
      private transient Method iCloneMethod;

      private PrototypeCloneFactory(T prototype, Method method) {
         this.iPrototype = prototype;
         this.iCloneMethod = method;
      }

      private void findCloneMethod() {
         try {
            this.iCloneMethod = this.iPrototype.getClass().getMethod("clone", (Class[])null);
         } catch (NoSuchMethodException var2) {
            throw new IllegalArgumentException("PrototypeCloneFactory: The clone method must exist and be public ");
         }
      }

      public T create() {
         if (this.iCloneMethod == null) {
            this.findCloneMethod();
         }

         try {
            return this.iCloneMethod.invoke(this.iPrototype, (Object[])null);
         } catch (IllegalAccessException var2) {
            throw new FunctorException("PrototypeCloneFactory: Clone method must be public", var2);
         } catch (InvocationTargetException var3) {
            throw new FunctorException("PrototypeCloneFactory: Clone method threw an exception", var3);
         }
      }

      // $FF: synthetic method
      PrototypeCloneFactory(Object x0, Method x1, Object x2) {
         this(x0, x1);
      }
   }
}
