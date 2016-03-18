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

import com.typesafe.scalalogging.LazyLogging
import com.ebay.rtran.api.IRule

import scala.collection.JavaConversions._


class MavenRemoveRepositoriesRule(ruleConfig: MavenRemoveRepositoriesRuleConfig)
  extends IRule[MultiModuleMavenModel] with LazyLogging {

  override def transform(model: MultiModuleMavenModel): MultiModuleMavenModel = {
    val modules = model.modules map { module =>
      val toBeRemoved = for {
        repo <- module.pomModel.getRepositories
        pattern <- ruleConfig.repoPatterns
        if repo.getUrl matches pattern
      } yield repo

      toBeRemoved foreach module.pomModel.removeRepository
      if (toBeRemoved.nonEmpty) logger.info("Rule {} was applied to 1 files", id)

      module
    }
    model.copy(modules = modules)
  }
}

case class MavenRemoveRepositoriesRuleConfig(repoPatterns: Set[String])
