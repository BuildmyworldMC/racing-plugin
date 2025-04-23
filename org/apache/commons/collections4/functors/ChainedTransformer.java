package org.apache.commons.collections4.functors;

import java.io.Serializable;
import java.util.Collection;
import org.apache.commons.collections4.Transformer;

public class ChainedTransformer<T> implements Transformer<T, T>, Serializable {
   private static final long serialVersionUID = 3514945074733160196L;
   private final Transformer<? super T, ? extends T>[] iTransformers;

   public static <T> Transformer<T, T> chainedTransformer(Transformer<? super T, ? extends T>... transformers) {
      FunctorUtils.validate(transformers);
      return (Transformer)(transformers.length == 0 ? NOPTransformer.nopTransformer() : new ChainedTransformer(transformers));
   }

   public static <T> Transformer<T, T> chainedTransformer(Collection<? extends Transformer<? super T, ? extends T>> transformers) {
      if (transformers == null) {
         throw new NullPointerException("Transformer collection must not be null");
      } else if (transformers.size() == 0) {
         return NOPTransformer.nopTransformer();
      } else {
         Transformer<T, T>[] cmds = (Transformer[])transformers.toArray(new Transformer[transformers.size()]);
         FunctorUtils.validate(cmds);
         return new ChainedTransformer(false, cmds);
      }
   }

   private ChainedTransformer(boolean clone, Transformer<? super T, ? extends T>[] transformers) {
      this.iTransformers = clone ? FunctorUtils.copy(transformers) : transformers;
   }

   public ChainedTransformer(Transformer<? super T, ? extends T>... transformers) {
      this(true, transformers);
   }

   public T transform(T object) {
      Transformer[] arr$ = this.iTransformers;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Transformer<? super T, ? extends T> iTransformer = arr$[i$];
         object = iTransformer.transform(object);
      }

      return object;
   }

   public Transformer<? super T, ? extends T>[] getTransformers() {
      return FunctorUtils.copy(this.iTransformers);
   }
}
