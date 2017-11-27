/*
 * Copyright (C) 2017  Ľuboš Kozmon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.elkozmon.zoonavigator.core.curator.background

import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.BackgroundCallback
import org.apache.curator.framework.api.CuratorEvent
import org.apache.curator.framework.api.UnhandledErrorListener
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.KeeperException.Code
import org.slf4j.LoggerFactory

import scala.concurrent.Promise

class DefaultBackgroundPromiseFactory extends BackgroundPromiseFactory {

  private val logger = LoggerFactory.getLogger(getClass)

  override def newBackgroundPromise[T](
      extractor: CuratorEvent => T
  ): BackgroundPromise[T] = {
    val promise = Promise[T]()

    val eventCallback = new BackgroundCallback {
      override def processResult(
          client: CuratorFramework,
          event: CuratorEvent
      ): Unit = {
        logger.debug(
          "{} event completed with result code {}",
          event.getType,
          event.getResultCode
        )

        if (event.getResultCode == 0) {
          promise.trySuccess(extractor(event)).asUnit()
        } else {
          val code = Code.get(event.getResultCode)
          val path = event.getPath

          promise.tryFailure(KeeperException.create(code, path)).asUnit()
        }
      }
    }

    val errorListener = new UnhandledErrorListener {
      override def unhandledError(message: String, e: Throwable): Unit = {
        logger.error(message, e)
        promise.tryFailure(e).asUnit()
      }
    }

    BackgroundPromise(promise, eventCallback, errorListener)
  }
}
