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
    private final File file;
   private YamlConfiguration configuration;
   private final List<Migration<T>> migrations;
   private Snapshot<T> snapshot;
   protected static final String VERSION_FIELD = "version";

   public Configuration(File file, List<Migration<T>> migrations) {
      this.file = file;
       NodeMap<T> nodes2 = new NodeMap<>();
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
         Snapshot<T> defaultSnapshot = new Snapshot<>(defaultSnapshotNodes, defaultSnapshotCfg);
         defaultSnapshot.applyConfiguration(this.configuration);

          for (Migration<T> migration : this.migrations) {
              if (migration.getVersion() > currentCfgVersion) {
                  Patch<T> patch = migration.createPatch(defaultSnapshot);
                  defaultSnapshot.update(patch);
                  defaultSnapshot.getConfiguration().set("version", migration.getVersion());
              }
          }

         if (!this.migrations.isEmpty()) {
            defaultSnapshot.getConfiguration().set("version", this.migrations.getLast().getVersion());
         }

         this.snapshot = defaultSnapshot;
         this.configuration = this.snapshot.getConfiguration();
         this.cleanup();
         this.persistToFile();
      } catch (InvalidConfigurationException | IOException exception) {
         throw new ConfigurationException(exception);
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
            Set<Integer> versions = this.migrations.stream().map(Migration::getVersion).collect(Collectors.toSet());
            if (!versions.contains(version)) {
               throw new ConfigurationException("Configuration file has version %d but such a version does not exist", version);
            }
         }

         NodeMap<T> snapshotNodes = new NodeMap<>();
         YamlConfiguration snapshotConfig = new YamlConfiguration();
         snapshotConfig.set("version", 0);
         Snapshot<T> snapshot = new Snapshot<>(snapshotNodes, snapshotConfig);
         NodeMap<T> nodesUsedInValidation = null;

          for (Migration<T> migration : this.migrations) {
              Patch<T> patch = migration.createPatch(snapshot);
              snapshot.update(patch);
              if (migration.getVersion() <= version) {
                  nodesUsedInValidation = new NodeMap<>(snapshot.getNodes());
              }
          }

         if (nodesUsedInValidation == null) {
            nodesUsedInValidation = new NodeMap<>(snapshot.getNodes());
         }

         if (version != 0) {
            Set<String> leafs = Util.getLeafs(this.configuration);
            Iterator nodeIterator = nodesUsedInValidation.iterator();
            while(nodeIterator.hasNext()) {
               Node node = (Node) nodeIterator.next();
               if (!this.configuration.isSet(node.getPath())) {
                  throw new ConfigurationException("Expected path %s to be found in configuration but none was set", node.getPath());
               }

               this.testType(node);
            }

            nodeIterator = leafs.iterator();

            while(true) {
               String path;
               do {
                  if (!nodeIterator.hasNext()) {
                     return;
                  }

                  path = (String)nodeIterator.next();
               } while(path.equals("version"));

               boolean hasPath = false;
               Node<T> foundNode = null;

                for (Node<T> node : nodesUsedInValidation) {
                    if (node.getPath().equals(path)) {
                        foundNode = node;
                        hasPath = true;
                        break;
                    }
                }

               if (!hasPath) {
                  throw new ConfigurationException("Found unexpected path %s in configuration", path);
               }

               this.testType(foundNode);
            }
         }
      }
   }

   private NodeMap<T> getNodeMapBeforeVersion(int version, YamlConfiguration cfg) {
      cfg.set("version", 0);
      NodeMap<T> nodeMap = new NodeMap<>();
      Snapshot<T> snapshot = new Snapshot<>(nodeMap, cfg);

       for (Migration<T> migration : this.migrations) {
           if (migration.getVersion() > version) {
               break;
           }

           Patch<T> patch = migration.createPatch(new Snapshot<>(nodeMap, cfg));
           snapshot.update(patch);
       }

      return nodeMap;
   }

   private void testType(Node<T> node) throws ConfigurationException {
      Object o = this.configuration.get(node.getPath());
      if (!node.getType().isCorrectType(o)) {
          assert o != null;
          throw new ConfigurationException("Expected path %s in configuration to be of type %s but found %s", node.getPath(), node.getType().name(), o.getClass());
      }
   }

   protected void cleanup() {
      Set<String> leafs = Util.getLeafs(this.configuration);
      leafs.remove("version");

       for (String path : leafs) {
           this.tryDeletePathRecursively(path);
       }

   }

   private void tryDeletePathRecursively(String path) {
      if (!this.hasPath(path)) {
         this.configuration.set(path, null);
         int separatorIndex = path.lastIndexOf(46);
         if (separatorIndex != -1) {
            this.tryDeletePathRecursively(path.substring(0, separatorIndex));
         }

      }
   }

   protected boolean hasPath(String path) {
      Iterator<Node<T>> nodeIterator = this.snapshot.getNodes().iterator();

      Node<T> value;
      do {
         if (!nodeIterator.hasNext()) {
            return false;
         }

         value = nodeIterator.next();
      } while(!value.getPath().equals(path));

      return true;
   }
}
