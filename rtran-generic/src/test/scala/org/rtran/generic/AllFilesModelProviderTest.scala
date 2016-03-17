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

import org.scalatest.{FlatSpecLike, Matchers}


class AllFilesModelProviderTest extends FlatSpecLike with Matchers {

  val projectRoot = new File(getClass.getClassLoader.getResource("someproject").getFile)

  "AllFilesModelProvider" should "list all the files under the project root" in {
    val projectCtx = new GenericProjectCtx(projectRoot)
    val provider = new AllFilesModelProvider
    val model = provider create projectCtx
    model.files.size should not be 0
  }

}
