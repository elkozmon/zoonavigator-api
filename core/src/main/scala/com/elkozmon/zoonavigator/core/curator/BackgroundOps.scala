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

import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import monix.eval.Callback
import monix.eval.Task
import monix.execution.Cancelable
import monix.execution.Scheduler
import org.apache.curator.framework.api._
import org.apache.curator.framework.api.transaction.CuratorOp
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.KeeperException.Code
import org.slf4j.LoggerFactory

import scala.util.Try

trait BackgroundOps {

  private val logger = LoggerFactory.getLogger(getClass)

  implicit class BackgroundTransactionOps(
      action: Backgroundable[ErrorListenerMultiTransactionMain]
  ) {

    def forOperationsBackground(ops: Seq[CuratorOp]): Task[CuratorEvent] =
      tryTaskCreate[CuratorEvent] { (scheduler, callback) =>
        action
          .inBackground(newEventCallback(callback), scheduler)
          .withUnhandledErrorListener(newErrorListener(callback))
          .forOperations(ops: _*)
          .asUnit()
      }
  }

  implicit class BackgroundPathableOps[T](action: BackgroundPathable[T]) {

    def forPathBackground(path: String): Task[CuratorEvent] =
      tryTaskCreate[CuratorEvent] { (scheduler, callback) =>
        action
          .inBackground(newEventCallback(callback), scheduler)
          .withUnhandledErrorListener(newErrorListener(callback))
          .forPath(path)
          .asUnit()
      }
  }

  implicit class BackgroundPathAndBytesableOps[T](
      action: BackgroundPathAndBytesable[T]
  ) {

    def forPathBackground(path: String): Task[CuratorEvent] =
      tryTaskCreate[CuratorEvent] { (scheduler, callback) =>
        action
          .inBackground(newEventCallback(callback), scheduler)
          .withUnhandledErrorListener(newErrorListener(callback))
          .forPath(path)
          .asUnit()
      }

    def forPathBackground(
        path: String,
        bytes: Array[Byte]
    ): Task[CuratorEvent] =
      tryTaskCreate[CuratorEvent] { (scheduler, callback) =>
        action
          .inBackground(newEventCallback(callback), scheduler)
          .withUnhandledErrorListener(newErrorListener(callback))
          .forPath(path, bytes)
          .asUnit()
      }
  }

  private def tryTaskCreate[T](fn: (Scheduler, Callback[T]) => Unit): Task[T] =
    Task.create[T] { (scheduler, callback) =>
      Try(fn(scheduler, callback)).toEither.left
        .foreach(callback.onError)

      Cancelable.empty
    }

  private def newEventCallback(
      callback: Callback[CuratorEvent]
  ): BackgroundCallback =
    (_, event: CuratorEvent) => {
      logger.debug(
        "{} event completed with result code {}",
        event.getType,
        event.getResultCode
      )

      if (event.getResultCode == 0) {
        callback.onSuccess(event)
      } else {
        val code = Code.get(event.getResultCode)
        val path = event.getPath

        callback.onError(KeeperException.create(code, path))
      }
    }

  private def newErrorListener(callback: Callback[_]): UnhandledErrorListener =
    (message: String, e: Throwable) => {
      logger.error(message, e)
      callback.onError(e)
    }
}
