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
import com.ebay.rtran.api.{IModel, IRule, IRuleConfigFactory}

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


object RuleRegistry extends LazyLogging {

  val PATH_TO_RULES = "upgrader.rules"

  private[this] lazy val ruleDefinitions = loadRuleDefinitions(UpgraderMeta.configs)

  def findRuleDefinition(name: String) = ruleDefinitions get name

  def hasRule(name: String) = ruleDefinitions contains name

  def rules = ruleDefinitions.keySet

  private def loadRuleDefinitions(configs: Iterator[Config]) = {
    var definitions = Map.empty[String, (Class[_ <: IRule[IModel]], Option[IRuleConfigFactory[_]])]
    configs.filter(_.hasPath(PATH_TO_RULES)).map(_.getConfig(PATH_TO_RULES)) foreach {config =>
      config.entrySet.map(_.getKey.split("\\.")(0)).toSet[String] foreach { key =>
        if (definitions contains key) {
          logger.warn("Definition of rule: {} already exists to {}", key, definitions(key))
        } else {
          loadRuleDefinition(Try(config.getConfig(key))) match {
            case Success(definition) => definitions += key -> definition
            case Failure(e) => logger.error("Failed to get rule class binding for {}, {}", key, e)
          }
        }
      }
    }
    definitions
  }

  private def loadRuleDefinition(ruleDef: Try[Config]) = ruleDef map {c =>
    val ruleClass = Class.forName(c.getString("class")).asSubclass(classOf[IRule[IModel]])
    val configFactory = Try(c.getString("config-factory")) map {className =>
      Class.forName(className).asSubclass(classOf[IRuleConfigFactory[_]]).newInstance
    } toOption

    (ruleClass, configFactory)
  }

}
