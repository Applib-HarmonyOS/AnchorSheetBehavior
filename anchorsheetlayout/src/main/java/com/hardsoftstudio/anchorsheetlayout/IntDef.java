package com.hardsoftstudio.anchorsheetlayout;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Helper annotation.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.ANNOTATION_TYPE})
public @interface IntDef {
    /**
     * Helps in converting Enum into int values.
     *
     * @return int value
     */
    int[] value() default {};
}
