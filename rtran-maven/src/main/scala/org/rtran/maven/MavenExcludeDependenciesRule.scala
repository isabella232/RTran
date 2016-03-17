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

import org.rtran.maven.util.{MavenUtil, MavenModelUtil}
import MavenModelUtil._
import MavenUtil._
import com.typesafe.scalalogging.LazyLogging
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter
import org.eclipse.aether.{graph => aether}
import org.rtran.api.{IProjectCtx, IRule, IRuleConfig}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}


class MavenExcludeDependenciesRule(ruleConfig: MavenExcludeDependenciesRuleConfig)
  extends IRule[MultiModuleMavenModel] with LazyLogging {

  override def transform(model: MultiModuleMavenModel): MultiModuleMavenModel = {
    var changes = Set.empty[File]
    val modules = model.modules map { module =>
      implicit val props = module.properties
      val managedDependencies = module.managedDependencies.values.map(mavenDependency2AetherDependency).toList
      // exclude from dependencyManagement
      Option(module.pomModel.getDependencyManagement).map(_.getDependencies.toList) getOrElse List.empty foreach {md =>
        val exclusions = ruleConfig.exclusions filter { exclusion =>
          getTransitiveDependencies(resolve(md), managedDependencies)
            .exists(d => d.getGroupId == exclusion.groupId && d.getArtifactId == exclusion.artifactId)
        }
        if (exclusions.nonEmpty) {
          changes += module.pomFile
          logger.info("{} excluded {} from {} in {}", id, exclusions, md, module.pomFile)
        }
        exclusions foreach (md.addExclusion(_))
      }
      // exclude from the dependencies that has explicit version
      module.pomModel.getDependencies.filter(dep => Option(dep.getVersion).nonEmpty) foreach {dep =>
        val exclusions = ruleConfig.exclusions filter { exclusion =>
          getTransitiveDependencies(resolve(dep), managedDependencies)
            .exists(d => d.getGroupId == exclusion.groupId && d.getArtifactId == exclusion.artifactId)
        }
        if (exclusions.nonEmpty) {
          changes += module.pomFile
          logger.info("{} excluded {} from {} in {}", id, exclusions, dep, module.pomFile)
        }
        exclusions foreach (dep.addExclusion(_))
      }
      module
    }
    logger.info("Rule {} was applied to {} files", id, changes.size.toString)
    model.copy(modules = modules)
  }

  private def getTransitiveDependencies(dependency: Dependency, managedDependencies: List[aether.Dependency]) = Try {
    MavenUtil.allDependencies(
      dependency,
      managedDependencies,
      dependencyFilter = new ExclusionsDependencyFilter(
        dependency.getExclusions.map(e => s"${e.getGroupId}:${e.getArtifactId}")
      )
    )
  } match {
    case Success(l) => l
    case Failure(e) => new java.util.ArrayList[Artifact]
  }

  override def isEligibleFor(projectCtx: IProjectCtx) = projectCtx.isInstanceOf[MavenProjectCtx]
}

case class MavenExcludeDependenciesRuleConfig(exclusions: Set[SimpleExclusion]) extends IRuleConfig

case class SimpleExclusion(groupId: String, artifactId: String)
