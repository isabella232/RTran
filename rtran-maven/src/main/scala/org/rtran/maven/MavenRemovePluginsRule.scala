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

import java.io.File

import com.typesafe.scalalogging.LazyLogging
import org.apache.maven.model.Plugin
import org.rtran.api.{IRule, IRuleConfig}

import scala.collection.JavaConversions._
import scala.util.Try


class MavenRemovePluginsRule(ruleConfig: MavenRemoveManagedPluginsRuleConfig)
  extends IRule[MultiModuleMavenModel] with LazyLogging {

  override def transform(model: MultiModuleMavenModel): MultiModuleMavenModel = {
    var changes = Set.empty[File]
    val modules = model.modules map { module =>
      val managedPlugins = for {
        mp <- Try(module.pomModel.getBuild.getPluginManagement.getPlugins.toList) getOrElse List.empty
        toBeRemoved <- ruleConfig.plugins
        if toBeRemoved matches mp
      } yield mp

      Try(module.pomModel.getBuild.getPluginManagement) foreach { pm =>
        managedPlugins foreach pm.removePlugin
        changes += module.pomFile
      }

      val plugins = for {
        p <- Try(module.pomModel.getBuild.getPlugins.toList) getOrElse List.empty
        toBeRemoved <- ruleConfig.plugins
        if toBeRemoved matches p
      } yield p

      Try(module.pomModel.getBuild) foreach { b =>
        plugins foreach b.removePlugin
        changes += module.pomFile
      }
      module
    }
    logger.info("Rule {} was applied to {} files", id, changes.size.toString)
    model.copy(modules = modules)
  }
}

case class MavenRemoveManagedPluginsRuleConfig(plugins: Set[SimplePlugin]) extends IRuleConfig

case class SimplePlugin(groupId: Option[String] = None, artifactId: String, version: Option[String] = None) {
  def matches(plugin: Plugin): Boolean = {
    groupId.getOrElse(plugin.getGroupId) == plugin.getGroupId &&
      artifactId == plugin.getArtifactId &&
      version.getOrElse(plugin.getVersion) == plugin.getVersion
  }
}
