package com.tierable.threadisolate;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.concurrent.Executor;

import javax.lang.model.element.Modifier;

import static com.tierable.threadisolate.ThreadIsolateProcessor.CLASS_NAME_EXECUTOR_INVOKING_IMPLEMENTATION;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
public abstract class ExecutorInvokingImplementationClassBuilder {
    public static final ClassName CLASS_NAME_EXECUTOR = ClassName.get(
            Executor.class
    );


    protected static final String FIELD_NAME_EXECUTOR            = "executor";
    protected static final String FIELD_NAME_REAL_IMPLEMENTATION = "realImplementation";

    public static final    String METHOD_NAME_GET_EXECUTOR         = "getExecutor";
    protected static final String PARAM_NAME_GET_EXECUTOR_EXECUTOR = "executor";

    public static final String METHOD_NAME_GET_REAL_IMPLEMENTATION = "getRealImplementation";

    protected static final String METHOD_NAME_SET_REAL_IMPLEMENTATION                    = "setRealImplementation";
    protected static final String PARAM_NAME_SET_REAL_IMPLEMENTATION_REAL_IMPLEMENTATION = "realImplementation";

    protected final TypeSpec.Builder typeBuilder;
    protected final ClassName        referenceClassClassName;


    public static ExecutorInvokingImplementationClassBuilder get(TypeSpec.Builder typeBuilder,
                                                                 ClassName referenceClassClassName,
                                                                 boolean useWeakReference) {
        if (useWeakReference) {
            return new ExecutorInvokingImplementationClassBuilderWeakReference(
                    typeBuilder, referenceClassClassName
            );
        } else {
            return new ExecutorInvokingImplementationClassBuilderStrongReference(
                    typeBuilder, referenceClassClassName
            );
        }
    }


    protected ExecutorInvokingImplementationClassBuilder(TypeSpec.Builder typeBuilder,
                                                         ClassName referenceClassClassName) {
        this.typeBuilder = typeBuilder;
        this.referenceClassClassName = referenceClassClassName;
    }


    public ExecutorInvokingImplementationClassBuilder applyClassDefinition() {
        typeBuilder.addSuperinterface(
                ParameterizedTypeName.get(CLASS_NAME_EXECUTOR_INVOKING_IMPLEMENTATION,
                                          referenceClassClassName)
        );

        return this;
    }


    public ExecutorInvokingImplementationClassBuilder applyFields() {
        createFieldExecutor();

        createFieldRealImplementation();

        return this;
    }

    private void createFieldExecutor() {
        typeBuilder.addField(
                FieldSpec.builder(CLASS_NAME_EXECUTOR, FIELD_NAME_EXECUTOR)
                         .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                         .build()
        );
    }

    protected abstract void createFieldRealImplementation();


    public ExecutorInvokingImplementationClassBuilder applyMethods() {
        createMethodConstructor();

        createMethodGetExecutor();

        createMethodGetRealImplementation();

        createMethodSetRealImplementation();

        return this;
    }

    private void createMethodConstructor() {
        typeBuilder.addMethod(
                MethodSpec.constructorBuilder()
                          .addModifiers(Modifier.PUBLIC)
                          .addParameter(
                                  ParameterSpec.builder(CLASS_NAME_EXECUTOR,
                                                        PARAM_NAME_GET_EXECUTOR_EXECUTOR)
                                               .build()
                          )
                          .addCode(
                                  CodeBlock.builder()
                                           .addStatement("this.$L = $L", FIELD_NAME_EXECUTOR,
                                                         PARAM_NAME_GET_EXECUTOR_EXECUTOR)
                                           .build()
                          )
                          .build()
        );
    }

    private void createMethodGetExecutor() {
        typeBuilder.addMethod(
                MethodSpec.methodBuilder(METHOD_NAME_GET_EXECUTOR)
                          .addAnnotation(Override.class)
                          .addModifiers(Modifier.PUBLIC)
                          .returns(CLASS_NAME_EXECUTOR)
                          .addCode(
                                  CodeBlock.builder()
                                           .addStatement("return $L", FIELD_NAME_EXECUTOR)
                                           .build()
                          )
                          .build()
        );
    }

    protected abstract void createMethodGetRealImplementation();

    protected abstract void createMethodSetRealImplementation();
}
