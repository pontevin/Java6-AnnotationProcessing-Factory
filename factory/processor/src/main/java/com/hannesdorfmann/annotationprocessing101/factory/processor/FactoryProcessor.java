/*
 * Copyright (C) 2015 Hannes Dorfmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hannesdorfmann.annotationprocessing101.factory.processor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.hannesdorfmann.annotationprocessing101.factory.annotation.Factory;

/**
 * Annotation Processor for @Factory annotation
 *
 * @author Hannes Dorfmann
 */
@AutoService(Processor.class)
public class FactoryProcessor extends AbstractProcessor {

  private Types typeUtils;
  private Elements elementUtils;
  private Filer filer;
  private Messager messager;

  private final Map<String, FactoryGroupedClasses> factoryClasses = new LinkedHashMap<String, FactoryGroupedClasses>();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(Factory.class.getCanonicalName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(Factory.class);

      for (Element annotatedElement : annotatedElements) {

        // Check if the annotated element is a Class
        if (annotatedElement.getKind() != ElementKind.CLASS) {
          throw new ProcessingException(
            annotatedElement, "Only classes can be annotated with @%s", Factory.class.getSimpleName());
        }

        // We can cast it, because we know that it's of ElementKind.CLASS
        TypeElement typeElement = (TypeElement) annotatedElement;

        FactoryAnnotatedClass annotatedClass = new FactoryAnnotatedClass(typeElement);
        checkAnnotationValidity(annotatedClass);

        // Everything is fine, so try to add
        String qualifiedGroupName = annotatedClass.getQualifiedGroupClassName();
        FactoryGroupedClasses factoryClass = factoryClasses.get(qualifiedGroupName);
        if (factoryClass == null) {
          factoryClass = new FactoryGroupedClasses(qualifiedGroupName);
          factoryClasses.put(qualifiedGroupName, factoryClass);
        }

        // Checks if id is conflicting with another @Factory annotated class with the
        // same id
        factoryClass.add(annotatedClass);
      }

      // Generate code
      for (FactoryGroupedClasses factoryClass : factoryClasses.values()) {
        factoryClass.generateCode(elementUtils, filer);
      }
      factoryClasses.clear();
    }
    catch (ProcessingException e) {
      printError(e.getElement(), e.getMessage());
    }
    catch (IOException e) {
      printError(null, e.getMessage());
    }

    return true;
  }

  /**
   * Checks if the annotated element observes our rules
   */
  private void checkAnnotationValidity(FactoryAnnotatedClass item) throws ProcessingException {

    TypeElement classElement = item.getTypeElement();

    // Check if it's a public class
    if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
      throw new ProcessingException(classElement, "The class %s is not public.",
          classElement.getQualifiedName().toString());
    }

    // Check if it's an abstract class
    if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
      throw new ProcessingException(classElement,
          "The class %s is abstract. You can't annotate abstract classes with @%",
          classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
    }

    // Check inheritance: Class must be childclass as specified in @Factory.type();
    TypeElement superClassElement = elementUtils.getTypeElement(item.getQualifiedGroupClassName());
    if (superClassElement.getKind() == ElementKind.INTERFACE) {
      // Check interface implemented
      if (!classElement.getInterfaces().contains(superClassElement.asType())) {
        throw new ProcessingException(classElement,
            "The class %s annotated with @%s must implement the interface %s",
            classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
            item.getQualifiedGroupClassName());
      }
    } else {
      // Check subclassing
      TypeElement currentClass = classElement;
      while (true) {
        TypeMirror superClassType = currentClass.getSuperclass();

        if (superClassType.getKind() == TypeKind.NONE) {
          // Basis class (java.lang.Object) reached, so exit
          throw new ProcessingException(classElement,
              "The class %s annotated with @%s must inherit from %s",
              classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
              item.getQualifiedGroupClassName());
        }

        if (superClassType.toString().equals(item.getQualifiedGroupClassName())) {
          // Required super class found
          break;
        }

        // Moving up in inheritance tree
        currentClass = (TypeElement) typeUtils.asElement(superClassType);
      }
    }

    // Check if an empty public constructor is given
    for (Element enclosed : classElement.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
        ExecutableElement constructorElement = (ExecutableElement) enclosed;
        if (constructorElement.getParameters().size() == 0 && constructorElement.getModifiers()
            .contains(Modifier.PUBLIC)) {
          // Found an empty constructor
          return;
        }
      }
    }

    // No empty constructor found
    throw new ProcessingException(classElement,
        "The class %s must provide an public empty default constructor",
        classElement.getQualifiedName().toString());
  }

  /**
   * Prints an error message
   *
   * @param e   The element which has caused the error. Can be null
   * @param msg The error message
   */
  private void printError(Element e, String msg) {
    messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
  }
}
