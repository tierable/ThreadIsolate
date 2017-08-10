package com.tierable.threadisolate;


import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import static com.google.auto.common.MoreElements.getPackage;


/**
 * @author Aniruddh Fichadia
 * @date 2017-08-10
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ThreadIsolateCompiler
        extends AbstractProcessor {
    private static final boolean IS_TEST = true;

    public static final ClassName CLASS_NAME_THREAD_ISOLATED_IMPLEMENTATION = ClassName.get(
            ThreadEnforcedImplementation.class
    );
    public static final ClassName CLASS_NAME_OBJECT                         = ClassName.get(
            Object.class
    );
    public static final ClassName CLASS_NAME_EXECUTOR                       = ClassName.get(
            Executor.class
    );
    public static final ClassName CLASS_NAME_WEAK_REFERENCE                 = ClassName.get(
            WeakReference.class
    );
    public static final ClassName CLASS_NAME_NULL_POINTER_EXCEPTION         = ClassName.get(
            NullPointerException.class
    );


    private Filer    filer;
    private Elements elementUtils;
    private Types    typeUtils;


    //region Initialisation boilerplate
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();

        annotations.add(InvocationsThreadEnforced.class);

        return annotations;
    }
    //endregion


    @Override
    public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
        if (env.processingOver()) {
            return true;
        }

        Set<? extends Element> annotatedElements = env.getElementsAnnotatedWith(
                InvocationsThreadEnforced.class
        );
        if (annotatedElements.isEmpty()) {
            // Early exit if there are no elements, or if this is another annotation processing
            // pass without any annotated elements remaining
            return false;
        }

        for (Element element : annotatedElements) {
            if (!SuperficialValidation.validateElement(element)) {
                error(element, "Not a valid class");
                continue;
            }

            if (ElementKind.INTERFACE.equals(element.getKind())) {
                TypeElement enclosingElement = findEnclosingTypeElement(element);

                String packageName = getPackage(enclosingElement).getQualifiedName().toString();
                String className = enclosingElement.getQualifiedName()
                                                   .toString()
                                                   .substring(packageName.length() + 1);

                InvocationsThreadEnforced invocationsThreadEnforcedAnnotation = element.getAnnotation(
                        InvocationsThreadEnforced.class);
                boolean useWeakReference = invocationsThreadEnforcedAnnotation.useWeakReference();


                ClassName referenceClass = ClassName.get(packageName, className);
                String sanitisedTargetClassName = className.replace('.', '$');
                ClassName generatedClassName = ClassName.get(
                        packageName,
                        CLASS_NAME_THREAD_ISOLATED_IMPLEMENTATION.simpleName() + sanitisedTargetClassName
                );

                TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(generatedClassName)
                                                       .addModifiers(Modifier.PUBLIC);

                final String fieldNameExecutor = "executor";
                final String paramNameExecutor = "executor";
                final String methodNameGetExecutor = "getExecutor";

                typeBuilder.addSuperinterface(
                        ParameterizedTypeName.get(CLASS_NAME_THREAD_ISOLATED_IMPLEMENTATION,
                                                  referenceClass))
                           .addField(
                                   FieldSpec.builder(CLASS_NAME_EXECUTOR, fieldNameExecutor)
                                            .addModifiers(Modifier.PRIVATE,
                                                          Modifier.FINAL)
                                            .build()
                           )
                           .addMethod(
                                   MethodSpec.constructorBuilder()
                                             .addModifiers(Modifier.PUBLIC)
                                             .addParameter(
                                                     ParameterSpec.builder(CLASS_NAME_EXECUTOR,
                                                                           paramNameExecutor)
                                                                  .build()
                                             )
                                             .addCode(
                                                     CodeBlock.builder()
                                                              .addStatement(
                                                                      "this.$L = $L",
                                                                      fieldNameExecutor,
                                                                      paramNameExecutor
                                                              )
                                                              .build()
                                             )
                                             .build()
                           )
                           .addMethod(
                                   MethodSpec.methodBuilder(methodNameGetExecutor)
                                             .addAnnotation(Override.class)
                                             .addModifiers(Modifier.PUBLIC)
                                             .returns(CLASS_NAME_EXECUTOR)
                                             .addCode(
                                                     CodeBlock.builder()
                                                              .addStatement("return $L",
                                                                            fieldNameExecutor)
                                                              .build()
                                             )
                                             .build()
                           );

                final String fieldNameRealImplementationReference = "realImplementationReference";
                final String methodNameGetRealImplementation = "getRealImplementation";
                final String methodNameSetRealImplementation = "setRealImplementation";
                final String paramNameRealImplementation = "realImplementation";

                TypeName realImplementationType;
                if (useWeakReference) {
                    realImplementationType = ParameterizedTypeName.get(
                            CLASS_NAME_WEAK_REFERENCE, referenceClass
                    );
                } else {
                    realImplementationType = referenceClass;
                }


                CodeBlock.Builder realImplementationInitialiser = CodeBlock.builder();

                if (useWeakReference) {
                    realImplementationInitialiser.add("new $T(null)", realImplementationType);
                } else {
                    realImplementationInitialiser.add("null");
                }

                typeBuilder.addField(
                        FieldSpec.builder(realImplementationType,
                                          fieldNameRealImplementationReference, Modifier.PRIVATE)
                                 .initializer(
                                         realImplementationInitialiser.build()
                                 )
                                 .build()
                );


                CodeBlock.Builder setRealImplementationCodeBuilder = CodeBlock.builder();
                if (useWeakReference) {
                    setRealImplementationCodeBuilder.addStatement(
                            "this.$L = new $T($L)",
                            fieldNameRealImplementationReference,
                            realImplementationType,
                            paramNameRealImplementation
                    );
                } else {
                    setRealImplementationCodeBuilder.addStatement(
                            "this.$L = $L",
                            fieldNameRealImplementationReference,
                            paramNameRealImplementation
                    );
                }

                CodeBlock.Builder getRealImplementationCodeBuilder = CodeBlock.builder();
                if (useWeakReference) {
                    getRealImplementationCodeBuilder.addStatement("return $L.get()",
                                                                  fieldNameRealImplementationReference);
                } else {
                    getRealImplementationCodeBuilder.addStatement("return $L",
                                                                  fieldNameRealImplementationReference);
                }

                typeBuilder.addMethod(
                        MethodSpec.methodBuilder(methodNameGetRealImplementation)
                                  .addAnnotation(Override.class)
                                  .addModifiers(Modifier.PUBLIC)
                                  .returns(referenceClass)
                                  .addCode(
                                          getRealImplementationCodeBuilder.build()
                                  )
                                  .build()
                )
                           .addMethod(
                                   MethodSpec.methodBuilder(methodNameSetRealImplementation)
                                             .addAnnotation(Override.class)
                                             .addModifiers(Modifier.PUBLIC)
                                             .addParameter(
                                                     ParameterSpec.builder(referenceClass,
                                                                           paramNameRealImplementation)
                                                                  .build()
                                             )
                                             .addCode(
                                                     setRealImplementationCodeBuilder
                                                             .build()
                                             )
                                             .build()
                           );


                typeBuilder.addSuperinterface(referenceClass);

                List<ExecutableElement> methods = getMethodsFromInterface((TypeElement) element);
                for (ExecutableElement method : methods) {
                    final String variableNameRealImplementation = "realImplementation";

                    String methodName = method.getSimpleName().toString();
                    List<? extends VariableElement> methodParameters = method.getParameters();
                    TypeMirror methodReturnType = method.getReturnType();
                    boolean methodReturnsVoid = methodReturnType.getKind() == TypeKind.VOID;


                    MethodSpec.Builder methodBuilder =
                            MethodSpec.methodBuilder(methodName)
                                      .addAnnotation(Override.class)
                                      .addModifiers(Modifier.PUBLIC)
                                      .returns(TypeName.get(methodReturnType));


                    CodeBlock.Builder methodInvocation =
                            CodeBlock.builder();
                    if (useWeakReference) {
                        methodInvocation.addStatement("$T $L = $L.get()", referenceClass,
                                                      variableNameRealImplementation,
                                                      fieldNameRealImplementationReference);
                    } else {
                        methodInvocation.addStatement("$T $L = $T.this.$L", referenceClass,
                                                      variableNameRealImplementation,
                                                      generatedClassName,
                                                      fieldNameRealImplementationReference);
                    }
                    methodInvocation.beginControlFlow(
                            "if ($L != null)",
                            variableNameRealImplementation
                    );

                    if (!methodReturnsVoid) {
                        methodInvocation.add("return ");
                    }
                    methodInvocation.add("$L.$L(", variableNameRealImplementation,
                                         methodName);

                    for (int i = 0; i < methodParameters.size(); i++) {
                        VariableElement parameter = methodParameters.get(i);

                        TypeName paramType = TypeName.get(parameter.asType());
                        Name paramName = parameter.getSimpleName();

                        Set<Modifier> modifiers = new HashSet<>(parameter.getModifiers());
                        if (methodReturnsVoid) {
                            modifiers.add(Modifier.FINAL);
                        }

                        Modifier[] modifierArr = new Modifier[modifiers.size()];
                        modifiers.toArray(modifierArr);

                        methodBuilder.addParameter(paramType,
                                                   paramName.toString(),
                                                   modifierArr);

                        methodInvocation.add("$L", paramName);

                        if (i < methodParameters.size() - 1) {
                            final String separator = ", ";

                            methodInvocation.add(separator);
                        }
                    }
                    methodInvocation.add(");\n");

                    if (methodReturnsVoid) {
                        methodInvocation.endControlFlow();
                    }


                    if (methodReturnsVoid) {
                        TypeSpec runCode =
                                TypeSpec.anonymousClassBuilder("")
                                        .superclass(Runnable.class)
                                        .addMethod(
                                                MethodSpec.methodBuilder("run")
                                                          .addAnnotation(Override.class)
                                                          .addModifiers(Modifier.PUBLIC)
                                                          .addCode(methodInvocation.build())
                                                          .build())
                                        .build();

                        methodBuilder.addCode(
                                CodeBlock.builder()
                                         .addStatement("$L().execute($L)", methodNameGetExecutor,
                                                       runCode)
                                         .build()
                        );

                        typeBuilder.addMethod(methodBuilder.build());
                    } else {
                        warning(method, "method returns a non-void value, may throw a %s",
                                NullPointerException.class.getSimpleName());

                        methodInvocation.nextControlFlow("else")
                                        .addStatement("throw new $T()",
                                                      CLASS_NAME_NULL_POINTER_EXCEPTION)
                                        .endControlFlow();


                        methodBuilder.addException(CLASS_NAME_NULL_POINTER_EXCEPTION);

                        methodBuilder.addCode(methodInvocation.build());

                        typeBuilder.addMethod(methodBuilder.build());
                    }
                }

                try {
                    JavaFile javaFile = JavaFile.builder(packageName, typeBuilder.build())
                                                .addFileComment(
                                                        "This code was generated by a tool " +
                                                                "(ThreadIsolateCompiler, not a " +
                                                                "human tool :D)"
                                                )
                                                .build();
                    javaFile.writeTo(filer);

                    if (IS_TEST) {
                        javaFile.writeTo(System.out);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                error(element, "Must be an interface");
            }
        }

        return true;
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


    //region Util
    public static TypeElement findEnclosingTypeElement(Element e) {
        while (e != null && !(e instanceof TypeElement)) {
            e = e.getEnclosingElement();
        }
        return TypeElement.class.cast(e);
    }
    //endregion


    //region Logging
    private void error(Element element, String message, Object... args) {
        printMessage(Kind.ERROR, element, message, args);
    }

    private void note(Element element, String message, Object... args) {
        printMessage(Kind.NOTE, element, message, args);
    }

    private void warning(Element element, String message, Object... args) {
        printMessage(Kind.WARNING, element, message, args);
    }

    private void printMessage(Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        processingEnv.getMessager().printMessage(kind, message, element);
    }
    //endregion
}
