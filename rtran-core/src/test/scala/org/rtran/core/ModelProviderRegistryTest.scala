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

package org.rtran.core

import org.rtran.api.{IModel, IModelProvider, IProjectCtx}
import org.rtran.core.mock._
import org.scalatest.{FlatSpecLike, Matchers}


class ModelProviderRegistryTest extends FlatSpecLike with Matchers {

  "ModelProviderRegistry" should "merge all implementations from metadata" in {
    ModelProviderRegistry.providers.size should not be 0
  }

  "ModelProviderRegistry" should "be able to register new provider at runtime" in {
    val size = ModelProviderRegistry.providers.size
    ModelProviderRegistry.registerProvider(new MyDummyModelProvider().asInstanceOf[IModelProvider[IModel, IProjectCtx]])
    ModelProviderRegistry.providers.size should be (size + 1)
  }

  "ModelProviderRegistry" should "find the correct provider according to model class and project class" in {
    ModelProviderRegistry.findProvider(classOf[MyFileModel], classOf[MyProject]).nonEmpty should be (true)
    ModelProviderRegistry.findProvider(classOf[MyDummyModel], classOf[MyProject]).nonEmpty should be (true)
    ModelProviderRegistry.findProvider(classOf[MyDummyModel2], classOf[MyProject]).nonEmpty should be (false)
  }

}
