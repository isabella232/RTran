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

package org.rtran.core

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.scalatest.{FlatSpecLike, Matchers}


class JsonUpgradeConfigurationTest extends FlatSpecLike with Matchers {

  implicit val formats = DefaultFormats

  "UpgradeConfiguration" should "be constructed from JSON string with projectType and rules defined" in {
    val jsonString =
      """
        |{
        | "ruleConfigs":[
        |   {"name":"MyModifyFileRule"},
        |   {"name":"MyRenameFileRule"}
        | ]
        |}
      """.stripMargin
    val configuration = parse(jsonString).extract[JsonUpgradeConfiguration]
    configuration.ruleConfigs should be (List(
      JsonRuleConfiguration("MyModifyFileRule", None),
      JsonRuleConfiguration("MyRenameFileRule", None)
    ))
  }

  "UpgradeConfiguration" should "be constructed from JSON string with all fields defined" in {
    val jsonString =
      """
        |{
        | "ruleConfigs":[
        |   {
        |     "name":"MyModifyFileRule",
        |     "config":{
        |       "key1":"value1"
        |     }
        |   },
        |   {
        |     "name":"MyRenameFileRule",
        |     "config":[
        |       "value1",
        |       "value2"
        |     ]
        |   },
        |   {
        |     "name":"MyRenameFileRule",
        |     "config":{
        |       "key3":{
        |         "key1":"value1"
        |       },
        |       "key4":"value4"
        |     }
        |   }
        | ]
        |}
      """.stripMargin
    val configuration = parse(jsonString).extract[JsonUpgradeConfiguration]
    configuration.ruleConfigs.size should be (3)
    configuration.ruleConfigs foreach (_.config should not be None)
  }

}
