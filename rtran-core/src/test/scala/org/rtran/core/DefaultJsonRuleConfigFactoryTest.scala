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

import org.json4s.jackson.JsonMethods._
import org.rtran.api.IRuleConfig
import org.scalatest.{FlatSpecLike, Matchers}


class DefaultJsonRuleConfigFactoryTest extends FlatSpecLike with Matchers {

  case class Person(name: String, age: Int) extends IRuleConfig

  "DefaultJsonRuleConfigFactory" should "create config object from JSON" in {
    val json = asJsonNode(parse(
      """
        |{
        | "name":"abc",
        | "age":30
        |}
      """.stripMargin
    ))
    val p = DefaultJsonRuleConfigFactory.createRuleConfig(classOf[Person], json)
    p.name should be ("abc")
    p.age should be (30)
  }

}
