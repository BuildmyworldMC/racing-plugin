package net.dv8tion.jda.internal.requests;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import javax.net.ssl.SSLPeerUnverifiedException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.ratelimit.BotRateLimiter;
import net.dv8tion.jda.internal.utils.JDALogger;
import net.dv8tion.jda.internal.utils.config.AuthorizationConfig;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.internal.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class Requester {
   public static final Logger LOG = JDALogger.getLog(Requester.class);
   public static final String DISCORD_API_PREFIX = String.format("https://discord.com/api/v%d/", 6);
   public static final String USER_AGENT;
   public static final RequestBody EMPTY_BODY;
   public static final MediaType MEDIA_TYPE_JSON;
   public static final MediaType MEDIA_TYPE_OCTET;
   protected final JDAImpl api;
   protected final AuthorizationConfig authConfig;
   private final RateLimiter rateLimiter;
   private final OkHttpClient httpClient;
   private boolean isContextReady;
   private ConcurrentMap<String, String> contextMap;
   private volatile boolean retryOnTimeout;

   public Requester(JDA api) {
      this(api, ((JDAImpl)api).getAuthorizationConfig());
   }

   public Requester(JDA api, AuthorizationConfig authConfig) {
      this.isContextReady = false;
      this.contextMap = null;
      this.retryOnTimeout = false;
      if (authConfig == null) {
         throw new NullPointerException("Provided config was null!");
      } else {
         this.authConfig = authConfig;
         this.api = (JDAImpl)api;
         this.rateLimiter = new BotRateLimiter(this);
         this.httpClient = this.api.getHttpClient();
      }
   }

   public void setContextReady(boolean ready) {
      this.isContextReady = ready;
   }

   public void setContext() {
      if (this.isContextReady) {
         if (this.contextMap == null) {
            this.contextMap = this.api.getContextMap();
         }

         this.contextMap.forEach(MDC::put);
      }
   }

   public JDAImpl getJDA() {
      return this.api;
   }

   public <T> void request(Request<T> apiRequest) {
      if (this.rateLimiter.isStopped) {
         throw new RejectedExecutionException("The Requester has been stopped! No new requests can be requested!");
      } else {
         if (apiRequest.shouldQueue()) {
            this.rateLimiter.queueRequest(apiRequest);
         } else {
            this.execute(apiRequest, true);
         }

      }
   }

   private static boolean isRetry(Throwable e) {
      return e instanceof SocketException || e instanceof SocketTimeoutException || e instanceof SSLPeerUnverifiedException;
   }

   public Long execute(Request<?> apiRequest) {
      return this.execute(apiRequest, false);
   }

   public Long execute(Request<?> apiRequest, boolean handleOnRateLimit) {
      return this.execute(apiRequest, false, handleOnRateLimit);
   }

   public Long execute(Request<?> apiRequest, boolean retried, boolean handleOnRatelimit) {
      Route.CompiledRoute route = apiRequest.getRoute();
      Long retryAfter = this.rateLimiter.getRateLimit(route);
      if (retryAfter != null && retryAfter > 0L) {
         if (handleOnRatelimit) {
            apiRequest.handleResponse(new Response(retryAfter, Collections.emptySet()));
         }

         return retryAfter;
      } else {
         okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
         String url = DISCORD_API_PREFIX + route.getCompiledRoute();
         builder.url(url);
         String method = apiRequest.getRoute().getMethod().toString();
         RequestBody body = apiRequest.getBody();
         if (body == null && HttpMethod.requiresRequestBody(method)) {
            body = EMPTY_BODY;
         }

         builder.method(method, body).header("X-RateLimit-Precision", "millisecond").header("user-agent", USER_AGENT).header("accept-encoding", "gzip");
         if (url.startsWith(DISCORD_API_PREFIX)) {
            builder.header("authorization", this.api.getToken());
         }

         if (apiRequest.getHeaders() != null) {
            Iterator var10 = apiRequest.getHeaders().entrySet().iterator();

            while(var10.hasNext()) {
               Entry<String, String> header = (Entry)var10.next();
               builder.addHeader((String)header.getKey(), (String)header.getValue());
            }
         }

         okhttp3.Request request = builder.build();
         Set<String> rays = new LinkedHashSet();
         okhttp3.Response[] responses = new okhttp3.Response[4];
         okhttp3.Response lastResponse = null;
         boolean var31 = false;

         Long var15;
         int var17;
         int var18;
         okhttp3.Response r;
         okhttp3.Response[] var40;
         label482: {
            String cfRay;
            label483: {
               label484: {
                  label485: {
                     label486: {
                        label487: {
                           try {
                              label502: {
                                 try {
                                    var31 = true;
                                    LOG.trace((String)"Executing request {} {}", (Object)apiRequest.getRoute().getMethod(), (Object)url);
                                    int attempt = 0;

                                    do {
                                       if (apiRequest.isSkipped()) {
                                          var15 = null;
                                          var31 = false;
                                          break label487;
                                       }

                                       Call call = this.httpClient.newCall(request);
                                       lastResponse = call.execute();
                                       responses[attempt] = lastResponse;
                                       cfRay = lastResponse.header("CF-RAY");
                                       if (cfRay != null) {
                                          rays.add(cfRay);
                                       }

                                       if (lastResponse.code() < 500) {
                                          break;
                                       }

                                       ++attempt;
                                       LOG.debug("Requesting {} -> {} returned status {}... retrying (attempt {})", apiRequest.getRoute().getMethod(), url, lastResponse.code(), attempt);

                                       try {
                                          Thread.sleep((long)(50 * attempt));
                                       } catch (InterruptedException var32) {
                                       }
                                    } while(attempt < 3 && lastResponse.code() >= 500);

                                    LOG.trace("Finished Request {} {} with code {}", route.getMethod(), lastResponse.request().url(), lastResponse.code());
                                    if (lastResponse.code() >= 500) {
                                       Response response = new Response(lastResponse, -1L, rays);
                                       apiRequest.handleResponse(response);
                                       cfRay = null;
                                       var31 = false;
                                       break label483;
                                    }

                                    retryAfter = this.rateLimiter.handleResponse(route, lastResponse);
                                    if (!rays.isEmpty()) {
                                       LOG.debug((String)"Received response with following cf-rays: {}", (Object)rays);
                                    }

                                    if (retryAfter == null) {
                                       apiRequest.handleResponse(new Response(lastResponse, -1L, rays));
                                    } else if (handleOnRatelimit) {
                                       apiRequest.handleResponse(new Response(lastResponse, retryAfter, rays));
                                    }

                                    var15 = retryAfter;
                                    var31 = false;
                                    break label485;
                                 } catch (UnknownHostException var33) {
                                    LOG.error((String)"DNS resolution failed: {}", (Object)var33.getMessage());
                                    apiRequest.handleResponse(new Response(lastResponse, var33, rays));
                                    var15 = null;
                                    var31 = false;
                                    break label486;
                                 } catch (IOException var34) {
                                    if (this.retryOnTimeout && !retried && isRetry(var34)) {
                                       var15 = this.execute(apiRequest, true, handleOnRatelimit);
                                       var31 = false;
                                       break label502;
                                    }
                                 } catch (Exception var35) {
                                    LOG.error((String)"There was an unexpected error while executing a REST request", (Throwable)var35);
                                    apiRequest.handleResponse(new Response(lastResponse, var35, rays));
                                    var15 = null;
                                    var31 = false;
                                    break label484;
                                 }

                                 LOG.error((String)"There was an I/O error while executing a REST request: {}", (Object)var34.getMessage());
                                 apiRequest.handleResponse(new Response(lastResponse, var34, rays));
                                 var15 = null;
                                 var31 = false;
                                 break label482;
                              }
                           } finally {
                              if (var31) {
                                 okhttp3.Response[] var22 = responses;
                                 int var23 = responses.length;

                                 for(int var24 = 0; var24 < var23; ++var24) {
                                    okhttp3.Response r = var22[var24];
                                    if (r == null) {
                                       break;
                                    }

                                    r.close();
                                 }

                              }
                           }

                           var40 = responses;
                           var17 = responses.length;

                           for(var18 = 0; var18 < var17; ++var18) {
                              r = var40[var18];
                              if (r == null) {
                                 break;
                              }

                              r.close();
                           }

                           return var15;
                        }

                        var40 = responses;
                        var17 = responses.length;

                        for(var18 = 0; var18 < var17; ++var18) {
                           r = var40[var18];
                           if (r == null) {
                              break;
                           }

                           r.close();
                        }

                        return var15;
                     }

                     var40 = responses;
                     var17 = responses.length;

                     for(var18 = 0; var18 < var17; ++var18) {
                        r = var40[var18];
                        if (r == null) {
                           break;
                        }

                        r.close();
                     }

                     return var15;
                  }

                  var40 = responses;
                  var17 = responses.length;

                  for(var18 = 0; var18 < var17; ++var18) {
                     r = var40[var18];
                     if (r == null) {
                        break;
                     }

                     r.close();
                  }

                  return var15;
               }

               var40 = responses;
               var17 = responses.length;

               for(var18 = 0; var18 < var17; ++var18) {
                  r = var40[var18];
                  if (r == null) {
                     break;
                  }

                  r.close();
               }

               return var15;
            }

            okhttp3.Response[] var42 = responses;
            var18 = responses.length;

            for(int var43 = 0; var43 < var18; ++var43) {
               okhttp3.Response r = var42[var43];
               if (r == null) {
                  break;
               }

               r.close();
            }

            return cfRay;
         }

         var40 = responses;
         var17 = responses.length;

         for(var18 = 0; var18 < var17; ++var18) {
            r = var40[var18];
            if (r == null) {
               break;
            }

            r.close();
         }

         return var15;
      }
   }

   private void applyBody(Request<?> apiRequest, okhttp3.Request.Builder builder) {
      String method = apiRequest.getRoute().getMethod().toString();
      RequestBody body = apiRequest.getBody();
      if (body == null && HttpMethod.requiresRequestBody(method)) {
         body = EMPTY_BODY;
      }

      builder.method(method, body);
   }

   private void applyHeaders(Request<?> apiRequest, okhttp3.Request.Builder builder, boolean authorized) {
      builder.header("user-agent", USER_AGENT).header("accept-encoding", "gzip").header("x-ratelimit-precision", "millisecond");
      if (authorized) {
         builder.header("authorization", this.authConfig.getToken());
      }

      if (apiRequest.getHeaders() != null) {
         Iterator var4 = apiRequest.getHeaders().entrySet().iterator();

         while(var4.hasNext()) {
            Entry<String, String> header = (Entry)var4.next();
            builder.addHeader((String)header.getKey(), (String)header.getValue());
         }
      }

   }

   public OkHttpClient getHttpClient() {
      return this.httpClient;
   }

   public RateLimiter getRateLimiter() {
      return this.rateLimiter;
   }

   public void setRetryOnTimeout(boolean retryOnTimeout) {
      this.retryOnTimeout = retryOnTimeout;
   }

   public boolean stop() {
      return this.rateLimiter.stop();
   }

   public void shutdown() {
      this.rateLimiter.shutdown();
   }

   static {
      USER_AGENT = "DiscordBot (https://github.com/DV8FromTheWorld/JDA, " + JDAInfo.VERSION + ")";
      EMPTY_BODY = RequestBody.create((MediaType)null, (byte[])(new byte[0]));
      MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
      MEDIA_TYPE_OCTET = MediaType.parse("application/octet-stream; charset=utf-8");
   }
}
