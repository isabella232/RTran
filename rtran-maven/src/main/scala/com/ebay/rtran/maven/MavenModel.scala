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

import java.io._

import com.ebay.rtran.api.{IModel, IModelProvider}
import com.ebay.rtran.maven.util.MavenModelUtil._
import com.ebay.rtran.maven.util.MavenUtil
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.maven.model.io.jdom.MavenJDOMWriter
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.model.{Dependency, Model, Plugin}
import org.eclipse.aether.artifact.DefaultArtifact
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.Format.TextMode

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


case class MultiModuleMavenModel(modules: List[MavenModel]) extends IModel {
  lazy val (parents, subModules) = modules.partition(_.localParent.isEmpty)
}

case class MavenModel(pomFile: File, pomModel: Model) {

  def localParent = Option(pomModel.getParent) flatMap { p =>
    Try {
      val relativePomPath = {
        if (p.getRelativePath.endsWith("pom.xml")) p.getRelativePath
        else p.getRelativePath.stripPrefix("/") + "/pom.xml"
      }
      val localParentFile = new File(pomFile.getParent, relativePomPath).getCanonicalFile
      val model = new MavenXpp3Reader().read(new FileReader(localParentFile))
      MavenModel(localParentFile, model)
    } match {
      case Success(m) if m.pomModel.getArtifactId == p.getArtifactId => Some(m)
      case _ => None
    }
  }

  def parent: Option[MavenModel] = localParent orElse {
    Option(pomModel.getParent) map {p =>
      val artifact = MavenUtil.resolveArtifact(
        new DefaultArtifact(s"${p.getGroupId}:${p.getArtifactId}:pom:${p.getVersion}")
      )
      val model = new MavenXpp3Reader().read(new FileReader(artifact.getFile))
      MavenModel(artifact.getFile, model)
    }
  }

  def resolvedDependencies: List[Dependency] = {
    pomModel.getDependencies.map(resolve) map { dep =>
      managedDependencies get dep.getManagementKey match {
        case None => dep
        case Some(md) => merge(md, dep)
      }
    } toList
  }

  def managedDependencies: Map[String, Dependency] = {
    parent.map(_.managedDependencies).getOrElse(Map.empty) ++
      Option(pomModel.getDependencyManagement)
        .map(_.getDependencies.map(resolve).map(dep => dep.getManagementKey -> dep).toMap)
        .getOrElse(Map.empty)
  }

  def managedPlugins: Map[String, Plugin] = {
    parent.map(_.managedPlugins).getOrElse(Map.empty) ++
      Try(pomModel.getBuild.getPluginManagement)
        .map(_.getPlugins.map(resolve).map(plugin => plugin.getKey -> plugin).toMap)
        .getOrElse(Map.empty)
  }

  def resolvedPlugins: List[Plugin] = {
    Option(pomModel.getBuild).map(_.getPlugins.map(resolve)).getOrElse(List.empty) map {plugin =>
      managedPlugins get plugin.getKey match {
        case None => plugin
        case Some(mp) => merge(mp, plugin)
      }
    } toList
  }

  implicit def properties: Map[String, String] = parent.map(_.properties).getOrElse(Map.empty) ++ pomModel.getProperties

}

class MultiModuleMavenModelProvider extends IModelProvider[MultiModuleMavenModel, MavenProjectCtx] {

  private val CompilerArgumentsPattern = """(?s)<configuration.*?>(.*?)</configuration>""".r

  private val ArgumentsPattern = """(</?.*?)(:)(.*?/?>)""".r

  private val ArgumentsBackPattern = """(</?.*?)(_colon_)(.*?/?>)""".r

  private val defaultEncoding = "UTF-8"

  override def id(): String = getClass.getName

  override def save(model: MultiModuleMavenModel): Unit = {
    model.modules foreach { module =>
      val encoding = Option(module.pomModel.getModelEncoding) getOrElse defaultEncoding
      val content = fixContent(FileUtils.readFileToString(module.pomFile, encoding))
      val builder = new SAXBuilder
      builder.setIgnoringBoundaryWhitespace(false)
      builder.setIgnoringElementContentWhitespace(false)
      val doc = builder.build(new StringReader(content))

      // guess the line separator
      val separator = if (content.contains(IOUtils.LINE_SEPARATOR_WINDOWS)) IOUtils.LINE_SEPARATOR_WINDOWS else IOUtils.LINE_SEPARATOR_UNIX

      val format = Format.getRawFormat.setEncoding(encoding).setTextMode(TextMode.PRESERVE).setLineSeparator(separator)
      val outWriter = new StringWriter()
      new MavenJDOMWriter().setExpandEmptyElements(true).write(module.pomModel, doc, outWriter, format)

      val updatedContent = outWriter.toString

      FileUtils.write(module.pomFile, fixBack(updatedContent), encoding)
    }
  }

  // replace element like Xlint:-path to Xlint_colon_-path, to pass the validation of xml
  private def fixContent(content: String) = {
    CompilerArgumentsPattern.replaceAllIn(content, {matcher =>
      ArgumentsPattern.replaceAllIn(matcher.matched, "$1_colon_$3")
    })
  }

  // replace _colon_ back to :
  private def fixBack(content: String) = {
    CompilerArgumentsPattern.replaceAllIn(content, {matcher =>
      ArgumentsBackPattern.replaceAllIn(matcher.matched, "$1:$3")
    })
  }

  override def create(projectCtx: MavenProjectCtx): MultiModuleMavenModel = {

    def findModules(root: MavenModel): List[MavenModel] =  {
      val modules = root.pomModel.getModules ++ root.pomModel.getProfiles.flatMap(profile => profile.getModules) toSet

      modules.foldLeft(List.empty[MavenModel]) {(list, module) =>
        list ++ (createMavenModel(new File(root.pomFile.getParent, s"$module/pom.xml")) map { m =>
          m :: findModules(m)
        } getOrElse List.empty)
      }
    }

    def createMavenModel(pomFile: File) = Try {
      val pomModel = new MavenXpp3Reader().read(new StringReader(fixContent(FileUtils.readFileToString(pomFile, defaultEncoding))))
      MavenModel(pomFile.getCanonicalFile, pomModel)
    }

    createMavenModel(projectCtx.rootPomFile) match {
      case Success(root) =>
        val modules = root :: findModules(root)
        val keys = modules.map(_.pomFile).toSet
        val unprocessedParents = modules.map(_.localParent) collect {
          case Some(m) => m
        } filter {m =>
          !keys.contains(m.pomFile)
        }
        MultiModuleMavenModel(unprocessedParents ++ modules)
      case Failure(e) => throw e
    }
  }
}
