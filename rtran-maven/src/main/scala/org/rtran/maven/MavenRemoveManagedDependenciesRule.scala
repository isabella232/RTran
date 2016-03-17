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

import org.rtran.maven.util.MavenModelUtil
import MavenModelUtil.SimpleDependency
import com.typesafe.scalalogging.LazyLogging
import org.rtran.api.{IRule, IRuleConfig}

import scala.collection.JavaConversions._
import scala.util.Try


class MavenRemoveManagedDependenciesRule(ruleConfig: MavenRemoveManagedDependenciesRuleConfig)
  extends IRule[MultiModuleMavenModel] with LazyLogging {

  override def transform(model: MultiModuleMavenModel): MultiModuleMavenModel = {
    var changes = Set.empty[File]
    val modules = model.modules map { module =>
      val matches = for {
        md <- Try(module.pomModel.getDependencyManagement.getDependencies.toList) getOrElse List.empty
        resolvedMd <- module.managedDependencies.values
        toBeRemoved <- ruleConfig.dependencies
        if (toBeRemoved matches resolvedMd) && (md.getManagementKey == resolvedMd.getManagementKey)
      } yield md

      Option(module.pomModel.getDependencyManagement) foreach { dm =>
        matches foreach dm.removeDependency
        changes += module.pomFile
      }
      module
    }
    logger.info("Rule {} was applied to {} files", id, changes.size.toString)
    model.copy(modules = modules)
  }
}

case class MavenRemoveManagedDependenciesRuleConfig(dependencies: Set[SimpleDependency]) extends IRuleConfig
