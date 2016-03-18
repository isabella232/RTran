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

package com.ebay.rtran.generic

import org.apache.commons.io.FileUtils
import com.ebay.rtran._
import com.ebay.rtran.api.{IRule, IRuleConfig}
import com.ebay.rtran.generic.util.{EncodingDetector, FilePathMatcher}

class ModifyFilesRule(ruleConfig: ModifyFilesRuleConfig) extends IRule[AllFilesModel] {

  override def transform(model: AllFilesModel): AllFilesModel = {
    val modified = model.files filter {file =>
      FilePathMatcher(model.projectRoot, ruleConfig.pathPattern).map(_ matches file) getOrElse false
    } map {file =>
      val content = ruleConfig.encoding map (encoding => FileUtils.readFileToString(file, encoding)) getOrElse {
        val (encoding, bytes) = EncodingDetector.guessEncoding(file)
        new String(bytes, encoding)
      }
      val newContent = ruleConfig.contentMappings.foldLeft(content) {(c, contentMapping) =>
        contentMapping match {
          case ContentMapping(regex, replacement, false) => c.replaceAll(regex, replacement)
          case ContentMapping(regex, replacement, true) => c.replaceFirst(regex, replacement)
        }
      }
      if (content != newContent) {
        FileUtils.write(file, newContent, false)
        Some(file)
      } else None
    } collect {
      case Some(f) => f
    }
    model.copy(modified = modified)
  }

}

case class ModifyFilesRuleConfig(pathPattern: String,
                                 encoding: Option[String],
                                 contentMappings: List[ContentMapping]) extends IRuleConfig

case class ContentMapping(regex: String, replacement: String, firstOnly: Boolean = false)