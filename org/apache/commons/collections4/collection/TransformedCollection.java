package org.apache.commons.collections4.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.Transformer;

public class TransformedCollection<E> extends AbstractCollectionDecorator<E> {
   private static final long serialVersionUID = 8692300188161871514L;
   protected final Transformer<? super E, ? extends E> transformer;

   public static <E> TransformedCollection<E> transformingCollection(Collection<E> coll, Transformer<? super E, ? extends E> transformer) {
      return new TransformedCollection(coll, transformer);
   }

   public static <E> TransformedCollection<E> transformedCollection(Collection<E> collection, Transformer<? super E, ? extends E> transformer) {
      TransformedCollection<E> decorated = new TransformedCollection(collection, transformer);
      if (collection.size() > 0) {
         E[] values = (Object[])collection.toArray();
         collection.clear();
         Object[] arr$ = values;
         int len$ = values.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            E value = arr$[i$];
            decorated.decorated().add(transformer.transform(value));
         }
      }

      return decorated;
   }

   protected TransformedCollection(Collection<E> coll, Transformer<? super E, ? extends E> transformer) {
      super(coll);
      if (transformer == null) {
         throw new NullPointerException("Transformer must not be null");
      } else {
         this.transformer = transformer;
      }
   }

   protected E transform(E object) {
      return this.transformer.transform(object);
   }

   protected Collection<E> transform(Collection<? extends E> coll) {
      List<E> list = new ArrayList(coll.size());
      Iterator i$ = coll.iterator();

      while(i$.hasNext()) {
         E item = i$.next();
         list.add(this.transform(item));
      }

      return list;
   }

   public boolean add(E object) {
      return this.decorated().add(this.transform(object));
   }

   public boolean addAll(Collection<? extends E> coll) {
      return this.decorated().addAll(this.transform(coll));
   }
}
