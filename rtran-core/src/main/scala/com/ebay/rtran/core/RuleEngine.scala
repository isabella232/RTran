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

import com.ebay.rtran.api.{IModel, IProjectCtx, IRule}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


class RuleEngine extends LazyLogging {

  def execute[P <: IProjectCtx](projectCtx: P, configuration: UpgradeConfiguration): Unit = {
    execute(projectCtx, configuration.ruleInstances)
  }

  def execute[P <: IProjectCtx](projectCtx: P, rules: java.util.List[_ <: IRule[_ <: IModel]]): Unit = {
    rules foreach { rule =>
      logger.info("Executing rule {} ...", rule.id)
      val start = Deadline.now
      executeRule(rule.asInstanceOf[IRule[IModel]], projectCtx)
      val elapsed = (Deadline.now - start).toMillis
      logger.info("Executed rule {} in {} ms", rule.id, elapsed.toString)
    }
  }

  private def executeRule(rule: IRule[IModel], projectCtx: IProjectCtx) = {
    ModelProviderRegistry.findProvider(rule.rutimeModelClass, projectCtx.getClass) match {
      case Some(provider) =>
        val model = provider create projectCtx
        val result = Try(rule transform model) match {
          case Success(newModel) => newModel
          case Failure(e) => logger.error("Failed execute rule {} on model {}, {}", rule.id, model, e)
            throw e
        }
        provider save result
      case None => logger.error("Cannot find provider for {} used in rule {}", rule.rutimeModelClass, rule.id)
    }
  }

}
