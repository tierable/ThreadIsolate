package com.tierable.threadisolate;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-11
 */
@Retention(SOURCE)
@Target({METHOD, TYPE})
public @interface DontInvokeOnExecutor {
}
