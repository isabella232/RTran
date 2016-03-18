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

package com.ebay.rtran.report

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply


class SameThreadFilter extends Filter[ILoggingEvent] {

  val currentThreadName = Thread.currentThread.getName

  override def decide(event: ILoggingEvent): FilterReply = {
    if (event.getThreadName == currentThreadName) {
      FilterReply.NEUTRAL
    } else {
      FilterReply.DENY
    }
  }
}

class LoggerNameFilter(loggerNames: Set[String]) extends Filter[ILoggingEvent] {
  override def decide(event: ILoggingEvent): FilterReply = {
    if (loggerNames.isEmpty) {
      FilterReply.NEUTRAL
    } else {
      if (loggerNames contains event.getLoggerName) FilterReply.NEUTRAL else FilterReply.DENY
    }
  }
}