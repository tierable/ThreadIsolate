package com.tierable.threadisolate;


import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
public class ExecutorInvokingImplementationClassBuilderStrongReference
        extends ExecutorInvokingImplementationClassBuilder {
    public ExecutorInvokingImplementationClassBuilderStrongReference(TypeSpec.Builder typeBuilder,
                                                                     TypeElement referenceClassElement) {
        super(typeBuilder, referenceClassElement);
    }


    @Override
    protected void createFieldRealImplementation() {
        typeBuilder.addField(
                FieldSpec.builder(referenceClassTypeName, FIELD_NAME_REAL_IMPLEMENTATION,
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
                          .returns(referenceClassTypeName)
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
                                  ParameterSpec.builder(referenceClassTypeName,
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
