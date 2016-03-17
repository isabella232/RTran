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

import org.scalatest.{FlatSpecLike, Matchers}


class RuleRegistryTest extends FlatSpecLike with Matchers {

  "RuleRegistry" should "register rule definitions" in {
    RuleRegistry.rules.size should not be 0
  }

  "RuleRegistry" should "get rule class according to the id" in {
    RuleRegistry.findRuleDefinition("ModifyFileRule").nonEmpty should be (true)
    RuleRegistry.findRuleDefinition("RenameFileRule").nonEmpty should be (true)
    RuleRegistry.findRuleDefinition("non-exist").nonEmpty should be (false)
  }

  "RuleRegistry" should "check rule class existence according to the id" in {
    RuleRegistry.hasRule("ModifyFileRule") should be (true)
    RuleRegistry.hasRule("RenameFileRule") should be (true)
    RuleRegistry.hasRule("non-exist") should be (false)
  }
}
