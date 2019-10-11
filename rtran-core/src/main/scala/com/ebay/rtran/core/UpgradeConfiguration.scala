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

import com.fasterxml.jackson.databind.JsonNode
import com.typesafe.scalalogging.LazyLogging
import org.json4s.JsonAST.JValue
import org.json4s.jackson.JsonMethods._
import com.ebay.rtran.api.{IModel, IRule, IRuleConfigFactory}
import org.json4s.DefaultFormats

import scala.util.{Failure, Success, Try}


trait RuleProducer {
  val ruleInstances: List[_ <: IRule[_ <: IModel]]
}

trait UpgradeConfiguration extends RuleProducer {
  val ruleConfigs: List[JsonRuleConfiguration]
}

case class JsonRuleConfiguration(name: String, metadata: Option[JValue] = None, config: Option[JValue] = None)

case class JsonUpgradeConfiguration(ruleConfigs: List[JsonRuleConfiguration])
  extends UpgradeConfiguration with JsonRuleProducer

trait JsonRuleProducer extends RuleProducer with LazyLogging {self: UpgradeConfiguration =>

  lazy val ruleInstances = ruleConfigs map {
    case JsonRuleConfiguration(name, metadata, configOpt) =>
      logger.info("Creating instance for {} with config {}", name, configOpt)
      implicit val formats = DefaultFormats

      //copy settings from metadata to Rule Registry
      RuleRegistry.findRuleDefinition(name) flatMap { case (ruleClass, rule) =>
        val properties = metadata.map(json => json.extract[Map[String, Any]])
        properties.map(m => m.mapValues(_.toString)).map(m => rule.additionalProperties ++= m)

        val configFactory = (rule.configFactory getOrElse DefaultJsonRuleConfigFactory)
          .asInstanceOf[IRuleConfigFactory[JsonNode]]
        configOpt map { config =>
          Try(JsonConfigurableRuleFactory.createRuleWithConfig(ruleClass, configFactory, asJsonNode(config)))
        } getOrElse Try(JsonConfigurableRuleFactory.createRule(ruleClass)) match {
          case Success(instance) =>
            Some(instance)
          case Failure(e) =>
            logger.warn(e.getMessage)
            None
        }
      }
  } collect {
    case Some(instance) => instance
  }

}