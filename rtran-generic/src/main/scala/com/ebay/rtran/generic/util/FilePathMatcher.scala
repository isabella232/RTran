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

package com.ebay.rtran.generic.util

import java.io.File
import java.nio.file.{FileSystems, PathMatcher}

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils
import org.mozilla.universalchardet.CharsetListener

import scala.util.Try


object FilePathMatcher {

  def apply(rootDir: File, pathPattern: String): Try[PathMatcher] = Try {
    val trimmedPattern = new String(pathPattern.trim.toCharArray.dropWhile(_ == '/')).trim
	val path=rootDir.getAbsolutePath.replaceAll("\\\\","/")
    FileSystems.getDefault.getPathMatcher(s"glob:${path}/$trimmedPattern")
    //FileSystems.getDefault.getPathMatcher(s"glob:${rootDir.getAbsolutePath}/$trimmedPattern")
  }
}

object EncodingDetector extends LazyLogging {

  val DEFAULT_ENCODING = "UTF-8"

  def guessEncoding(file: File) = {
    val bytes = FileUtils.readFileToByteArray(file)
    val dummyListener = new CharsetListener {
      override def report(charset: String): Unit = {}
    }
    val detector = new org.mozilla.universalchardet.UniversalDetector(dummyListener)
    detector.handleData(bytes, 0, bytes.length)
    detector.dataEnd()
    val encoding = Option(detector.getDetectedCharset) getOrElse DEFAULT_ENCODING
    logger.debug("Detected encoding {} for {}", detector.getDetectedCharset, file)
    detector.reset()
    (encoding, bytes)
  }
}
