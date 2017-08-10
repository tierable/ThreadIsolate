package com.tierable.threadisolate;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.lang.ref.WeakReference;

import javax.lang.model.element.Modifier;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
public class ExecutorInvokingImplementationClassBuilderWeakReference
        extends ExecutorInvokingImplementationClassBuilder {
    public static final ClassName CLASS_NAME_WEAK_REFERENCE = ClassName.get(
            WeakReference.class
    );


    private final TypeName realImplementationType;


    public ExecutorInvokingImplementationClassBuilderWeakReference(TypeSpec.Builder typeBuilder,
                                                                   ClassName referenceClass) {
        super(typeBuilder, referenceClass);

        realImplementationType = ParameterizedTypeName.get(
                CLASS_NAME_WEAK_REFERENCE, referenceClass
        );
    }


    @Override
    protected void createFieldRealImplementation() {
        typeBuilder.addField(
                FieldSpec.builder(realImplementationType, FIELD_NAME_REAL_IMPLEMENTATION,
                                  Modifier.PRIVATE)
                         .initializer(CodeBlock.of("new $T(null)", realImplementationType))
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
                                           .addStatement("return $L.get()",
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
                                           .addStatement("this.$L = new $T($L)",
                                                         FIELD_NAME_REAL_IMPLEMENTATION,
                                                         realImplementationType,
                                                         PARAM_NAME_SET_REAL_IMPLEMENTATION_REAL_IMPLEMENTATION)
                                           .build()
                          )
                          .build()
        );
    }
}
