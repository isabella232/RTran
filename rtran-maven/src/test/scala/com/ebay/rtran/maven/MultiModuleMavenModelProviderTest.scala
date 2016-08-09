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

import java.io.{File, FileReader}

import org.apache.commons.io.FileUtils
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.codehaus.plexus.util.xml.Xpp3Dom
import org.scalatest.{FlatSpecLike, Matchers}

import collection.JavaConversions._


class MultiModuleMavenModelProviderTest extends FlatSpecLike with Matchers {

  val projectRoot = new File(getClass.getClassLoader.getResource("mvnproject").getFile)

  "MavenModelProvider" should "resolve all the pom files in the project" in {
    val projectCtx = new MavenProjectCtx(projectRoot)
    val provider = new MultiModuleMavenModelProvider
    val model = provider.create(projectCtx)

    model.modules foreach { m =>
      m.resolvedDependencies foreach {dep =>
        Option(dep.getVersion) should not be None
      }
    }
  }

  "MavenModelProvider" should "resolve all the pom files recursively in the project" in {
    val dir = new File(getClass.getClassLoader.getResource("recursive").getFile)
    val projectCtx = new MavenProjectCtx(dir)
    val provider = new MultiModuleMavenModelProvider
    val model = provider.create(projectCtx)
    model.modules.size should be (5)
  }

  "MavenModelProvider" should "not remove empty property nodes" in {
    val dir = new File(projectRoot.getParent, projectRoot.getName + "-bak")
    FileUtils.deleteQuietly(dir)
    FileUtils.copyDirectory(projectRoot, dir)
    val projectCtx = new MavenProjectCtx(dir)
    val provider = new MultiModuleMavenModelProvider
    val model = provider.create(projectCtx)

    provider save model

    val pom = new MavenXpp3Reader().read(new FileReader(new File(dir, "pom.xml")))
    pom.getProperties.getProperty("empty.property1") should be ("")
    pom.getProperties.getProperty("empty.property2") should be ("")
    pom.getProperties.getProperty("empty.property3") should be ("")
  }

  "MavenModelProvider" should "not break on xlint element" in {
    val dir = new File(projectRoot.getParent, projectRoot.getName + "-bak")
    FileUtils.deleteQuietly(dir)
    FileUtils.copyDirectory(projectRoot, dir)
    val projectCtx = new MavenProjectCtx(dir)
    val provider = new MultiModuleMavenModelProvider
    val model = provider.create(projectCtx)

    for {
      root <- model.parents.headOption
      build <- Option(root.pomModel.getBuild)
      sourcePlugin <- build.getPlugins.find(_.getArtifactId == "some-maven-plugin")
    } {
      build.removePlugin(sourcePlugin)
    }
    provider save model

    val pom = new MavenXpp3Reader().read(new FileReader(new File(dir, "pom.xml")))
    pom.getBuild.getPlugins.size() should be(1)
    val plugin = pom.getBuild.getPlugins.find(_.getArtifactId == "maven-source-plugin")
    plugin shouldNot be(None)
    plugin.map(_.getConfiguration.asInstanceOf[Xpp3Dom].getChild("compilerArguments").getChildCount) should be(Some(3))

  }

}
