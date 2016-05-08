package com.llug.api;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Assert;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write JSON using <a href="http://jackson.codehaus.org/">Jackson's</a> {@link ObjectMapper}.
 *
 * <p>This converter can be used to bind to typed beans, or untyped {@link java.util.HashMap HashMap} instances.
 *
 * <p>By default, this converter supports {@code application/json}. This can be overridden by setting the
 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} property.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * org.springframework.web.servlet.view.json.MappingJacksonJsonView
 */

//http://stackoverflow.com/questions/3754055/spring-mvc-mappping-view-for-google-gson
//http://stackoverflow.com/questions/5019162/custom-httpmessageconverter-with-responsebody-to-do-json-things

public abstract class ApiHttpMessageConverterBase extends AbstractHttpMessageConverter<Object> {
    private static final Boolean USE_JACKSON = true;

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private ObjectMapper objectMapper = new ObjectMapper();
    private GsonBuilder gsonBuilder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setDateFormat(DATE_FORMAT);

    private boolean prefixJson = false;

    public ApiHttpMessageConverterBase(MediaType mediaType) {
        super(mediaType);
    }

    /**
     * Set the {@code ObjectMapper} for this view. If not set, a default
     * {@link ObjectMapper#ObjectMapper() ObjectMapper} is used.
     * <p>Setting a custom-configured {@code ObjectMapper} is one way to take further control of the JSON
     * serialization process. For example, an extended {@link org.codehaus.jackson.map.SerializerFactory}
     * can be configured that provides custom serializers for specific types. The other option for refining
     * the serialization process is to use Jackson's provided annotations on the types to be serialized,
     * in which case a custom-configured ObjectMapper is unnecessary.
     */
    public void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        this.objectMapper = objectMapper;
    }

    /**
     * Return the underlying {@code ObjectMapper} for this view.
     */
    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    /**
     * Indicate whether the JSON output by this view should be prefixed with "{} &amp;&amp;". Default is false.
     * <p>Prefixing the JSON string in this manner is used to help prevent JSON Hijacking.
     * The prefix renders the string syntactically invalid as a script so that it cannot be hijacked.
     * This prefix does not affect the evaluation of JSON, but if JSON validation is performed on the
     * string, the prefix would need to be ignored.
     */
    public void setPrefixJson(boolean prefixJson) {
        this.prefixJson = prefixJson;
    }

    public void registerTypeAdapter(Type type, Object serializer) {
        gsonBuilder.registerTypeAdapter(type, serializer);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        JavaType javaType = getJavaType(clazz);
        return (this.objectMapper.canDeserialize(javaType) && canRead(mediaType));
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return (this.objectMapper.canSerialize(clazz) && canWrite(mediaType));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // should not be called, since we override canRead/Write instead
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        if (USE_JACKSON) {
            JavaType javaType = getJavaType(clazz);
            try {
                return this.objectMapper.readValue(inputMessage.getBody(), javaType);
            } catch (JsonProcessingException ex) {
                throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex);
            }
        } else {
            try {
                Gson gson = gsonBuilder.create();
                StringWriter writer = new StringWriter();
                IOUtils.copy(inputMessage.getBody(), writer, "UTF-8");

                return gson.fromJson(writer.toString(), clazz);
            } catch (JsonParseException e) {
                throw new HttpMessageNotReadableException("Could not read JSON: " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected void writeInternal(Object object, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        boolean specifyLowerCaseWithUnderscores = true;

        if (USE_JACKSON) {
            JsonEncoding encoding = getJsonEncoding(outputMessage.getHeaders().getContentType());
            JsonGenerator jsonGenerator = this.objectMapper.getJsonFactory().createJsonGenerator(outputMessage.getBody(), encoding);
            try {
                if (this.prefixJson) {
                    jsonGenerator.writeRaw("{} && ");
                }

                if (specifyLowerCaseWithUnderscores) {
                    // http://stackoverflow.com/questions/9533227/specifying-the-field-naming-policy-for-jackson
                    ObjectMapper mapr = new ObjectMapper();

                    mapr.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
                    mapr.writeValue(jsonGenerator, object);
                } else {
                    this.objectMapper.writeValue(jsonGenerator, object);
                }
            } catch (JsonProcessingException ex) {
                throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
            }
        } else {
            Type genericType = TypeToken.get(object.getClass()).getType();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputMessage.getBody(), DEFAULT_CHARSET));
            try {
                // See http://code.google.com/p/google-gson/issues/detail?id=199 for details on SQLTimestamp
                // conversion
                Gson gson = null;

                if (specifyLowerCaseWithUnderscores) {
                    gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                            .setDateFormat(DATE_FORMAT)
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create();
                } else {
                    gson = gsonBuilder.create();
                }

                writer.append(gson.toJson(object, genericType));
            } finally {
                writer.flush();
                writer.close();
            }
        }
    }

    /**
     * Return the Jackson {@link JavaType} for the specified class.
     * <p>The default implementation returns {@link TypeFactory#type(java.lang.reflect.Type)},
     * but this can be overridden in subclasses, to allow for custom generic collection handling.
     * For instance:
     * <pre class="code">
     * protected JavaType getJavaType(Class&lt;?&gt; clazz) {
     *   if (List.class.isAssignableFrom(clazz)) {
     *     return TypeFactory.collectionType(ArrayList.class, MyBean.class);
     *   } else {
     *     return super.getJavaType(clazz);
     *   }
     * }
     * </pre>
     * @param clazz the class to return the java type for
     * @return the java type
     */
    protected JavaType getJavaType(Class<?> clazz) {
        return TypeFactory.type(clazz);
    }

    /**
     * Determine the JSON encoding to use for the given content type.
     * @param contentType the media type as requested by the caller
     * @return the JSON encoding to use (never <code>null</code>)
     */
    protected JsonEncoding getJsonEncoding(MediaType contentType) {
        if (contentType != null && contentType.getCharSet() != null) {
            Charset charset = contentType.getCharSet();
            for (JsonEncoding encoding : JsonEncoding.values()) {
                if (charset.name().equals(encoding.getJavaName())) {
                    return encoding;
                }
            }
        }
        return JsonEncoding.UTF8;
    }
}