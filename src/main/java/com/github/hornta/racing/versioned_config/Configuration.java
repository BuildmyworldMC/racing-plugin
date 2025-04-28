package com.github.hornta.racing.versioned_config;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class Configuration<T extends Enum<T>> {
   private final NodeMap<T> nodes2;
   private final File file;
   private YamlConfiguration configuration;
   private final List<Migration<T>> migrations;
   private Snapshot<T> snapshot;
   protected static final String VERSION_FIELD = "version";

   public Configuration(File file, List<Migration<T>> migrations) {
      this.file = file;
      this.nodes2 = new NodeMap();
      this.migrations = migrations;
   }

   public void reload() throws ConfigurationException {
      try {
         this.loadOrCreateConfigurationFromFile();
         this.validateConfiguration();
         int currentCfgVersion = this.configuration.getInt("version");
         YamlConfiguration defaultSnapshotCfg = new YamlConfiguration();
         defaultSnapshotCfg.set("version", 0);
         NodeMap<T> defaultSnapshotNodes = this.getNodeMapBeforeVersion(currentCfgVersion, defaultSnapshotCfg);
         Snapshot<T> defaultSnapshot = new Snapshot(defaultSnapshotNodes, defaultSnapshotCfg);
         defaultSnapshot.applyConfiguration(this.configuration);
         Iterator var5 = this.migrations.iterator();

         while(var5.hasNext()) {
            Migration<T> migration = (Migration)var5.next();
            if (migration.getVersion() > currentCfgVersion) {
               Patch<T> patch = migration.createPatch(defaultSnapshot);
               defaultSnapshot.update(patch);
               defaultSnapshot.getConfiguration().set("version", migration.getVersion());
            }
         }

         if (!this.migrations.isEmpty()) {
            defaultSnapshot.getConfiguration().set("version", ((Migration)this.migrations.get(this.migrations.size() - 1)).getVersion());
         }

         this.snapshot = defaultSnapshot;
         this.configuration = this.snapshot.getConfiguration();
         this.cleanup();
         this.persistToFile();
      } catch (InvalidConfigurationException | IOException var8) {
         throw new ConfigurationException(var8);
      }
   }

   public <E> E get(T id) {
      return this.snapshot.get(id);
   }

   private void persistToFile() throws IOException {
      this.configuration.save(this.file);
   }

   private void loadOrCreateConfigurationFromFile() throws IOException, InvalidConfigurationException {
      if (this.file.exists()) {
         this.configuration = new YamlConfiguration();
         this.configuration.load(this.file);
      } else {
         this.configuration = new YamlConfiguration();
         this.configuration.set("version", 0);
      }

   }

   private void validateConfiguration() throws ConfigurationException {
      if (!this.configuration.isInt("version")) {
         throw new ConfigurationException("Expected path `version` value in %s", new Object[]{this.file.getName()});
      } else {
         int version = this.configuration.getInt("version");
         if (version > 0) {
            Set<Integer> versions = (Set)this.migrations.stream().map(Migration::getVersion).collect(Collectors.toSet());
            if (!versions.contains(version)) {
               throw new ConfigurationException("Configuration file has version %d but such a version does not exist", new Object[]{version});
            }
         }

         NodeMap<T> snapshotNodes = new NodeMap();
         YamlConfiguration snapshotConfig = new YamlConfiguration();
         snapshotConfig.set("version", 0);
         Snapshot<T> snapshot = new Snapshot(snapshotNodes, snapshotConfig);
         NodeMap<T> nodesUsedInValidation = null;
         Iterator var6 = this.migrations.iterator();

         while(var6.hasNext()) {
            Migration<T> migration = (Migration)var6.next();
            Patch<T> patch = migration.createPatch(snapshot);
            snapshot.update(patch);
            if (migration.getVersion() <= version) {
               nodesUsedInValidation = new NodeMap(snapshot.getNodes());
            }
         }

         if (nodesUsedInValidation == null) {
            nodesUsedInValidation = new NodeMap(snapshot.getNodes());
         }

         if (version != 0) {
            Set<String> leafs = Util.getLeafs(this.configuration);
            Iterator var15 = nodesUsedInValidation.iterator();

            while(var15.hasNext()) {
               Node<T> node = (Node)var15.next();
               if (!this.configuration.isSet(node.getPath())) {
                  throw new ConfigurationException("Expected path %s to be found in configuration but none was set", new Object[]{node.getPath()});
               }

               this.testType(node);
            }

            var15 = leafs.iterator();

            while(true) {
               String path;
               do {
                  if (!var15.hasNext()) {
                     return;
                  }

                  path = (String)var15.next();
               } while(path.equals("version"));

               boolean hasPath = false;
               Node<T> foundNode = null;
               Iterator var11 = nodesUsedInValidation.iterator();

               while(var11.hasNext()) {
                  Node<T> node = (Node)var11.next();
                  if (node.getPath().equals(path)) {
                     foundNode = node;
                     hasPath = true;
                     break;
                  }
               }

               if (!hasPath) {
                  throw new ConfigurationException("Found unexpected path %s in configuration", new Object[]{path});
               }

               this.testType(foundNode);
            }
         }
      }
   }

   private NodeMap<T> getNodeMapBeforeVersion(int version, YamlConfiguration cfg) {
      cfg.set("version", 0);
      NodeMap<T> nodeMap = new NodeMap();
      Snapshot<T> snapshot = new Snapshot(nodeMap, cfg);
      Iterator var5 = this.migrations.iterator();

      while(var5.hasNext()) {
         Migration<T> migration = (Migration)var5.next();
         if (migration.getVersion() > version) {
            break;
         }

         Patch<T> patch = migration.createPatch(new Snapshot(nodeMap, cfg));
         snapshot.update(patch);
      }

      return nodeMap;
   }

   private void testType(Node<T> node) throws ConfigurationException {
      Object o = this.configuration.get(node.getPath());
      if (!node.getType().isCorrectType(o)) {
         throw new ConfigurationException("Expected path %s in configuration to be of type %s but found %s", new Object[]{node.getPath(), node.getType().name(), o.getClass()});
      }
   }

   protected void cleanup() {
      Set<String> leafs = Util.getLeafs(this.configuration);
      leafs.remove("version");
      Iterator var2 = leafs.iterator();

      while(var2.hasNext()) {
         String path = (String)var2.next();
         this.tryDeletePathRecursively(path);
      }

   }

   private void tryDeletePathRecursively(String path) {
      if (!this.hasPath(path)) {
         this.configuration.set(path, (Object)null);
         int separatorIndex = path.lastIndexOf(46);
         if (separatorIndex != -1) {
            this.tryDeletePathRecursively(path.substring(0, separatorIndex));
         }

      }
   }

   protected boolean hasPath(String path) {
      Iterator var2 = this.snapshot.getNodes().iterator();

      Node value;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         value = (Node)var2.next();
      } while(!value.getPath().equals(path));

      return true;
   }
}
