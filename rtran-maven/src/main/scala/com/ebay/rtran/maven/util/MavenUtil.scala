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

import java.io.File
import java.util
import java.util.Collections

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.apache.maven.repository.internal._
import org.apache.maven.{model => maven}
import org.eclipse.aether.artifact.{Artifact, ArtifactProperties, DefaultArtifact, DefaultArtifactType}
import org.eclipse.aether.collection.{CollectRequest, DependencyCollectionException}
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph._
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.{LocalRepository, RemoteRepository}
import org.eclipse.aether.resolution.{ArtifactRequest, DependencyResolutionException, VersionRangeRequest}
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter
import org.eclipse.aether.util.graph.visitor.{FilteringDependencyVisitor, TreeDependencyVisitor}
import org.eclipse.aether.{RepositorySystem, RepositorySystemSession, repository => aether}

import scala.collection.JavaConversions._
import scala.io.Source
import scala.language.{implicitConversions, postfixOps}
import scala.util.Try

object MavenUtil {
  private lazy val config = ConfigFactory.load(getClass.getClassLoader).getConfig("maven.util")
  private val DEFAULT = "default"

  private lazy val DEFAULT_REPOSITORY_REMOTE = new RemoteRepository.Builder("central", DEFAULT,
    "http://repo1.maven.org/maven2").build()

  lazy val repositorySystem = {
    val locator = MavenRepositorySystemUtils.newServiceLocator
    locator.addService(classOf[RepositoryConnectorFactory], classOf[BasicRepositoryConnectorFactory])
    locator.addService(classOf[TransporterFactory], classOf[FileTransporterFactory])
    locator.addService(classOf[TransporterFactory], classOf[HttpTransporterFactory])
    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      override def serviceCreationFailed(`type`: Class[_], impl: Class[_], exception: Throwable) {
        exception.printStackTrace()
      }
    })
    locator.getService(classOf[RepositorySystem])
  }

  private lazy val remoteRepositories: List[RemoteRepository] = {
    val repositories = config.getConfig("remote-repositories")
    repositories.entrySet map {entry =>
      val key = entry.getKey
      val url = repositories.getString(entry.getKey)
      val (releasePolicy, snapshotPolicy) = if (url endsWith "snapshots") {
        (new aether.RepositoryPolicy(false, "daily", ""), new aether.RepositoryPolicy(true, "always", ""))
      } else {
        (new aether.RepositoryPolicy(true, "daily", ""), new aether.RepositoryPolicy(false, "always", ""))
      }
      if(key =="maven_central_mirror"){
        new RemoteRepository.Builder(key, DEFAULT, url)
          .setReleasePolicy(releasePolicy)
          .setSnapshotPolicy(snapshotPolicy)
          .setMirroredRepositories(List(DEFAULT_REPOSITORY_REMOTE))
          .build
      }else{
        new RemoteRepository.Builder(key, DEFAULT, url)
          .setReleasePolicy(releasePolicy)
          .setSnapshotPolicy(snapshotPolicy)
          .build
      }
    } toList
  }

  private lazy val remoteReleaseRepositories: List[RemoteRepository] =
    remoteRepositories.filter(_.getPolicy(false).isEnabled)

  private lazy val remoteSnapshotRepositories: List[RemoteRepository] =
    remoteRepositories.filter(_.getPolicy(true).isEnabled)

  private[maven] lazy val localRepository = new File(System.getProperty("user.home"), config.getString("local-repository"))

  def repositorySystemSession: RepositorySystemSession = {
    val session = MavenRepositorySystemUtils.newSession
    val localRepo = new LocalRepository(localRepository.getAbsolutePath)
    session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo))
    session
  }

  def getTransitiveDependencies(dependency: maven.Dependency,
                                managedDependencies: util.List[maven.Dependency] = List.empty[maven.Dependency],
                                enableCache: Boolean = true): util.List[Artifact] = {

    def filterCachedResults(artifacts: util.List[Artifact]): util.List[Artifact] ={
      val excls = dependency.getExclusions.map(e => s"${e.getGroupId}:${e.getArtifactId}").toSet
      artifacts.filterNot(a => excls.contains(s"${a.getGroupId}:${a.getArtifactId}"))
    }

    if (enableCache && !dependency.getVersion.contains("SNAPSHOT")) {
      val cacheDir = new File(localRepository, "cache")
      cacheDir.mkdirs()
      val cacheFile = new File(cacheDir, dependency.toString)
      Try {
        this.synchronized {
          val cachedResults: util.List[Artifact] =
            Source.fromFile(cacheFile).getLines().map(new DefaultArtifact(_)).toList
          filterCachedResults(cachedResults)
        }
      } getOrElse {
        Try {
          /**
            * should not cache exclusions result as applications upgrades are running parallel.
            * if one application already has exclusion and has the result cached, other application get affected.
            */
          val results = allDependencies(dependency, managedDependencies.map(mavenDependency2AetherDependency).toList, new EmptyDependencyFilter)
          if (results.find(_.getVersion.endsWith("SNAPSHOT")).isEmpty) {
            //the release artifact should not depend on any snapshot version
            this.synchronized {
              FileUtils.writeLines(cacheFile, results)
            }
          }
          filterCachedResults(results)
        } getOrElse util.Collections.emptyList[Artifact]
      }
    } else {
      val dependencyFilter = new ExclusionsDependencyFilter(
        dependency.getExclusions.map(e => s"${e.getGroupId}:${e.getArtifactId}")
      )

      Try {
        allDependencies(dependency, managedDependencies.map(mavenDependency2AetherDependency).toList, dependencyFilter)
      } getOrElse util.Collections.emptyList[Artifact]
    }
  }

  def allDependencies(artifact: Artifact,
                      managedDependencies: util.List[Dependency] = List.empty[Dependency],
                      dependencyFilter: DependencyFilter = new EmptyDependencyFilter,
                      additionalRepositories: util.List[RemoteRepository] = List.empty[RemoteRepository]): util.List[Artifact] = {
    val collectRequest = new CollectRequest(new Dependency(artifact, ""), remoteRepositories)
    collectRequest.setManagedDependencies(managedDependencies)

    val collectResult = Try {
      repositorySystem.collectDependencies(repositorySystemSession, collectRequest)
    } recover {
      case e: DependencyCollectionException => e.getResult
      case e => throw e
    }

    (for {
      result <- collectResult.toOption
      root <- Option(result.getRoot)
    } yield {
      val rtranVisitor = new RtranDependencyVisitor
      val visitor = new TreeDependencyVisitor(new FilteringDependencyVisitor(rtranVisitor, dependencyFilter))
      root.accept(visitor)
      rtranVisitor.artifacts
    }).getOrElse(Collections.emptyList())
  }

  def resolveArtifact(artifact: Artifact,
                      additionalRepositories: util.List[RemoteRepository] = List.empty[RemoteRepository]): Artifact = {
    val artifactRequest = new ArtifactRequest(
      artifact,
      (new RemoteRepository.Builder(
        "local", DEFAULT, s"file://${localRepository.getAbsolutePath}"
      ).build :: remoteRepositories) ++ additionalRepositories,
      ""
    )
    val artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest)
    artifactResult.getArtifact
  }

  def findAvailableVersions(groupId: String,
                            artifactId: String,
                            versionPrefix: String = "",
                            snapshot: Boolean = false): util.List[String] = {
    val versionRange = if (Option(versionPrefix).isEmpty || versionPrefix.isEmpty) "[0,)" else s"[$versionPrefix.*]"
    val artifact = new DefaultArtifact(s"$groupId:$artifactId:$versionRange")
    val versionRangeRequest = new VersionRangeRequest(
      artifact,
      if (snapshot) remoteSnapshotRepositories else remoteReleaseRepositories,
      ""
    )
    val versionRangeResponse = repositorySystem.resolveVersionRange(repositorySystemSession, versionRangeRequest)
    versionRangeResponse.getVersions.map(_.toString)
  }

  implicit def mavenDependency2AetherDependency(dependency: maven.Dependency): Dependency = {
    val artifactTypeRegistry = repositorySystemSession.getArtifactTypeRegistry
    val stereotype = Option(artifactTypeRegistry.get(dependency.getType)) getOrElse new DefaultArtifactType(dependency.getType)
    val props = Option(dependency.getSystemPath) match {
      case Some("") | None => Map.empty[String, String]
      case Some(path) => Map(ArtifactProperties.LOCAL_PATH -> path)
    }
    val artifact = new DefaultArtifact(dependency.getGroupId, dependency.getArtifactId, dependency.getClassifier, "",
      dependency.getVersion, props, stereotype)
    val exclusions = dependency.getExclusions map mavenExclusion2AetherExclusion
    new Dependency(artifact, dependency.getScope, dependency.isOptional, exclusions)
  }

  implicit def mavenDependency2Artifact(dep: maven.Dependency): Artifact = {
    new DefaultArtifact(s"${dep.getGroupId}:${dep.getArtifactId}::${dep.getVersion}")
  }

  implicit def mavenExclusion2AetherExclusion(exclusion: maven.Exclusion): Exclusion = {
    new Exclusion(exclusion.getGroupId, exclusion.getArtifactId, "*", "*" )
  }

  class EmptyDependencyFilter extends DependencyFilter {
    override def accept(node: DependencyNode, parents: util.List[DependencyNode]): Boolean = true
  }
}

/**
  * DependencyNode for collecting resolved transitive dependencies
  *
  */
private class RtranDependencyVisitor extends DependencyVisitor {
  val artifacts: util.List[Artifact] = new util.ArrayList[Artifact]()

  override def visitEnter(node: DependencyNode): Boolean = {
    Option(node.getArtifact).foreach(artifacts.add)
    true
  }

  override def visitLeave(node: DependencyNode): Boolean = true
}