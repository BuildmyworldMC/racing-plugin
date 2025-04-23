package com.github.hornta.racing.messenger.org.reflections;

import com.github.hornta.racing.messenger.org.reflections.adapters.MetadataAdapter;
import com.github.hornta.racing.messenger.org.reflections.scanners.Scanner;
import com.github.hornta.racing.messenger.org.reflections.serializers.Serializer;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

public interface Configuration {
   Set<Scanner> getScanners();

   Set<URL> getUrls();

   MetadataAdapter getMetadataAdapter();

   Predicate<String> getInputsFilter();

   ExecutorService getExecutorService();

   Serializer getSerializer();

   ClassLoader[] getClassLoaders();

   boolean shouldExpandSuperTypes();
}
