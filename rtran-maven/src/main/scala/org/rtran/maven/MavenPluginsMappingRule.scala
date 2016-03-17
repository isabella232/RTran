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

package org.rtran.maven

import org.rtran.api.{IRule, IRuleConfig}

import scala.collection.JavaConversions._
import scala.util.Try


class MavenPluginsMappingRule(ruleConfig: MavenPluginsMappingRuleConfig)
  extends IRule[MultiModuleMavenModel] {

  override def transform(model: MultiModuleMavenModel): MultiModuleMavenModel = {
    val modules = model.modules map { module =>
      (for {
        p <- Try(module.pomModel.getBuild.getPlugins.toList) getOrElse List.empty
        mappings <- ruleConfig.mappings
        if mappings.from matches p
      } yield {
        p -> mappings.to
      }) foreach {
        case (p, to) =>
          to.groupId foreach p.setGroupId
          p.setArtifactId(to.artifactId)
          to.version foreach p.setVersion
      }

      (for {
        p <- Try(module.pomModel.getBuild.getPluginManagement.getPlugins.toList) getOrElse List.empty
        mappings <- ruleConfig.mappings
        if mappings.from matches p
      } yield {
          p -> mappings.to
        }) foreach {
        case (p, to) =>
          to.groupId foreach p.setGroupId
          p.setArtifactId(to.artifactId)
          to.version foreach p.setVersion
      }

      module
    }
    model.copy(modules = modules)
  }
}

case class MavenPluginsMappingRuleConfig(mappings: List[PluginMapping]) extends IRuleConfig
case class PluginMapping(from: SimplePlugin, to: SimplePlugin)