package com.tierable.threadisolate;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface InvocationsThreadEnforced {
    boolean useWeakReference() default true;
}
