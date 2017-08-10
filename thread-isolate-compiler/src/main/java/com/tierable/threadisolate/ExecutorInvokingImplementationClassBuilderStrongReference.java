package com.tierable.threadisolate;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
public class ExecutorInvokingImplementationClassBuilderStrongReference
        extends ExecutorInvokingImplementationClassBuilder {
    public ExecutorInvokingImplementationClassBuilderStrongReference(TypeSpec.Builder typeBuilder,
                                                                     ClassName referenceClass) {
        super(typeBuilder, referenceClass);
    }


    @Override
    protected void createFieldRealImplementation() {
        typeBuilder.addField(
                FieldSpec.builder(referenceClassClassName, FIELD_NAME_REAL_IMPLEMENTATION,
                                  Modifier.PRIVATE)
                         .initializer(CodeBlock.of("null"))
                         .build()
        );
    }


    @Override
    protected void createMethodGetRealImplementation() {
        typeBuilder.addMethod(
                MethodSpec.methodBuilder(METHOD_NAME_GET_REAL_IMPLEMENTATION)
                          .addAnnotation(Override.class)
                          .addModifiers(Modifier.PUBLIC)
                          .returns(referenceClassClassName)
                          .addCode(
                                  CodeBlock.builder()
                                           .addStatement("return $L",
                                                         FIELD_NAME_REAL_IMPLEMENTATION)
                                           .build()
                          )
                          .build()
        );
    }

    @Override
    protected void createMethodSetRealImplementation() {
        typeBuilder.addMethod(
                MethodSpec.methodBuilder(METHOD_NAME_SET_REAL_IMPLEMENTATION)
                          .addAnnotation(Override.class)
                          .addModifiers(Modifier.PUBLIC)
                          .addParameter(
                                  ParameterSpec.builder(referenceClassClassName,
                                                        PARAM_NAME_SET_REAL_IMPLEMENTATION_REAL_IMPLEMENTATION)
                                               .build()
                          )
                          .addCode(
                                  CodeBlock.builder()
                                           .addStatement("this.$L = $L",
                                                         FIELD_NAME_REAL_IMPLEMENTATION,
                                                         PARAM_NAME_SET_REAL_IMPLEMENTATION_REAL_IMPLEMENTATION)
                                           .build()
                          )
                          .build()
        );
    }
}
