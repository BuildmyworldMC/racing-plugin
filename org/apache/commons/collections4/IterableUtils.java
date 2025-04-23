package org.apache.commons.collections4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.functors.EqualPredicate;
import org.apache.commons.collections4.iterators.LazyIteratorChain;
import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.collections4.iterators.UniqueFilterIterator;

public class IterableUtils {
   static final FluentIterable EMPTY_ITERABLE = new FluentIterable<Object>() {
      public Iterator<Object> iterator() {
         return IteratorUtils.emptyIterator();
      }
   };

   public static <E> Iterable<E> emptyIterable() {
      return EMPTY_ITERABLE;
   }

   public static <E> Iterable<E> chainedIterable(Iterable<? extends E> a, Iterable<? extends E> b) {
      return chainedIterable(a, b);
   }

   public static <E> Iterable<E> chainedIterable(Iterable<? extends E> a, Iterable<? extends E> b, Iterable<? extends E> c) {
      return chainedIterable(a, b, c);
   }

   public static <E> Iterable<E> chainedIterable(Iterable<? extends E> a, Iterable<? extends E> b, Iterable<? extends E> c, Iterable<? extends E> d) {
      return chainedIterable(a, b, c, d);
   }

   public static <E> Iterable<E> chainedIterable(final Iterable<? extends E>... iterables) {
      checkNotNull(iterables);
      return new FluentIterable<E>() {
         public Iterator<E> iterator() {
            return new LazyIteratorChain<E>() {
               protected Iterator<? extends E> nextIterator(int count) {
                  return count > iterables.length ? null : iterables[count - 1].iterator();
               }
            };
         }
      };
   }

   public static <E> Iterable<E> collatedIterable(final Iterable<? extends E> a, final Iterable<? extends E> b) {
      checkNotNull(a, b);
      return new FluentIterable<E>() {
         public Iterator<E> iterator() {
            return IteratorUtils.collatedIterator((Comparator)null, a.iterator(), b.iterator());
         }
      };
   }

   public static <E> Iterable<E> collatedIterable(final Comparator<? super E> comparator, final Iterable<? extends E> a, final Iterable<? extends E> b) {
      checkNotNull(a, b);
      return new FluentIterable<E>() {
         public Iterator<E> iterator() {
            return IteratorUtils.collatedIterator(comparator, a.iterator(), b.iterator());
         }
      };
   }

   public static <E> Iterable<E> filteredIterable(final Iterable<E> iterable, final Predicate<? super E> predicate) {
      checkNotNull(iterable);
      if (predicate == null) {
         throw new NullPointerException("Predicate must not be null.");
      } else {
         return new FluentIterable<E>() {
            public Iterator<E> iterator() {
               return IteratorUtils.filteredIterator(IterableUtils.emptyIteratorIfNull(iterable), predicate);
            }
         };
      }
   }

   public static <E> Iterable<E> boundedIterable(final Iterable<E> iterable, final long maxSize) {
      checkNotNull(iterable);
      if (maxSize < 0L) {
         throw new IllegalArgumentException("MaxSize parameter must not be negative.");
      } else {
         return new FluentIterable<E>() {
            public Iterator<E> iterator() {
               return IteratorUtils.boundedIterator(iterable.iterator(), maxSize);
            }
         };
      }
   }

   public static <E> Iterable<E> loopingIterable(final Iterable<E> iterable) {
      checkNotNull(iterable);
      return new FluentIterable<E>() {
         public Iterator<E> iterator() {
            return new LazyIteratorChain<E>() {
               protected Iterator<? extends E> nextIterator(int count) {
                  return IterableUtils.isEmpty(iterable) ? null : iterable.iterator();
               }
            };
         }
      };
   }

   public static <E> Iterable<E> reversedIterable(final Iterable<E> iterable) {
      checkNotNull(iterable);
      return new FluentIterable<E>() {
         public Iterator<E> iterator() {
            List<E> list = iterable instanceof List ? (List)iterable : IteratorUtils.toList(iterable.iterator());
            return new ReverseListIterator(list);
         }
      };
   }

   public static <E> Iterable<E> skippingIterable(final Iterable<E> iterable, final long elementsToSkip) {
      checkNotNull(iterable);
      if (elementsToSkip < 0L) {
         throw new IllegalArgumentException("ElementsToSkip parameter must not be negative.");
      } else {
         return new FluentIterable<E>() {
            public Iterator<E> iterator() {
               return IteratorUtils.skippingIterator(iterable.iterator(), elementsToSkip);
            }
         };
      }
   }

   public static <I, O> Iterable<O> transformedIterable(final Iterable<I> iterable, final Transformer<? super I, ? extends O> transformer) {
      checkNotNull(iterable);
      if (transformer == null) {
         throw new NullPointerException("Transformer must not be null.");
      } else {
         return new FluentIterable<O>() {
            public Iterator<O> iterator() {
               return IteratorUtils.transformedIterator(iterable.iterator(), transformer);
            }
         };
      }
   }

   public static <E> Iterable<E> uniqueIterable(final Iterable<E> iterable) {
      checkNotNull(iterable);
      return new FluentIterable<E>() {
         public Iterator<E> iterator() {
            return new UniqueFilterIterator(iterable.iterator());
         }
      };
   }

   public static <E> Iterable<E> unmodifiableIterable(Iterable<E> iterable) {
      checkNotNull(iterable);
      return (Iterable)(iterable instanceof IterableUtils.UnmodifiableIterable ? iterable : new IterableUtils.UnmodifiableIterable(iterable));
   }

   public static <E> Iterable<E> zippingIterable(final Iterable<? extends E> a, final Iterable<? extends E> b) {
      checkNotNull(a);
      checkNotNull(b);
      return new FluentIterable<E>() {
         public Iterator<E> iterator() {
            return IteratorUtils.zippingIterator(a.iterator(), b.iterator());
         }
      };
   }

   public static <E> Iterable<E> zippingIterable(final Iterable<? extends E> first, final Iterable<? extends E>... others) {
      checkNotNull(first);
      checkNotNull(others);
      return new FluentIterable<E>() {
         public Iterator<E> iterator() {
            Iterator<? extends E>[] iterators = new Iterator[others.length + 1];
            iterators[0] = first.iterator();

            for(int i = 0; i < others.length; ++i) {
               iterators[i + 1] = others[i].iterator();
            }

            return IteratorUtils.zippingIterator(iterators);
         }
      };
   }

   public static <E> Iterable<E> emptyIfNull(Iterable<E> iterable) {
      return iterable == null ? emptyIterable() : iterable;
   }

   public static <E> void forEach(Iterable<E> iterable, Closure<? super E> closure) {
      IteratorUtils.forEach(emptyIteratorIfNull(iterable), closure);
   }

   public static <E> E forEachButLast(Iterable<E> iterable, Closure<? super E> closure) {
      return IteratorUtils.forEachButLast(emptyIteratorIfNull(iterable), closure);
   }

   public static <E> E find(Iterable<E> iterable, Predicate<? super E> predicate) {
      return IteratorUtils.find(emptyIteratorIfNull(iterable), predicate);
   }

   public static <E> int indexOf(Iterable<E> iterable, Predicate<? super E> predicate) {
      return IteratorUtils.indexOf(emptyIteratorIfNull(iterable), predicate);
   }

   public static <E> boolean matchesAll(Iterable<E> iterable, Predicate<? super E> predicate) {
      return IteratorUtils.matchesAll(emptyIteratorIfNull(iterable), predicate);
   }

   public static <E> boolean matchesAny(Iterable<E> iterable, Predicate<? super E> predicate) {
      return IteratorUtils.matchesAny(emptyIteratorIfNull(iterable), predicate);
   }

   public static <E> long countMatches(Iterable<E> input, Predicate<? super E> predicate) {
      if (predicate == null) {
         throw new NullPointerException("Predicate must not be null.");
      } else {
         return (long)size(filteredIterable(emptyIfNull(input), predicate));
      }
   }

   public static boolean isEmpty(Iterable<?> iterable) {
      return iterable instanceof Collection ? ((Collection)iterable).isEmpty() : IteratorUtils.isEmpty(emptyIteratorIfNull(iterable));
   }

   public static <E> boolean contains(Iterable<E> iterable, Object object) {
      return iterable instanceof Collection ? ((Collection)iterable).contains(object) : IteratorUtils.contains(emptyIteratorIfNull(iterable), object);
   }

   public static <E> boolean contains(Iterable<? extends E> iterable, E object, Equator<? super E> equator) {
      if (equator == null) {
         throw new NullPointerException("Equator must not be null.");
      } else {
         return matchesAny(iterable, EqualPredicate.equalPredicate(object, equator));
      }
   }

   public static <E, T extends E> int frequency(Iterable<E> iterable, T obj) {
      if (iterable instanceof Set) {
         return ((Set)iterable).contains(obj) ? 1 : 0;
      } else {
         return iterable instanceof Bag ? ((Bag)iterable).getCount(obj) : size(filteredIterable(emptyIfNull(iterable), EqualPredicate.equalPredicate(obj)));
      }
   }

   public static <T> T get(Iterable<T> iterable, int index) {
      CollectionUtils.checkIndexBounds(index);
      return iterable instanceof List ? ((List)iterable).get(index) : IteratorUtils.get(emptyIteratorIfNull(iterable), index);
   }

   public static int size(Iterable<?> iterable) {
      return iterable instanceof Collection ? ((Collection)iterable).size() : IteratorUtils.size(emptyIteratorIfNull(iterable));
   }

   public static <O> List<List<O>> partition(Iterable<? extends O> iterable, Predicate<? super O> predicate) {
      if (predicate == null) {
         throw new NullPointerException("Predicate must not be null.");
      } else {
         Factory<List<O>> factory = FactoryUtils.instantiateFactory(ArrayList.class);
         Predicate<? super O>[] predicates = new Predicate[]{predicate};
         return partition(iterable, factory, predicates);
      }
   }

   public static <O> List<List<O>> partition(Iterable<? extends O> iterable, Predicate<? super O>... predicates) {
      Factory<List<O>> factory = FactoryUtils.instantiateFactory(ArrayList.class);
      return partition(iterable, factory, predicates);
   }

   public static <O, R extends Collection<O>> List<R> partition(Iterable<? extends O> iterable, Factory<R> partitionFactory, Predicate<? super O>... predicates) {
      if (iterable == null) {
         Iterable<O> empty = emptyIterable();
         return partition(empty, partitionFactory, predicates);
      } else if (predicates == null) {
         throw new NullPointerException("Predicates must not be null.");
      } else {
         Predicate[] arr$ = predicates;
         int numberOfPartitions = predicates.length;

         for(int i$ = 0; i$ < numberOfPartitions; ++i$) {
            Predicate<?> p = arr$[i$];
            if (p == null) {
               throw new NullPointerException("Predicate must not be null.");
            }
         }

         if (predicates.length < 1) {
            R singlePartition = (Collection)partitionFactory.create();
            CollectionUtils.addAll(singlePartition, iterable);
            return Collections.singletonList(singlePartition);
         } else {
            int numberOfPredicates = predicates.length;
            numberOfPartitions = numberOfPredicates + 1;
            List<R> partitions = new ArrayList(numberOfPartitions);

            for(int i = 0; i < numberOfPartitions; ++i) {
               partitions.add(partitionFactory.create());
            }

            Iterator i$ = iterable.iterator();

            while(i$.hasNext()) {
               O element = i$.next();
               boolean elementAssigned = false;

               for(int i = 0; i < numberOfPredicates; ++i) {
                  if (predicates[i].evaluate(element)) {
                     ((Collection)partitions.get(i)).add(element);
                     elementAssigned = true;
                     break;
                  }
               }

               if (!elementAssigned) {
                  ((Collection)partitions.get(numberOfPredicates)).add(element);
               }
            }

            return partitions;
         }
      }
   }

   public static <E> List<E> toList(Iterable<E> iterable) {
      return IteratorUtils.toList(emptyIteratorIfNull(iterable));
   }

   public static <E> String toString(Iterable<E> iterable) {
      return IteratorUtils.toString(emptyIteratorIfNull(iterable));
   }

   public static <E> String toString(Iterable<E> iterable, Transformer<? super E, String> transformer) {
      if (transformer == null) {
         throw new NullPointerException("Transformer must not be null.");
      } else {
         return IteratorUtils.toString(emptyIteratorIfNull(iterable), transformer);
      }
   }

   public static <E> String toString(Iterable<E> iterable, Transformer<? super E, String> transformer, String delimiter, String prefix, String suffix) {
      return IteratorUtils.toString(emptyIteratorIfNull(iterable), transformer, delimiter, prefix, suffix);
   }

   static void checkNotNull(Iterable<?> iterable) {
      if (iterable == null) {
         throw new NullPointerException("Iterable must not be null.");
      }
   }

   static void checkNotNull(Iterable<?>... iterables) {
      if (iterables == null) {
         throw new NullPointerException("Iterables must not be null.");
      } else {
         Iterable[] arr$ = iterables;
         int len$ = iterables.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Iterable<?> iterable = arr$[i$];
            checkNotNull(iterable);
         }

      }
   }

   private static <E> Iterator<E> emptyIteratorIfNull(Iterable<E> iterable) {
      return (Iterator)(iterable != null ? iterable.iterator() : IteratorUtils.emptyIterator());
   }

   private static final class UnmodifiableIterable<E> extends FluentIterable<E> {
      private final Iterable<E> unmodifiable;

      public UnmodifiableIterable(Iterable<E> iterable) {
         this.unmodifiable = iterable;
      }

      public Iterator<E> iterator() {
         return IteratorUtils.unmodifiableIterator(this.unmodifiable.iterator());
      }
   }
}
