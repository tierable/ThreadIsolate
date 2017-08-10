package com.tierable.threadisolate;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import static com.tierable.threadisolate.ExecutorInvokingImplementationClassBuilder.METHOD_NAME_GET_EXECUTOR;
import static com.tierable.threadisolate.ExecutorInvokingImplementationClassBuilder.METHOD_NAME_GET_REAL_IMPLEMENTATION;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
public class ReferenceClassClassBuilder {
    public static final ClassName CLASS_NAME_OBJECT                 = ClassName.get(
            Object.class
    );
    public static final ClassName CLASS_NAME_NULL_POINTER_EXCEPTION = ClassName.get(
            NullPointerException.class
    );


    private static final String VARIABLE_NAME_REAL_IMPLEMENTATION = "realImplementation";
    private static final String SEPARATOR_PARAMETERS              = ", ";


    private final TypeSpec.Builder               typeBuilder;
    private final ClassName                      referenceClassClassName;
    private final Element                        referenceClassElement;
    private final Elements                       elementUtils;
    private final LinkedHashMap<Element, String> warnings;


    public ReferenceClassClassBuilder(TypeSpec.Builder typeBuilder, ClassName referenceClassClassName,
                                      Element referenceClassElement, Elements elementUtils) {
        this.typeBuilder = typeBuilder;
        this.referenceClassClassName = referenceClassClassName;
        this.referenceClassElement = referenceClassElement;
        this.elementUtils = elementUtils;
        this.warnings = new LinkedHashMap<>();
    }


    public ReferenceClassClassBuilder applyClassDefinition() {
        typeBuilder.addSuperinterface(referenceClassClassName);

        return this;
    }


    public ReferenceClassClassBuilder applyFields() {

        return this;
    }


    public ReferenceClassClassBuilder applyMethods() {
        List<ExecutableElement> methods = getMethodsFromInterface(
                (TypeElement) referenceClassElement
        );

        for (ExecutableElement method : methods) {
            typeBuilder.addMethod(createMethodImplementation(method));
        }

        return this;
    }


    private MethodSpec createMethodImplementation(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        List<? extends VariableElement> methodParameters = method.getParameters();
        TypeMirror methodReturnType = method.getReturnType();
        boolean methodReturnsVoid = methodReturnType.getKind() == TypeKind.VOID;

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                                                     .addAnnotation(Override.class)
                                                     .addModifiers(Modifier.PUBLIC)
                                                     .returns(TypeName.get(methodReturnType));

        CodeBlock.Builder callRealMethodCodeBuilder =
                CodeBlock.builder()
                         .addStatement("$T $L = $L()", referenceClassClassName,
                                       VARIABLE_NAME_REAL_IMPLEMENTATION,
                                       METHOD_NAME_GET_REAL_IMPLEMENTATION)
                         .beginControlFlow("if ($L != null)", VARIABLE_NAME_REAL_IMPLEMENTATION);

        if (!methodReturnsVoid) {
            callRealMethodCodeBuilder.add("return ");
        }
        callRealMethodCodeBuilder.add("$L.$L(", VARIABLE_NAME_REAL_IMPLEMENTATION, methodName);

        for (int i = 0; i < methodParameters.size(); i++) {
            VariableElement parameter = methodParameters.get(i);
            Name paramName = parameter.getSimpleName();

            methodBuilder.addParameter(
                    ParameterSpec.get(parameter).toBuilder()
                                 .addModifiers(Modifier.FINAL)
                                 .build()
            );

            callRealMethodCodeBuilder.add("$L", paramName);

            if (i < methodParameters.size() - 1) {
                callRealMethodCodeBuilder.add(SEPARATOR_PARAMETERS);
            }
        }
        callRealMethodCodeBuilder.add(");\n");

        if (methodReturnsVoid) {
            callRealMethodCodeBuilder.endControlFlow();
        }


        if (methodReturnsVoid) {
            TypeSpec runnableTypeSpec =
                    TypeSpec.anonymousClassBuilder("")
                            .superclass(Runnable.class)
                            .addMethod(
                                    MethodSpec.methodBuilder("run")
                                              .addAnnotation(Override.class)
                                              .addModifiers(Modifier.PUBLIC)
                                              .addCode(callRealMethodCodeBuilder.build())
                                              .build())
                            .build();

            methodBuilder.addCode(
                    CodeBlock.builder()
                             .addStatement("$L().execute($L)", METHOD_NAME_GET_EXECUTOR,
                                           runnableTypeSpec)
                             .build()
            );
        } else {
            CodeBlock.Builder methodInvocation =
                    CodeBlock.builder()
                             .add(callRealMethodCodeBuilder.build())
                             .nextControlFlow("else")
                             .addStatement("throw new $T()", CLASS_NAME_NULL_POINTER_EXCEPTION)
                             .endControlFlow();
            methodBuilder.addCode(methodInvocation.build());

            methodBuilder.addException(CLASS_NAME_NULL_POINTER_EXCEPTION);

            warnings.put(
                    method,
                    String.format(Locale.ENGLISH, "method returns a non-void value, may throw a %s",
                                  NullPointerException.class.getSimpleName())
            );
        }

        return methodBuilder.build();
    }


    private List<ExecutableElement> getMethodsFromInterface(TypeElement element) {
        List<? extends Element> objectMembers = elementUtils.getAllMembers(
                elementUtils.getTypeElement(CLASS_NAME_OBJECT.toString())
        );

        List<? extends Element> allMembers = new ArrayList<>(
                elementUtils.getAllMembers(element)
        );

        allMembers.removeAll(objectMembers);

        return ElementFilter.methodsIn(allMembers);
    }


    public LinkedHashMap<Element, String> getWarnings() {
        return warnings;
    }
}
