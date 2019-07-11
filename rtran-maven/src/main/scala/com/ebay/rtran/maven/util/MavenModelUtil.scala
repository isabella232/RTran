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

package com.ebay.rtran.maven.util

import com.ebay.rtran.maven.SimpleExclusion
import org.apache.maven.model.{Dependency, Exclusion, Model, Plugin}
import org.eclipse.aether.util.version.GenericVersionScheme

import scala.collection.JavaConversions._
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.Try


object MavenModelUtil {

  implicit class ModelAddon(model: Model) {

    def removePropertyByValue[T](value: T)(onRemove: ((String, String)) => Unit = (_) => {}) = {
      model.getProperties find {
        case (k, v) => v == value
      } foreach { prop =>
        model.getProperties.remove(prop._1)
        onRemove(prop)
      }
    }

    def parentKey = Option(model.getParent).map(p => s"${p.getGroupId}:${p.getArtifactId}:${p.getVersion}") getOrElse ""
  }



  def resolve[T](obj: T)(implicit properties: Map[String, String], classTag: ClassTag[T]): T = {
    val ExtractPattern = """\$\{(.+)\}""".r
    val clazz = classTag.runtimeClass
    val result = clazz.getMethod("clone").invoke(obj).asInstanceOf[T]
    val getters = clazz.getDeclaredMethods filter { method =>
      method.getName.startsWith("get") && method.getParameterCount == 0 && method.getReturnType == classOf[String]
    }

    def replaceVariable(s: String, properties: Map[String, String]): String = {
      Option(s) match {
        case Some(v) =>
          var replaced = s
          ExtractPattern.findAllMatchIn(s) foreach { mat =>
            replaced = s.replace(mat.matched, properties.get(mat.group(1)).getOrElse(""))
          }
          replaced

        case _ => ""
      }
    }
    getters foreach {getter =>

      //FIX: should replace all variables. Case "a.b.c-${scala.version}" was not working
      val v = getter.invoke(obj, getter.getParameterTypes: _*).asInstanceOf[String]
      val newV = replaceVariable(v, properties)
      if (Option(v).getOrElse("") != newV) {
        val setter = Try(clazz.getDeclaredMethod(getter.getName.replaceFirst("get", "set"), getter.getReturnType))
        setter.foreach(_.invoke(result, newV))
      }


//      getter.invoke(obj, getter.getParameterTypes:_*).asInstanceOf[String] match {
//        case ExtractPattern(key) =>
//          properties.get(key) foreach {value =>
//            val setter = Try(clazz.getDeclaredMethod(getter.getName.replaceFirst("get", "set"), getter.getReturnType))
//            setter.foreach(_.invoke(result, value))
//          }
//        case other =>
//      }
    }
    result
  }

  def merge[T](base: T, target: T)(implicit classTag: ClassTag[T]): T = {
    val clazz = classTag.runtimeClass
    val result = clazz.getMethod("clone").invoke(base).asInstanceOf[T]
    val getters = clazz.getDeclaredMethods filter { method =>
      method.getName.startsWith("get") && method.getParameterCount == 0
    }
    getters foreach {getter =>
      val value = getter.invoke(target, getter.getParameterTypes:_*)
      if (Option(value).nonEmpty) {
        val setter = Try(clazz.getDeclaredMethod(getter.getName.replaceFirst("get", "set"), getter.getReturnType))
        setter.foreach(_.invoke(result, value))
      }
    }
    result
  }

  case class SimpleDependency(groupId: String,
                              artifactId: String,
                              version: Option[String] = None,
                              classifier: Option[String] = None,
                              `type`: Option[String] = None,
                              scope: Option[String] = None) {

    lazy val key = (groupId, artifactId)

    def matches(dependency: Dependency): Boolean = {
      def compareOptionalField(opt: Option[String], dest: String) = (opt, Option(dest)) match {
        case (None, _) => true
        case (Some(_), None) => false
        case (Some(src), Some(d)) => src == d
      }
      groupId == dependency.getGroupId &&
        artifactId == dependency.getArtifactId  &&
        compareOptionalField(version, dependency.getVersion) &&
        compareOptionalField(classifier, dependency.getClassifier) &&
        compareOptionalField(`type`, dependency.getType) &&
        compareOptionalField(scope, dependency.getScope)
    }
  }

  implicit class DependencyAddon(dependency: Dependency) {
    def merge(dep: SimpleDependency): Dependency = {
      if (dependency.key == dep.key) {
        dependency.setVersion(dep.version getOrElse dependency.getVersion)
        dependency.setClassifier(dep.classifier getOrElse dependency.getClassifier)
        dependency.setType(dep.`type` getOrElse dependency.getType)
        dependency.setScope(dep.scope getOrElse dependency.getScope)
        dependency
      } else dependency
    }
  }

  implicit def simpleDependency2MavenDependency(dep: SimpleDependency): Dependency = {
    val d = new Dependency
    d setGroupId dep.groupId
    d setArtifactId dep.artifactId
    dep.version foreach d.setVersion
    dep.classifier foreach d.setClassifier
    dep.`type` foreach d.setType
    dep.scope foreach d.setScope
    d
  }

  implicit def mavenDependency2SimpleDependency(dep: Dependency): SimpleDependency = SimpleDependency(
    dep.getGroupId,
    dep.getArtifactId,
    version = Option(dep.getVersion),
    classifier = Option(dep.getClassifier),
    `type` = Option(dep.getType),
    scope = Option(dep.getScope)
  )

  implicit def simpleExclusion2MavenExclusion(exclusion: SimpleExclusion): Exclusion = {
    val result = new Exclusion
    result.setGroupId(exclusion.groupId)
    result.setArtifactId(exclusion.artifactId)
    result
  }

  case class SimpleArtifact(groupId: String, artifactId: String, version: String)

  implicit def dependency2SimpleArtifact(dep: Dependency): SimpleArtifact =
    SimpleArtifact(dep.getGroupId, dep.getArtifactId, dep.getVersion)
  implicit def plugin2SimpleArtifact(plugin: Plugin): SimpleArtifact =
    SimpleArtifact(plugin.getGroupId, plugin.getArtifactId, plugin.getVersion)

  def compareVersions(versionL: String, versionR: String) = {
    val versionScheme = new GenericVersionScheme
    val lv = versionScheme.parseVersion(versionL)
    val rv = versionScheme.parseVersion(versionR)
    lv compareTo rv
  }

  def latestVersion(groupId: String, artifactId: String, versionPrefix: Option[String], snapshot: Boolean): String = {
    val allowSnapshot = Try(System.getProperty("allowSnapshot").toBoolean) getOrElse snapshot
    val versions = MavenUtil.findAvailableVersions(groupId, artifactId, versionPrefix.orNull, allowSnapshot)
    versions.lastOption match {
      case Some(v) => v
      case None => throw new IllegalStateException(s"Cannot find version for $groupId, $artifactId, $versionPrefix")
    }
  }

  implicit class VersionAddon(version: String) {
    // check the version format
    new GenericVersionScheme().parseVersion(version)

    lazy val minorPrefix = version.split("\\.").take(2).mkString(".")
    lazy val majorPrefix = version.split("\\.").take(1).mkString(".")
  }
}
