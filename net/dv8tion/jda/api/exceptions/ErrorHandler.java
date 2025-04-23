package net.dv8tion.jda.api.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.utils.Checks;

public class ErrorHandler implements Consumer<Throwable> {
   private static final Consumer<? super Throwable> empty = (e) -> {
   };
   private final Consumer<? super Throwable> base;
   private final Map<Predicate<? super Throwable>, Consumer<? super Throwable>> cases;

   public ErrorHandler() {
      this(RestAction.getDefaultFailure());
   }

   public ErrorHandler(@Nonnull Consumer<? super Throwable> base) {
      this.cases = new LinkedHashMap();
      Checks.notNull(base, "Consumer");
      this.base = base;
   }

   @Nonnull
   public ErrorHandler ignore(@Nonnull ErrorResponse ignored, @Nonnull ErrorResponse... errorResponses) {
      Checks.notNull(ignored, "ErrorResponse");
      Checks.noneNull((Object[])errorResponses, "ErrorResponse");
      return this.ignore((Collection)EnumSet.of(ignored, errorResponses));
   }

   @Nonnull
   public ErrorHandler ignore(@Nonnull Collection<ErrorResponse> errorResponses) {
      return this.handle(errorResponses, empty);
   }

   @Nonnull
   public ErrorHandler ignore(@Nonnull Class<?> clazz, @Nonnull Class<?>... classes) {
      Checks.notNull(clazz, "Classes");
      Checks.noneNull((Object[])classes, "Classes");
      return this.ignore((it) -> {
         if (clazz.isInstance(it)) {
            return true;
         } else {
            Class[] var3 = classes;
            int var4 = classes.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               Class<?> e = var3[var5];
               if (e.isInstance(it)) {
                  return true;
               }
            }

            return false;
         }
      });
   }

   @Nonnull
   public ErrorHandler ignore(@Nonnull Predicate<? super Throwable> condition) {
      return this.handle(condition, empty);
   }

   @Nonnull
   public ErrorHandler handle(@Nonnull ErrorResponse response, @Nonnull Consumer<? super ErrorResponseException> handler) {
      Checks.notNull(response, "ErrorResponse");
      return this.handle((Collection)EnumSet.of(response), handler);
   }

   @Nonnull
   public ErrorHandler handle(@Nonnull Collection<ErrorResponse> errorResponses, @Nonnull Consumer<? super ErrorResponseException> handler) {
      Checks.notNull(handler, "Handler");
      Checks.noneNull(errorResponses, "ErrorResponse");
      return this.handle(ErrorResponseException.class, (it) -> {
         return errorResponses.contains(it.getErrorResponse());
      }, handler);
   }

   @Nonnull
   public <T> ErrorHandler handle(@Nonnull Class<T> clazz, @Nonnull Consumer<? super T> handler) {
      Checks.notNull(clazz, "Class");
      Checks.notNull(handler, "Handler");
      Objects.requireNonNull(clazz);
      return this.handle(clazz::isInstance, (ex) -> {
         handler.accept(clazz.cast(ex));
      });
   }

   @Nonnull
   public <T> ErrorHandler handle(@Nonnull Class<T> clazz, @Nonnull Predicate<? super T> condition, @Nonnull Consumer<? super T> handler) {
      Checks.notNull(clazz, "Class");
      Checks.notNull(handler, "Handler");
      return this.handle((it) -> {
         return clazz.isInstance(it) && condition.test(clazz.cast(it));
      }, (ex) -> {
         handler.accept(clazz.cast(ex));
      });
   }

   @Nonnull
   public ErrorHandler handle(@Nonnull Collection<Class<?>> clazz, @Nullable Predicate<? super Throwable> condition, @Nonnull Consumer<? super Throwable> handler) {
      Checks.noneNull(clazz, "Class");
      Checks.notNull(handler, "Handler");
      List<Class<?>> classes = new ArrayList(clazz);
      Predicate<? super Throwable> check = (it) -> {
         return classes.stream().anyMatch((c) -> {
            return c.isInstance(it);
         }) && (condition == null || condition.test(it));
      };
      return this.handle(check, handler);
   }

   @Nonnull
   public ErrorHandler handle(@Nonnull Predicate<? super Throwable> condition, @Nonnull Consumer<? super Throwable> handler) {
      Checks.notNull(condition, "Condition");
      Checks.notNull(handler, "Handler");
      this.cases.put(condition, handler);
      return this;
   }

   public void accept(Throwable t) {
      Iterator var2 = this.cases.entrySet().iterator();

      Predicate condition;
      Consumer callback;
      do {
         if (!var2.hasNext()) {
            this.base.accept(t);
            return;
         }

         Entry<Predicate<? super Throwable>, Consumer<? super Throwable>> entry = (Entry)var2.next();
         condition = (Predicate)entry.getKey();
         callback = (Consumer)entry.getValue();
      } while(!condition.test(t));

      callback.accept(t);
   }
}
