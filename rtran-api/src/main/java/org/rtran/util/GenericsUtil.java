/*
 * Copyright (c) 2016 eBay Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rtran.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GenericsUtil {

  @SuppressWarnings("unchecked")
  static public <T, U> Optional<Class<T>> getClassForGenericType(Class<? extends U> instanceClass,
                                                                 Class<U> interfaceClass,
                                                                 int index) {
    List<Type> types = new ArrayList<>();
    Class<?> clazz = instanceClass;
    while (clazz != null) {
      Collections.addAll(types, clazz.getGenericInterfaces());
      clazz = clazz.getSuperclass();
    }
    Optional<ParameterizedType> typeOpt = Optional.empty();
    for (Type t : types) {
      if (t instanceof ParameterizedType) {
        ParameterizedType parameterizedType = (ParameterizedType) t;
        if (parameterizedType.getRawType().getTypeName().equals(interfaceClass.getTypeName())) {
          typeOpt = Optional.of(parameterizedType);
        }
      }
    }
    return typeOpt.map(type -> {
      Type [] typeArgs = type.getActualTypeArguments();
      if (index < typeArgs.length)
        return (Class<T>) typeArgs[index];
      else
        return null;
    });
  }
}
