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

import org.junit.Test;

import java.util.Optional;


public class GenericsUtilTest {

  MyInterface<?, ?> instance = new MyImplementation();

  AnotherInterface instance2 = new AnotherImplementation();

  @Test
  public void testGettingActualTypeClass() {
    Optional<Class<Integer>> firstClass = GenericsUtil.getClassForGenericType(instance.getClass(), MyInterface.class, 0);
    assert firstClass.isPresent();
    Optional<Class<String>> secondClass = GenericsUtil.getClassForGenericType(instance.getClass(), MyInterface.class, 1);
    assert secondClass.isPresent();
  }

  @Test
  public void testIndexOutOfBound() {
    Optional<Class<Integer>> clazz = GenericsUtil.getClassForGenericType(instance.getClass(), MyInterface.class, 2);
    assert !clazz.isPresent();
  }

  @Test
  public void testInstanceWihtoutTypeParameters() {
    Optional<Class<String>> clazz = GenericsUtil.getClassForGenericType(instance2.getClass(), AnotherInterface.class, 0);
    assert !clazz.isPresent();
  }
}

interface MyInterface<T, U> {}

class MyImplementation implements MyInterface<Integer, String> {}

interface AnotherInterface {}

class AnotherImplementation implements AnotherInterface{}