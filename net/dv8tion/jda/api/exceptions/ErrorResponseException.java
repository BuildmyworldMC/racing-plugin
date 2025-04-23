package net.dv8tion.jda.api.exceptions;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.utils.Checks;

public class ErrorResponseException extends RuntimeException {
   private final ErrorResponse errorResponse;
   private final Response response;
   private final String meaning;
   private final int code;

   private ErrorResponseException(ErrorResponse errorResponse, Response response, int code, String meaning) {
      super(code + ": " + meaning);
      this.response = response;
      if (response != null && response.getException() != null) {
         this.initCause(response.getException());
      }

      this.errorResponse = errorResponse;
      this.code = code;
      this.meaning = meaning;
   }

   public boolean isServerError() {
      return this.errorResponse == ErrorResponse.SERVER_ERROR;
   }

   public String getMeaning() {
      return this.meaning;
   }

   public int getErrorCode() {
      return this.code;
   }

   public ErrorResponse getErrorResponse() {
      return this.errorResponse;
   }

   public Response getResponse() {
      return this.response;
   }

   public static ErrorResponseException create(ErrorResponse errorResponse, Response response) {
      Optional<DataObject> optObj = response.optObject();
      String meaning = errorResponse.getMeaning();
      int code = errorResponse.getCode();
      if (response.isError() && response.getException() != null) {
         code = response.code;
         meaning = response.getException().getClass().getName();
      } else if (optObj.isPresent()) {
         DataObject obj = (DataObject)optObj.get();
         if (obj.isNull("code") && obj.isNull("message")) {
            code = response.code;
            meaning = obj.toString();
         } else {
            if (!obj.isNull("code")) {
               code = obj.getInt("code");
            }

            if (!obj.isNull("message")) {
               meaning = obj.getString("message");
            }
         }
      } else {
         code = response.code;
         meaning = response.getString();
      }

      return new ErrorResponseException(errorResponse, response, code, meaning);
   }

   @Nonnull
   public static Consumer<Throwable> ignore(@Nonnull Collection<ErrorResponse> set) {
      return ignore(RestAction.getDefaultFailure(), set);
   }

   @Nonnull
   public static Consumer<Throwable> ignore(@Nonnull ErrorResponse ignored, @Nonnull ErrorResponse... errorResponses) {
      return ignore(RestAction.getDefaultFailure(), ignored, errorResponses);
   }

   @Nonnull
   public static Consumer<Throwable> ignore(@Nonnull Consumer<? super Throwable> orElse, @Nonnull ErrorResponse ignored, @Nonnull ErrorResponse... errorResponses) {
      return ignore((Consumer)orElse, (Collection)EnumSet.of(ignored, errorResponses));
   }

   @Nonnull
   public static Consumer<Throwable> ignore(@Nonnull Consumer<? super Throwable> orElse, @Nonnull Collection<ErrorResponse> set) {
      Checks.notNull(orElse, "Callback");
      Checks.notEmpty(set, "Ignored collection");
      EnumSet<ErrorResponse> ignored = EnumSet.copyOf(set);
      return (new ErrorHandler(orElse)).ignore((Collection)ignored);
   }
}
