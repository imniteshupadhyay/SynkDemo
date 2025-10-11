package com.playmotech.api.core.utils;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;

public class ColumnUtil {

    public static List<Field> getIncludedFields(Class<?> clazz) {
        return List.of(clazz.getDeclaredFields()).stream()
                .filter(f -> f.isAnnotationPresent(ColumnHeader.class))
                .filter(f -> f.getAnnotation(ColumnHeader.class).include())
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(ColumnHeader.class).order()))
                .toList();
    }

    public static List<String> getColumnHeaders(Class<?> clazz) {
        return getIncludedFields(clazz).stream()
                .map(f -> f.getAnnotation(ColumnHeader.class).value())
                .toList();
    }
}
