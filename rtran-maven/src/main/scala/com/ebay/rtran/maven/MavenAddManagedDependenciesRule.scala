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

package com.ebay.rtran.maven

import com.ebay.rtran.maven.util.MavenModelUtil
import MavenModelUtil.{SimpleDependency, _}
import com.typesafe.scalalogging.LazyLogging
import org.apache.maven.model.DependencyManagement
import com.ebay.rtran.api.{IProjectCtx, IRule, IRuleConfig}

import scala.collection.JavaConversions._


class MavenAddManagedDependenciesRule(ruleConfig: MavenAddManagedDependenciesRuleConfig)
  extends IRule[MultiModuleMavenModel] with LazyLogging {

  override def transform(model: MultiModuleMavenModel): MultiModuleMavenModel = {
    val altered = model.parents map { parent =>
      val dependencyManagement = Option(parent.pomModel.getDependencyManagement) getOrElse new DependencyManagement
      ruleConfig.dependencies filterNot {dep =>
        dependencyManagement.getDependencies.exists(_.key == dep.key)
      } foreach {dep =>
        dependencyManagement.addDependency(dep)
        logger.info("{} added managed dependency {} to {}", id, dep, parent.pomFile)
      }

      parent.pomModel.setDependencyManagement(dependencyManagement)
      parent
    }
    logger.info("Rule {} was applied to {} files", id, altered.size.toString)
    model.copy(modules = model.subModules ++ altered)
  }

  override def isEligibleFor(projectCtx: IProjectCtx) = projectCtx.isInstanceOf[MavenProjectCtx]
}

case class MavenAddManagedDependenciesRuleConfig(dependencies: Set[SimpleDependency]) extends IRuleConfig