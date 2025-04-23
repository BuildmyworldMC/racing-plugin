package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.io.IOException;
import java.lang.annotation.Annotation;

public class CreatorProperty extends SettableBeanProperty {
   private static final long serialVersionUID = 1L;
   protected final AnnotatedParameter _annotated;
   protected final Object _injectableValueId;
   protected SettableBeanProperty _fallbackSetter;
   protected final int _creatorIndex;
   protected boolean _ignorable;

   public CreatorProperty(PropertyName name, JavaType type, PropertyName wrapperName, TypeDeserializer typeDeser, Annotations contextAnnotations, AnnotatedParameter param, int index, Object injectableValueId, PropertyMetadata metadata) {
      super(name, type, wrapperName, typeDeser, contextAnnotations, metadata);
      this._annotated = param;
      this._creatorIndex = index;
      this._injectableValueId = injectableValueId;
      this._fallbackSetter = null;
   }

   protected CreatorProperty(CreatorProperty src, PropertyName newName) {
      super(src, newName);
      this._annotated = src._annotated;
      this._injectableValueId = src._injectableValueId;
      this._fallbackSetter = src._fallbackSetter;
      this._creatorIndex = src._creatorIndex;
      this._ignorable = src._ignorable;
   }

   protected CreatorProperty(CreatorProperty src, JsonDeserializer<?> deser, NullValueProvider nva) {
      super(src, deser, nva);
      this._annotated = src._annotated;
      this._injectableValueId = src._injectableValueId;
      this._fallbackSetter = src._fallbackSetter;
      this._creatorIndex = src._creatorIndex;
      this._ignorable = src._ignorable;
   }

   public SettableBeanProperty withName(PropertyName newName) {
      return new CreatorProperty(this, newName);
   }

   public SettableBeanProperty withValueDeserializer(JsonDeserializer<?> deser) {
      if (this._valueDeserializer == deser) {
         return this;
      } else {
         NullValueProvider nvp = this._valueDeserializer == this._nullProvider ? deser : this._nullProvider;
         return new CreatorProperty(this, deser, (NullValueProvider)nvp);
      }
   }

   public SettableBeanProperty withNullProvider(NullValueProvider nva) {
      return new CreatorProperty(this, this._valueDeserializer, nva);
   }

   public void fixAccess(DeserializationConfig config) {
      if (this._fallbackSetter != null) {
         this._fallbackSetter.fixAccess(config);
      }

   }

   public void setFallbackSetter(SettableBeanProperty fallbackSetter) {
      this._fallbackSetter = fallbackSetter;
   }

   public void markAsIgnorable() {
      this._ignorable = true;
   }

   public boolean isIgnorable() {
      return this._ignorable;
   }

   public Object findInjectableValue(DeserializationContext context, Object beanInstance) throws JsonMappingException {
      if (this._injectableValueId == null) {
         context.reportBadDefinition(ClassUtil.classOf(beanInstance), String.format("Property '%s' (type %s) has no injectable value id configured", this.getName(), this.getClass().getName()));
      }

      return context.findInjectableValue(this._injectableValueId, this, beanInstance);
   }

   public void inject(DeserializationContext context, Object beanInstance) throws IOException {
      this.set(beanInstance, this.findInjectableValue(context, beanInstance));
   }

   public <A extends Annotation> A getAnnotation(Class<A> acls) {
      return this._annotated == null ? null : this._annotated.getAnnotation(acls);
   }

   public AnnotatedMember getMember() {
      return this._annotated;
   }

   public int getCreatorIndex() {
      return this._creatorIndex;
   }

   public void deserializeAndSet(JsonParser p, DeserializationContext ctxt, Object instance) throws IOException {
      this._verifySetter();
      this._fallbackSetter.set(instance, this.deserialize(p, ctxt));
   }

   public Object deserializeSetAndReturn(JsonParser p, DeserializationContext ctxt, Object instance) throws IOException {
      this._verifySetter();
      return this._fallbackSetter.setAndReturn(instance, this.deserialize(p, ctxt));
   }

   public void set(Object instance, Object value) throws IOException {
      this._verifySetter();
      this._fallbackSetter.set(instance, value);
   }

   public Object setAndReturn(Object instance, Object value) throws IOException {
      this._verifySetter();
      return this._fallbackSetter.setAndReturn(instance, value);
   }

   public PropertyMetadata getMetadata() {
      PropertyMetadata md = super.getMetadata();
      return this._fallbackSetter != null ? md.withMergeInfo(this._fallbackSetter.getMetadata().getMergeInfo()) : md;
   }

   public Object getInjectableValueId() {
      return this._injectableValueId;
   }

   public String toString() {
      return "[creator property, name '" + this.getName() + "'; inject id '" + this._injectableValueId + "']";
   }

   private final void _verifySetter() throws IOException {
      if (this._fallbackSetter == null) {
         this._reportMissingSetter((JsonParser)null, (DeserializationContext)null);
      }

   }

   private void _reportMissingSetter(JsonParser p, DeserializationContext ctxt) throws IOException {
      String msg = "No fallback setter/field defined for creator property '" + this.getName() + "'";
      if (ctxt != null) {
         ctxt.reportBadDefinition(this.getType(), msg);
      } else {
         throw InvalidDefinitionException.from(p, msg, this.getType());
      }
   }
}
