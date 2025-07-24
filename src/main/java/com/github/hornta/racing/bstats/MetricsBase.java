package com.github.hornta.racing.bstats;

import com.github.hornta.racing.bstats.charts.CustomChart;
import com.github.hornta.racing.bstats.json.JsonObjectBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HttpsURLConnection;

public class MetricsBase {
   public static final String METRICS_VERSION = "2.2.1";
   private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, (task) -> new Thread(task, "bStats-Metrics"));
   private static final String REPORT_URL = "https://bStats.org/api/v2/data/%s";
   private final String platform;
   private final String serverUuid;
   private final int serviceId;
   private final Consumer<JsonObjectBuilder> appendPlatformDataConsumer;
   private final Consumer<JsonObjectBuilder> appendServiceDataConsumer;
   private final Consumer<Runnable> submitTaskConsumer;
   private final Supplier<Boolean> checkServiceEnabledSupplier;
   private final BiConsumer<String, Throwable> errorLogger;
   private final Consumer<String> infoLogger;
   private final boolean logErrors;
   private final boolean logSentData;
   private final boolean logResponseStatusText;
   private final Set<CustomChart> customCharts = new HashSet<>();
   private final boolean enabled;

   public MetricsBase(String platform, String serverUuid, int serviceId, boolean enabled, Consumer<JsonObjectBuilder> appendPlatformDataConsumer, Consumer<JsonObjectBuilder> appendServiceDataConsumer, Consumer<Runnable> submitTaskConsumer, Supplier<Boolean> checkServiceEnabledSupplier, BiConsumer<String, Throwable> errorLogger, Consumer<String> infoLogger, boolean logErrors, boolean logSentData, boolean logResponseStatusText) {
      this.platform = platform;
      this.serverUuid = serverUuid;
      this.serviceId = serviceId;
      this.enabled = enabled;
      this.appendPlatformDataConsumer = appendPlatformDataConsumer;
      this.appendServiceDataConsumer = appendServiceDataConsumer;
      this.submitTaskConsumer = submitTaskConsumer;
      this.checkServiceEnabledSupplier = checkServiceEnabledSupplier;
      this.errorLogger = errorLogger;
      this.infoLogger = infoLogger;
      this.logErrors = logErrors;
      this.logSentData = logSentData;
      this.logResponseStatusText = logResponseStatusText;
      this.checkRelocation();
      if (enabled) {
         this.startSubmitting();
      }

   }

   public void addCustomChart(CustomChart chart) {
      this.customCharts.add(chart);
   }

   private void startSubmitting() {
      Runnable submitTask = () -> {
         if (this.enabled && this.checkServiceEnabledSupplier.get()) {
            if (this.submitTaskConsumer != null) {
               this.submitTaskConsumer.accept(this::submitData);
            } else {
               this.submitData();
            }

         } else {
            scheduler.shutdown();
         }
      };
      long initialDelay = (long)(60000.0D * (3.0D + Math.random() * 3.0D));
      long secondDelay = (long)(60000.0D * Math.random() * 30.0D);
      scheduler.schedule(submitTask, initialDelay, TimeUnit.MILLISECONDS);
      scheduler.scheduleAtFixedRate(submitTask, initialDelay + secondDelay, 1800000L, TimeUnit.MILLISECONDS);
   }

   private void submitData() {
      JsonObjectBuilder baseJsonBuilder = new JsonObjectBuilder();
      this.appendPlatformDataConsumer.accept(baseJsonBuilder);
      JsonObjectBuilder serviceJsonBuilder = new JsonObjectBuilder();
      this.appendServiceDataConsumer.accept(serviceJsonBuilder);
      JsonObjectBuilder.JsonObject[] chartData = this.customCharts.stream().map((customChart) -> customChart.getRequestJsonObject(this.errorLogger, this.logErrors)).filter(Objects::nonNull).toArray(JsonObjectBuilder.JsonObject[]::new);
      serviceJsonBuilder.appendField("id", this.serviceId);
      serviceJsonBuilder.appendField("customCharts", chartData);
      baseJsonBuilder.appendField("service", serviceJsonBuilder.build());
      baseJsonBuilder.appendField("serverUUID", this.serverUuid);
      baseJsonBuilder.appendField("metricsVersion", "2.2.1");
      JsonObjectBuilder.JsonObject data = baseJsonBuilder.build();
      scheduler.execute(() -> {
         try {
            this.sendData(data);
         } catch (Exception exception) {
            if (this.logErrors) {
               this.errorLogger.accept("Could not submit bStats metrics data", exception);
            }
         }

      });
   }

   private void sendData(JsonObjectBuilder.JsonObject data) throws Exception {
      if (this.logSentData) {
         this.infoLogger.accept("Sent bStats metrics data: " + data.toString());
      }

      String url = String.format("https://bStats.org/api/v2/data/%s", this.platform);
      HttpsURLConnection connection = (HttpsURLConnection)(new URL(url)).openConnection();
      byte[] compressedData = compress(data.toString());
      connection.setRequestMethod("POST");
      connection.addRequestProperty("Accept", "application/json");
      connection.addRequestProperty("Connection", "close");
      connection.addRequestProperty("Content-Encoding", "gzip");
      connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("User-Agent", "Metrics-Service/1");
      connection.setDoOutput(true);
      DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

      try {
         outputStream.write(compressedData);
      } catch (Throwable throwable) {
         try {
            outputStream.close();
         } catch (Throwable throwable1) {
            throwable.addSuppressed(throwable1);
         }

         throw throwable;
      }

      outputStream.close();
      StringBuilder builder = new StringBuilder();
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

      String line;
      try {
         while((line = bufferedReader.readLine()) != null) {
            builder.append(line);
         }
      } catch (Throwable throwable) {
         try {
            bufferedReader.close();
         } catch (Throwable throwable1) {
            throwable.addSuppressed(throwable1);
         }

         throw throwable;
      }

      bufferedReader.close();
      if (this.logResponseStatusText) {
         this.infoLogger.accept("Sent data to bStats and received response: " + builder);
      }

   }

   private void checkRelocation() {
      if (System.getProperty("bstats.relocatecheck") == null || !System.getProperty("bstats.relocatecheck").equals("false")) {
         String defaultPackage = new String(new byte[]{111, 114, 103, 46, 98, 115, 116, 97, 116, 115});
         String examplePackage = new String(new byte[]{121, 111, 117, 114, 46, 112, 97, 99, 107, 97, 103, 101});
         if (MetricsBase.class.getPackage().getName().startsWith(defaultPackage) || MetricsBase.class.getPackage().getName().startsWith(examplePackage)) {
            throw new IllegalStateException("bStats Metrics class has not been relocated correctly!");
         }
      }

   }

   private static byte[] compress(String str) throws IOException {
      if (str == null) {
         return null;
      } else {
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         GZIPOutputStream gzip = new GZIPOutputStream(outputStream);

         try {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
         } catch (Throwable throwable) {
            try {
               gzip.close();
            } catch (Throwable throwable1) {
               throwable.addSuppressed(throwable1);
            }

            throw throwable;
         }

         gzip.close();
         return outputStream.toByteArray();
      }
   }
}
