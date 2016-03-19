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

package com.ebay.rtran.core

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ebay.rtran.api.{IModel, IModelProvider, IProjectCtx}

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConversions._


object ModelProviderRegistry extends LazyLogging {

  val PATH_TO_MODEL_PROVIDER = "rtran.model-providers"

  private[this] var modelProviders = loadModelProviders(UpgraderMeta.configs)

  def findProvider(modelClass: Class[_ <: IModel], projectCtxClass: Class[_ <: IProjectCtx]) =
    modelProviders get (modelClass, projectCtxClass) orElse {
      modelProviders find {
        case ((mclass, pclass), provider) => mclass == modelClass && pclass.isAssignableFrom(projectCtxClass)
      } map (_._2)
    }

  def providers = modelProviders.values

  private[rtran] def registerProvider[T <: IModelProvider[IModel, IProjectCtx]](provider: T): Unit = {
    modelProviders += (provider.runtimeModelClass, provider.runtimeProjectCtxClass) -> provider
  }

  private def loadModelProviders(configs: Iterator[Config]) = {
    var providers = Map.empty[(Class[_ <: IModel], Class[_ <: IProjectCtx]), IModelProvider[IModel, IProjectCtx]]
    configs.filter(_.hasPath(PATH_TO_MODEL_PROVIDER)).flatMap(_.getStringList(PATH_TO_MODEL_PROVIDER)) foreach {className =>
      loadModelProvider(className) match {
        case Success(provider) if providers contains (provider.runtimeModelClass, provider.runtimeProjectCtxClass) =>
          val modelClass = provider.runtimeModelClass
          val projectCtxClass = provider.runtimeProjectCtxClass
          if (providers((modelClass, projectCtxClass)).getClass == provider.getClass) {
            logger.warn("Get duplicated model provider definition for {}", provider.getClass)
          } else {
            logger.warn("Model provider {} already exists for {}", provider.getClass, (modelClass, projectCtxClass))
          }
        case Success(provider) =>
          providers += (provider.runtimeModelClass, provider.runtimeProjectCtxClass) -> provider
        case Failure(e) =>
          logger.error("Failed to create provider instance {}, {}", className, e)
      }
    }
    providers
  }

  private def loadModelProvider(className: String) = Try {
    Class.forName(className).asSubclass(classOf[IModelProvider[IModel, IProjectCtx]]).newInstance
  }

}
