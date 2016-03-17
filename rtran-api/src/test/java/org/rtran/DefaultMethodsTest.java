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

package org.rtran;

import org.rtran.api.IModelProvider;
import org.rtran.api.IRule;
import org.junit.Test;
import org.rtran.mock.MockModel;
import org.rtran.mock.MockProject;

public class DefaultMethodsTest {

  @Test
  public void testRuntimeModelClassForRule()
      throws IllegalAccessException, ClassNotFoundException, InstantiationException {
    IRule<?> rule = Class.forName("org.rtran.mock.MockRule").asSubclass(IRule.class).newInstance();
    assert rule.rutimeModelClass().equals(MockModel.class);
  }

  @Test
  public void testRutimeModelClassAndRuntimeProjectClassForModelProvider()
      throws IllegalAccessException, ClassNotFoundException, InstantiationException {
    IModelProvider<?, ?> provider = Class.forName("org.rtran.mock.MockModelProvider")
        .asSubclass(IModelProvider.class).newInstance();
    assert provider.runtimeModelClass().equals(MockModel.class);
    assert provider.runtimeProjectCtxClass().equals(MockProject.class);
  }

  @Test
  public void testIsEligibleForRule()
      throws IllegalAccessException, ClassNotFoundException, InstantiationException {
    IRule<?> rule = Class.forName("org.rtran.mock.MockRule").asSubclass(IRule.class).newInstance();
    assert rule.isEligibleFor(() -> null);
  }
}
