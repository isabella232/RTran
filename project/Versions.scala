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

import scala.xml.{Node, Elem}
import scala.xml.transform.{RuleTransformer, RewriteRule}

object Versions {

  val slf4jVersion = "1.7.19"
  val logbackVersion = "1.1.6"
  val scalatestVersion = "2.2.5"
  val typesafeConfigVersion = "1.3.0"
  val apacheCommonsIOVersion = "2.4"
  val scalaLoggingVersion = "3.1.0"
  val json4sVersion = "3.2.11"
}

object FilterBadDependency extends RewriteRule {

  override def transform(n: Node): Seq[Node] = n match {
    /**
      * When we find the dependencies node we want to rewrite it removing any of
      * the scoverage dependencies.
      */
    case dependencies @ Elem(_, "dependencies", _, _, _*) =>
      <dependencies>
        {
          dependencies.child filter { dep =>
            (dep \ "groupId").text != "org.scoverage"
          }
        }
      </dependencies>
    /**
      * Otherwise we just skip over the node and do nothing
      */
    case other => other
  }

}

object TransformFilterBadDependencies extends RuleTransformer(FilterBadDependency)
