package com.hk.commons.util;

import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 字段工具类
 *
 * @author: kevin
 * @date: 2017年8月31日上午11:12:57
 */
public abstract class FieldUtils {

    /**
     * 获取有指定注解标识的字段，包括父类
     *
     * @param cls
     * @param annotationCls
     * @return
     */
    public static List<Field> getFieldsListWithAnnotation(final Class<?> cls,
                                                          final Class<? extends Annotation> annotationCls) {
        final List<Field> allFields = getAllFieldsList(cls);
        final List<Field> annotatedFields = new ArrayList<>();
        for (final Field field : allFields) {
            if (null != field.getAnnotation(annotationCls)) {
                annotatedFields.add(field);
            }
        }
        return annotatedFields;
    }

    /**
     * 获取有指定注解标识的字段，包括父类
     *
     * @param cls
     * @param annotationCls
     * @return
     */
    public static Field[] getFieldsWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        List<Field> annotatedFieldsList = getFieldsListWithAnnotation(cls, annotationCls);
        return annotatedFieldsList.toArray(new Field[annotatedFieldsList.size()]);
    }

    /**
     * 获取包括父类的所有字段
     *
     * @param cls
     * @return
     */
    public static Field[] getAllFields(final Class<?> cls) {
        List<Field> allFieldsList = getAllFieldsList(cls);
        return allFieldsList.toArray(new Field[allFieldsList.size()]);
    }

    /**
     * 获取属性的字段,包括父类
     *
     * @param cls
     * @param propertyName
     * @return
     */
    public static Field findField(final Class<?> cls, String propertyName) {
        List<Field> fieldsList = getAllFieldsList(cls);
        return fieldsList.stream()
                .filter(item -> StringUtils.equals(item.getName(), propertyName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(cls.getName() + " no such propertyName :" + propertyName));
    }

    /**
     * 获取包括父类的所有字段
     *
     * @param cls
     * @return
     * @see ReflectionUtils#findField
     */
    public static List<Field> getAllFieldsList(final Class<?> cls) {
        AssertUtils.notNull(cls, "The class must not be null");
        final List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = cls;
        while (null != currentClass) {
            CollectionUtils.addAllNotNull(allFields, currentClass.getDeclaredFields());
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }

}
