package com.tierable.threadisolate;


import java.util.concurrent.Executor;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
public interface ThreadEnforcedImplementation<RealImplementationT> {
    Executor getExecutor();


    RealImplementationT getRealImplementation();

    void setRealImplementation(RealImplementationT realImplementation);
}