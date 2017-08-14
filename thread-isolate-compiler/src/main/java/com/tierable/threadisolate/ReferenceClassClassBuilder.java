package com.tierable.threadisolate;


import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.tierable.threadisolate.ExecutorInvokingImplementationClassBuilder.FIELD_NAME_REAL_IMPLEMENTATION;
import static com.tierable.threadisolate.ExecutorInvokingImplementationClassBuilder.METHOD_NAME_GET_EXECUTOR;
import static com.tierable.threadisolate.ExecutorInvokingImplementationClassBuilder.METHOD_NAME_GET_REAL_IMPLEMENTATION;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
public class ReferenceClassClassBuilder {
    private static final String SEPARATOR_PARAMETERS = ", ";


    private final TypeSpec.Builder               typeBuilder;
    private final TypeElement                    referenceClassElement;
    private final List<TypeMirror>               excludedSuperTypes;
    private final Elements                       elementUtils;
    private final Types                          typeUtils;
    private final LinkedHashMap<Element, String> warnings;


    public ReferenceClassClassBuilder(Builder typeBuilder, TypeElement referenceClassElement, List<TypeMirror> excludedSuperTypes, Elements elementUtils, Types typeUtils) {
        this.typeBuilder = typeBuilder;
        this.referenceClassElement = referenceClassElement;
        this.excludedSuperTypes = excludedSuperTypes;
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
        this.warnings = new LinkedHashMap<>();
    }


    public ReferenceClassClassBuilder applyClassDefinition() {
        typeBuilder.addSuperinterface(TypeName.get(referenceClassElement.asType()));
        List<? extends TypeParameterElement> typeParameters = referenceClassElement.getTypeParameters();
        for (TypeParameterElement typeParameter : typeParameters) {
            typeBuilder.addTypeVariable(TypeVariableName.get(typeParameter));
        }

        return this;
    }


    public ReferenceClassClassBuilder applyFields() {
        // No fields

        return this;
    }


    public ReferenceClassClassBuilder applyMethods() {
        List<ExecutableElement> methods = getMethodsFromType(referenceClassElement);

        for (ExecutableElement method : methods) {
            typeBuilder.addMethod(createMethodImplementation(method));
        }

        return this;
    }


    private boolean isMethodExcluded(ExecutableElement method) {
        TypeMirror typeMirror = method.getEnclosingElement().asType();

        for (TypeMirror excludedSuperType : excludedSuperTypes) {
            if (typeUtils.isSameType(typeUtils.erasure(excludedSuperType),
                                     typeUtils.erasure(typeMirror))) {
                return true;
            }
        }
        return false;
    }

    private MethodSpec createMethodImplementation(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        List<? extends VariableElement> methodParameters = method.getParameters();
        boolean methodReturnsVoid = method.getReturnType().getKind() == TypeKind.VOID;
        boolean hasDontInvokeOnExecutorAnnotation =
                method.getAnnotation(DontInvokeOnExecutor.class) != null;
        boolean isMethodExcluded = isMethodExcluded(method);

        boolean shouldInvokeOnExecutor = methodReturnsVoid && !hasDontInvokeOnExecutorAnnotation
                && !isMethodExcluded;


        CodeBlock.Builder callRealMethodCodeBuilder = CodeBlock.builder();

        if (!methodReturnsVoid) {
            callRealMethodCodeBuilder.add("return ");
        }

        callRealMethodCodeBuilder
                .add("$L().$L(", METHOD_NAME_GET_REAL_IMPLEMENTATION, methodName);

        for (int i = 0; i < methodParameters.size(); i++) {
            VariableElement parameter = methodParameters.get(i);

            Name paramName = parameter.getSimpleName();

            callRealMethodCodeBuilder.add("$L", paramName);

            if (i < methodParameters.size() - 1) {
                callRealMethodCodeBuilder.add(SEPARATOR_PARAMETERS);
            }
        }
        callRealMethodCodeBuilder.add(");\n");


        MethodSpec.Builder methodBuilder = createMethodOverride(method);
        if (shouldInvokeOnExecutor) {
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
            methodBuilder.addCode(callRealMethodCodeBuilder.build());

            warnings.put(
                    method,
                    String.format(Locale.ENGLISH, "method may throw a %s if the %s is not set",
                                  NullPointerException.class.getSimpleName(),
                                  FIELD_NAME_REAL_IMPLEMENTATION)
            );

        }

        return methodBuilder.build();
    }


    private List<ExecutableElement> getMethodsFromType(TypeElement element) {
        List<? extends Element> objectMembers = elementUtils.getAllMembers(
                elementUtils.getTypeElement(Object.class.getCanonicalName())
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


    // TODO: preferably, we'd use a MethodSpec.overriding, but it doesn't support final modifiers
    private MethodSpec.Builder createMethodOverride(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

        methodBuilder.addAnnotation(Override.class);

        Set<Modifier> modifiers = method.getModifiers();
        modifiers = new LinkedHashSet<>(modifiers);
        modifiers.remove(Modifier.ABSTRACT);
        methodBuilder.addModifiers(modifiers);

        for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
            TypeVariable var = (TypeVariable) typeParameterElement.asType();
            methodBuilder.addTypeVariable(TypeVariableName.get(var));
        }


        ExecutableType executableType = (ExecutableType) typeUtils.asMemberOf(
                ((DeclaredType) referenceClassElement.asType()), method
        );

        methodBuilder.returns(TypeName.get(executableType.getReturnType()));

        List<? extends VariableElement> parameters = method.getParameters();
        List<? extends TypeMirror> resolvedParameterTypes = executableType.getParameterTypes();

        for (int i = 0; i < parameters.size(); i++) {
            VariableElement parameter = parameters.get(i);
            methodBuilder.addParameter(
                    ParameterSpec.builder(TypeName.get(resolvedParameterTypes.get(i)),
                                          parameter.getSimpleName().toString(), Modifier.FINAL)
                                 .build()
            );
        }

        methodBuilder.varargs(method.isVarArgs());

        for (TypeMirror thrownType : method.getThrownTypes()) {
            methodBuilder.addException(TypeName.get(thrownType));
        }

        return methodBuilder;
    }
}
