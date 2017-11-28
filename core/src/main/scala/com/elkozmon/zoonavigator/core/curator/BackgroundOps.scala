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

package com.elkozmon.zoonavigator.core.curator

import java.util.concurrent.Executor

import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import org.apache.curator.framework.api._
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.KeeperException.Code
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.Promise

trait BackgroundOps {

  private val logger = LoggerFactory.getLogger(getClass)

  implicit class BackgroundPathableOps[T](action: BackgroundPathable[T]) {

    def forPathBackground(
        path: String
    )(implicit e: Executor): Future[CuratorEvent] = {
      val promise = Promise[CuratorEvent]()

      action
        .inBackground(newEventCallback(promise), e)
        .withUnhandledErrorListener(newErrorListener(promise))
        .forPath(path)
        .asUnit()

      promise.future
    }
  }

  implicit class BackgroundPathAndBytesableOps[T](
      action: BackgroundPathAndBytesable[T]
  ) {

    def forPathBackground(
        path: String
    )(implicit e: Executor): Future[CuratorEvent] = {
      val promise = Promise[CuratorEvent]()

      action
        .inBackground(newEventCallback(promise), e)
        .withUnhandledErrorListener(newErrorListener(promise))
        .forPath(path)
        .asUnit()

      promise.future
    }

    def forPathBackground(path: String, bytes: Array[Byte])(
        implicit e: Executor
    ): Future[CuratorEvent] = {
      val promise = Promise[CuratorEvent]()

      action
        .inBackground(newEventCallback(promise), e)
        .withUnhandledErrorListener(newErrorListener(promise))
        .forPath(path, bytes)
        .asUnit()

      promise.future
    }
  }

  private def newEventCallback(
      promise: Promise[CuratorEvent]
  ): BackgroundCallback =
    (_, event: CuratorEvent) => {
      logger.debug(
        "{} event completed with result code {}",
        event.getType,
        event.getResultCode
      )

      if (event.getResultCode == 0) {
        promise.trySuccess(event).asUnit()
      } else {
        val code = Code.get(event.getResultCode)
        val path = event.getPath

        promise.tryFailure(KeeperException.create(code, path)).asUnit()
      }
    }

  private def newErrorListener(promise: Promise[_]): UnhandledErrorListener =
    (message: String, e: Throwable) => {
      logger.error(message, e)
      promise.tryFailure(e).asUnit()
    }
}
