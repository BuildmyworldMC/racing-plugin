package com.github.hornta.racing.messenger;

import com.github.hornta.racing.messenger.org.reflections.Reflections;
import com.github.hornta.racing.messenger.org.reflections.scanners.ResourcesScanner;
import com.github.hornta.racing.messenger.org.reflections.scanners.Scanner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Translations {
   private static final Pattern ymlResources = Pattern.compile(".+\\.yml$");
   private final Pattern translationResource;
   private final Map<String, File> languageFiles;
   private final File translationsDirectory;
   private final JavaPlugin plugin;
   private final MessageManager messageManager;

   public Translations(JavaPlugin plugin, MessageManager messageManager) {
      this.plugin = plugin;
      this.messageManager = messageManager;
      this.translationResource = Pattern.compile("^translations/" + plugin.getDescription().getName() + "/.+\\.yml$");
      this.languageFiles = new HashMap<>();
      this.translationsDirectory = new File(plugin.getDataFolder() + File.separator + "translations");
      this.saveDefaults();
      this.readLanguageFiles(this.translationsDirectory);
   }

   private Map<String, InputStream> getResourceTranslationStreams() {
      Map<String, InputStream> streams = new HashMap<>();
      Reflections reflections = new Reflections(null, new Scanner[]{new ResourcesScanner()});
      Set<String> resourceList = reflections.getResources(ymlResources);
       for (String resource : resourceList) {
           String filename = resource.lastIndexOf("/") == -1 ? resource : resource.substring(resource.lastIndexOf("/") + 1);
           String path = ((String[]) reflections.getStore().get(ResourcesScanner.class, filename).toArray(new String[0]))[0];
           if (this.translationResource.matcher(path).matches()) {
               streams.put(path, this.getClass().getClassLoader().getResourceAsStream(path));
           }
       }

      return streams;
   }

   public Translation createTranslation(String language) throws MessengerException {
      if (!this.languageFiles.containsKey(language)) {
         throw new MessengerException("Couldn't find translation `%s`", (Object) new String[]{language});
      } else {
         Translation translation = new Translation(this.languageFiles.get(language), language);
         translation.load(this.plugin, this.messageManager.getIdentifiers());
         return translation;
      }
   }

   private void readLanguageFiles(File translationsDirectory) {
      File[] files = translationsDirectory.listFiles();
      if (files != null) {
          for (File file : files) {
              if (!file.isFile()) {
                  this.readLanguageFiles(file);
              } else if (file.getName().endsWith(".yml")) {
                  this.languageFiles.put(getFilenameWithoutExtension(file), file);
              }
          }
      }
   }

   public void saveDefaults() {
       this.translationsDirectory.mkdirs();
       for (Entry<String, InputStream> entry : this.getResourceTranslationStreams().entrySet()) {
           try {
               File destination = new File(this.plugin.getDataFolder(), (entry.getKey()).replaceAll(this.plugin.getDescription().getName() + "/", ""));
               boolean result = this.saveTranslationResource(entry, destination);
               if (!result) {
                   return;
               }
           } catch (IllegalArgumentException ignored) {
           }
       }
   }

   private boolean saveTranslationResource(Entry<String, InputStream> resource, File dest) {
      if (dest.exists()) {
         this.plugin.getLogger().log(Level.INFO, "Found existing translation file in " + dest.getName() + ".");
         YamlConfiguration destYaml = YamlConfiguration.loadConfiguration(dest);
         YamlConfiguration resourceYaml = YamlConfiguration.loadConfiguration(new InputStreamReader(resource.getValue(), StandardCharsets.UTF_8));
         Iterator<String> iterator = destYaml.getKeys(true).iterator();

         String key;
         while (iterator.hasNext()) {
            key = iterator.next();
            if (!resourceYaml.contains(key)) {
               destYaml.set(key, null);
               this.plugin.getLogger().log(Level.INFO, "Deleted unused key `" + key + "` from `" + dest.getName() + "`.");
            }
         }

         iterator = resourceYaml.getKeys(true).iterator();

         while (iterator.hasNext()) {
            key = iterator.next();
            if (!destYaml.isSet(key)) {
               destYaml.set(key, resourceYaml.get(key));
               this.plugin.getLogger().log(Level.INFO, "Added missing key `" + key + "` to `" + dest.getName() + "`.");
            }
         }

         try {
            destYaml.save(dest);
         } catch (IOException exception) {
            this.plugin.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
            return false;
         }
      } else {
         try {
            this.saveResource(resource.getKey(), (resource.getKey()).replaceAll(this.plugin.getDescription().getName() + "/", ""));
         } catch (IllegalArgumentException exception) {
            this.plugin.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
         }

         this.plugin.getLogger().log(Level.INFO, "Saving new translation file to `" + dest.getName() + "`");
      }

      return true;
   }

   public void saveResource(String resourcePath, String outPath) {
      if (resourcePath != null && !resourcePath.isEmpty()) {
         resourcePath = resourcePath.replace('\\', '/');
         InputStream in = this.plugin.getResource(resourcePath);
         if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
         } else {
            File outFile = new File(this.plugin.getDataFolder(), outPath);
            int lastIndex = outPath.lastIndexOf(47);
            File outDir = new File(this.plugin.getDataFolder(), outPath.substring(0, Math.max(lastIndex, 0)));
            if (!outDir.exists()) {
               outDir.mkdirs();
            }

            try {
               if (!outFile.exists()) {
                  OutputStream out = new FileOutputStream(outFile);
                  byte[] buf = new byte[1024];

                  int len;
                  while((len = in.read(buf)) > 0) {
                     out.write(buf, 0, len);
                  }

                  out.close();
                  in.close();
               } else {
                  this.plugin.getLogger().log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
               }
            } catch (IOException exception) {
               this.plugin.getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, exception);
            }

         }
      } else {
         throw new IllegalArgumentException("ResourcePath cannot be null or empty");
      }
   }

   private static String getFilenameWithoutExtension(File file) {
      String filename = file.getName();
      int lastDotIndex = filename.lastIndexOf(46);
      return lastDotIndex == -1 ? filename : filename.substring(0, lastDotIndex);
   }
}
