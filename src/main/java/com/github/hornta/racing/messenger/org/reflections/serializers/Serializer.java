package com.github.hornta.racing.messenger.org.reflections.serializers;

import com.github.hornta.racing.messenger.org.reflections.Reflections;
import java.io.File;
import java.io.InputStream;

public interface Serializer {
   Reflections read(InputStream var1);

   File save(Reflections var1, String var2);

   String toString(Reflections var1);
}
