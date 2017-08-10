package com.tierable.threadisolate;


import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
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
public class ThreadIsolateProcessor
        extends AbstractProcessor {
    private static final boolean IS_TEST = false;

    public static final ClassName CLASS_NAME_EXECUTOR_INVOKING_IMPLEMENTATION = ClassName.get(
            ExecutorInvokingImplementation.class
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

        annotations.add(InvokeMethodsOnExecutor.class);

        return annotations;
    }
    //endregion


    @Override
    public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
        if (env.processingOver()) {
            return true;
        }

        Set<? extends Element> annotatedElements = env.getElementsAnnotatedWith(
                InvokeMethodsOnExecutor.class
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
                TypeElement elementAsTypeElement = (TypeElement) element;


                TypeElement enclosingElement = findEnclosingTypeElement(element);

                String packageName = getPackage(enclosingElement).getQualifiedName().toString();
                String className = enclosingElement.getQualifiedName()
                                                   .toString()
                                                   .substring(packageName.length() + 1);

                ClassName referenceClass = ClassName.get(packageName, className);
                String sanitisedTargetClassName = className.replace('.', '$');
                ClassName generatedClassName = ClassName.get(
                        packageName,
                        CLASS_NAME_EXECUTOR_INVOKING_IMPLEMENTATION.simpleName() + sanitisedTargetClassName
                );
                InvokeMethodsOnExecutor invokeMethodsOnExecutorAnnotation = element.getAnnotation(
                        InvokeMethodsOnExecutor.class
                );
                boolean useWeakReference = invokeMethodsOnExecutorAnnotation.useWeakReference();


                TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(generatedClassName)
                                                       .addModifiers(Modifier.PUBLIC);

                ExecutorInvokingImplementationClassBuilder.get(typeBuilder, elementAsTypeElement,
                                                               useWeakReference)
                                                          .applyClassDefinition()
                                                          .applyFields()
                                                          .applyMethods();


                ReferenceClassClassBuilder referenceClassClassBuilder = new ReferenceClassClassBuilder(
                        typeBuilder, elementAsTypeElement, elementUtils, typeUtils
                );
                referenceClassClassBuilder.applyClassDefinition()
                                          .applyFields()
                                          .applyMethods();

                LinkedHashMap<Element, String> warnings = referenceClassClassBuilder.getWarnings();
                for (Entry<Element, String> warningInfo : warnings.entrySet()) {
                    warning(warningInfo.getKey(), warningInfo.getValue());
                }


                try {
                    JavaFile javaFile = JavaFile.builder(packageName, typeBuilder.build())
                                                .addFileComment(
                                                        "This code was generated by a tool " +
                                                                "(ThreadIsolateProcessor, not a " +
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
