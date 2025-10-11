package com.playmotech.api.core.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnHeader {
    String value();        // Display name
    int order();           // Column order
    boolean include() default true;  // Skip column if false
    String format() default "";  // Format pattern for date/time values (e.g., "dd-MM-yyyy", "HH:mm:ss")
}
