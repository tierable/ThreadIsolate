package com.tierable.threadisolate;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
@InvokeMethodsOnExecutor
public interface TestInterface {
    int returnsValue();

    void methodWithParameters(String stringParam, int intParam);

    void method();
}
