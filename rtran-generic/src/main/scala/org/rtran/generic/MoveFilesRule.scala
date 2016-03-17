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

package org.rtran.generic

import java.io.File

import org.rtran._
import org.apache.commons.io.FileUtils
import org.rtran.api.{IRule, IRuleConfig}
import org.rtran.generic.util.FilePathMatcher


class MoveFilesRule(ruleConfig: MoveFilesRuleConfig) extends IRule[AllFilesModel] {

  override def transform(model: AllFilesModel): AllFilesModel = {
    val result = ruleConfig.moves.foldLeft(model.files) {(files, move) =>
      val removes = files filter { file =>
        FilePathMatcher(model.projectRoot, move.pathPattern).map(_ matches file).getOrElse(false)
      }
      val dest = new File(model.projectRoot, move.destDir)
      val creates = removes map {f =>
        FileUtils.moveFileToDirectory(f, dest, true)
        new File(dest, f.getName)
      }
      files diff removes ++ creates
    }
    model.copy(files = result)
  }
}

case class MoveFilesRuleConfig(moves: List[Move]) extends IRuleConfig

case class Move(pathPattern: String, destDir: String)
