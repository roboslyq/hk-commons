package com.hk.commons.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.hk.commons.jackson.DisableJsonIgnorePropertiesAnnotationIntrospector;
import com.hk.commons.util.date.DatePattern;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * JSON Utils
 *
 * @author kevin
 * @date 2018-07-17 14:48
 */
public final class JsonUtils {

    public static final String IGNORE_ENTITY_SERIALIZE_FIELD_FILTER_ID = "fieldFilter";

    private static ObjectMapper mapper;

    private static final JavaTimeModule JAVA_TIME_MODULE;

    static {
        JAVA_TIME_MODULE = new JavaTimeModule();
        JAVA_TIME_MODULE.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.YYYY_MM_DD_HH_MM_SS.getPattern())));
        JAVA_TIME_MODULE.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DatePattern.YYYY_MM_DD_HH_MM_SS.getPattern())));

        JAVA_TIME_MODULE.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePattern.YYYY_MM_DD.getPattern())));
        JAVA_TIME_MODULE.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePattern.YYYY_MM_DD.getPattern())));

        JAVA_TIME_MODULE.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.HH_MM_SS.getPattern())));
        JAVA_TIME_MODULE.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePattern.HH_MM_SS.getPattern())));

    }

    public static JavaTimeModule getJavaTimeModule() {
        return JAVA_TIME_MODULE;
    }

    private static ObjectMapper getMapper() {
        if (mapper == null) {
            synchronized (JsonUtils.class) {
                if (mapper == null) {
                    mapper = new ObjectMapper();
                    configure(mapper);
                }
            }
        }
        return mapper;
    }

    public static void configure(ObjectMapper om) {
//        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//        om.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        om.setDateFormat(new SimpleDateFormat(DatePattern.YYYY_MM_DD_HH_MM_SS.getPattern()));
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 空值处理为空串
//        om.getSerializerProvider().setNullValueSerializer(NullEmptyJsonSerializer.INSTANCE);
        // 设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        om.configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false);
        om.registerModule(JAVA_TIME_MODULE);
//        om.configure(MapperFeature.USE_ANNOTATIONS, false);//忽略注解
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        /*
         * 忽略实体中的Hibernate getOne查询返回的  "handler", "hibernateLazyInitializer" 字段
         *
         */
        filterProvider.addFilter(IGNORE_ENTITY_SERIALIZE_FIELD_FILTER_ID, SimpleBeanPropertyFilter.serializeAllExcept("handler", "hibernateLazyInitializer"));
        om.setFilterProvider(filterProvider);

        om.setAnnotationIntrospector(new DisableJsonIgnorePropertiesAnnotationIntrospector());
    }

    private static ObjectMapper indentMapper;

    private static ObjectMapper getIndentMapper() {
        if (indentMapper == null) {
            synchronized (JsonUtils.class) {
                if (indentMapper == null) {
                    indentMapper = new ObjectMapper();
                    indentMapper.enable(SerializationFeature.INDENT_OUTPUT);
                    configure(indentMapper);
                }
            }
        }
        return indentMapper;
    }

    /**
     * 将对象序列化为JSON string.
     *
     * @param obj obj
     * @return json str
     */
    public static String serialize(Object obj) {
        return serialize(obj, false);
    }

    /**
     * 将对象序列化为 JSON string
     *
     * @param obj    obj
     * @param indent indent
     * @return json str
     */
    public static String serialize(Object obj, boolean indent) {
        if (obj == null) {
            return null;
        }
        ObjectMapper mapper = indent ? getIndentMapper() : getMapper();
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将json 字符串反序列化为对象
     *
     * @param json  json str
     * @param clazz Class
     * @param <T>   T
     * @return T
     */
    public static <T> T deserialize(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        ObjectMapper mapper = getMapper();
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将json 字符串反序列化为对象集合
     *
     * @param <T>   T
     * @param json  json str
     * @param clazz class
     * @return 序列化的List
     */
    public static <T> List<T> deserializeList(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        ObjectMapper mapper = getMapper();
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructParametricType(List.class, clazz));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 反序列化json 字符串到对象
     * 二级泛型: 如：JsonResult<List<SysUser>>
     *
     * @return 序列化的对象
     */
    public static <T> T deserialize(String json, Class<T> rawType, Class<?> parametrized, Class<?> parameterClasses) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        ObjectMapper mapper = getMapper();
        try {
            JavaType type = mapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
            return mapper.readValue(json, mapper.getTypeFactory().constructParametricType(rawType, type));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
