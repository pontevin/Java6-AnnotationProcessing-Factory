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

import com.hannesdorfmann.annotationprocessing101.factory.annotation.Factory;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import org.apache.commons.lang3.StringUtils;

/**
 * Holds the information about a class annotated with @Factory
 *
 * @author Hannes Dorfmann
 */
public class FactoryAnnotatedClass {

  private final TypeElement annotatedClassElement;
  private final String qualifiedGroupClassName;
  private final String id;

  public FactoryAnnotatedClass(TypeElement classElement) throws ProcessingException {
    annotatedClassElement = classElement;
    
    Factory annotation = classElement.getAnnotation(Factory.class);
    id = annotation.id();

    if (StringUtils.isEmpty(id)) {
      throw new ProcessingException(classElement,
          "id() in @%s for class %s must not be null or empty!",
          Factory.class.getSimpleName(), classElement.getQualifiedName().toString());
    }

    qualifiedGroupClassName = determineGroupClassName(annotation);
  }

  private String determineGroupClassName(Factory annotation) {
    try {
      Class<?> clazz = annotation.type();
      return clazz.getCanonicalName();
    }
    catch (MirroredTypeException mte) {
      DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
      TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
      return classTypeElement.getQualifiedName().toString();
    }
  }

  /**
   * Get the id as specified in {@link Factory#id()}.
   */
  public String getId() {
    return id;
  }

  /**
   * Get the full qualified name of the type specified in {@link Factory#type()}.
   */
  public String getQualifiedGroupClassName() {
    return qualifiedGroupClassName;
  }

  /**
   * The original element that was annotated with @Factory
   */
  public TypeElement getTypeElement() {
    return annotatedClassElement;
  }
}
