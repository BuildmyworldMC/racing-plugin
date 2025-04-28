package com.github.hornta.racing.messenger.org.reflections.util;

import com.github.hornta.racing.messenger.org.reflections.ReflectionsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class FilterBuilder implements Predicate<String> {
   private final List<Predicate<String>> chain;

   public FilterBuilder() {
      this.chain = new ArrayList();
   }

   private FilterBuilder(Collection<Predicate<String>> filters) {
      this.chain = new ArrayList(filters);
   }

   public FilterBuilder include(String regex) {
      return this.add(new FilterBuilder.Include(regex));
   }

   public FilterBuilder exclude(String regex) {
      this.add(new FilterBuilder.Exclude(regex));
      return this;
   }

   public FilterBuilder add(Predicate<String> filter) {
      this.chain.add(filter);
      return this;
   }

   public FilterBuilder includePackage(Class<?> aClass) {
      return this.add(new FilterBuilder.Include(packageNameRegex(aClass)));
   }

   public FilterBuilder excludePackage(Class<?> aClass) {
      return this.add(new FilterBuilder.Exclude(packageNameRegex(aClass)));
   }

   public FilterBuilder includePackage(String... prefixes) {
      String[] var2 = prefixes;
      int var3 = prefixes.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         String prefix = var2[var4];
         this.add(new FilterBuilder.Include(prefix(prefix)));
      }

      return this;
   }

   public FilterBuilder excludePackage(String prefix) {
      return this.add(new FilterBuilder.Exclude(prefix(prefix)));
   }

   private static String packageNameRegex(Class<?> aClass) {
      return prefix(aClass.getPackage().getName() + ".");
   }

   public static String prefix(String qualifiedName) {
      return qualifiedName.replace(".", "\\.") + ".*";
   }

   public String toString() {
      return Utils.join(this.chain, ", ");
   }

   public boolean test(String regex) {
      boolean accept = this.chain.isEmpty() || this.chain.get(0) instanceof FilterBuilder.Exclude;
      Iterator var3 = this.chain.iterator();

      while(var3.hasNext()) {
         Predicate<String> filter = (Predicate)var3.next();
         if ((!accept || !(filter instanceof FilterBuilder.Include)) && (accept || !(filter instanceof FilterBuilder.Exclude))) {
            accept = filter.test(regex);
            if (!accept && filter instanceof FilterBuilder.Exclude) {
               break;
            }
         }
      }

      return accept;
   }

   public static FilterBuilder parse(String includeExcludeString) {
      List<Predicate<String>> filters = new ArrayList();
      if (!Utils.isEmpty(includeExcludeString)) {
         String[] var2 = includeExcludeString.split(",");
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            String string = var2[var4];
            String trimmed = string.trim();
            char prefix = trimmed.charAt(0);
            String pattern = trimmed.substring(1);
            Object filter;
            switch(prefix) {
            case '+':
               filter = new FilterBuilder.Include(pattern);
               break;
            case '-':
               filter = new FilterBuilder.Exclude(pattern);
               break;
            default:
               throw new ReflectionsException("includeExclude should start with either + or -");
            }

            filters.add((Predicate<String>) filter);
         }

         return new FilterBuilder(filters);
      } else {
         return new FilterBuilder();
      }
   }

   public static FilterBuilder parsePackages(String includeExcludeString) {
      List<Predicate<String>> filters = new ArrayList();
      if (!Utils.isEmpty(includeExcludeString)) {
         String[] var2 = includeExcludeString.split(",");
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            String string = var2[var4];
            String trimmed = string.trim();
            char prefix = trimmed.charAt(0);
            String pattern = trimmed.substring(1);
            if (!pattern.endsWith(".")) {
               pattern = pattern + ".";
            }

            pattern = prefix(pattern);
            Object filter;
            switch(prefix) {
            case '+':
               filter = new FilterBuilder.Include(pattern);
               break;
            case '-':
               filter = new FilterBuilder.Exclude(pattern);
               break;
            default:
               throw new ReflectionsException("includeExclude should start with either + or -");
            }

            filters.add((Predicate<String>) filter);
         }

         return new FilterBuilder(filters);
      } else {
         return new FilterBuilder();
      }
   }

   public static class Exclude extends FilterBuilder.Matcher {
      public Exclude(String patternString) {
         super(patternString);
      }

      public boolean test(String regex) {
         return !this.pattern.matcher(regex).matches();
      }

      public String toString() {
         return "-" + super.toString();
      }
   }

   public static class Include extends FilterBuilder.Matcher {
      public Include(String patternString) {
         super(patternString);
      }

      public boolean test(String regex) {
         return this.pattern.matcher(regex).matches();
      }

      public String toString() {
         return "+" + super.toString();
      }
   }

   public abstract static class Matcher implements Predicate<String> {
      final Pattern pattern;

      public Matcher(String regex) {
         this.pattern = Pattern.compile(regex);
      }

      public String toString() {
         return this.pattern.pattern();
      }
   }
}
