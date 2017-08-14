package com.tierable.threadisolate;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
@InvokeMethodsOnExecutor(useWeakReference = false)
public interface TestInterfaceStrongReference {
    int returnsValue();

    void methodWithParameters(String stringParam, int intParam);

    void method();

    @DontInvokeOnExecutor
    void methodNotOnExecutor();
}
