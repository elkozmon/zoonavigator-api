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

package com.elkozmon.zoonavigator.core.command.commands

import java.util.concurrent.Executor

import com.elkozmon.zoonavigator.core.command.CommandHandler
import com.elkozmon.zoonavigator.core.curator.background.BackgroundPromiseFactory
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import org.apache.curator.framework.CuratorFramework

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Try}

class DeleteZNodeRecursiveCommandHandler(
  curatorFramework: CuratorFramework,
  backgroundPromiseFactory: BackgroundPromiseFactory,
  executionContextExecutor: ExecutionContextExecutor
) extends CommandHandler[DeleteZNodeRecursiveCommand] {

  override def handle(command: DeleteZNodeRecursiveCommand): Future[Unit] = {
    val backgroundPromise = backgroundPromiseFactory.newBackgroundPromiseUnit

    Try {
      curatorFramework
        .delete()
        .deletingChildrenIfNeeded()
        .withVersion(
          command.expectedDataVersion.version.toInt
        )
        .inBackground(
          backgroundPromise.eventCallback,
          executionContextExecutor: Executor
        )
        .withUnhandledErrorListener(
          backgroundPromise.errorListener
        )
        .forPath(
          command.path.path
        )
        .asUnit()
    } match {
      case Failure(throwable) =>
        backgroundPromise
          .promise
          .tryFailure(throwable)
          .asUnit()
      case _ =>
    }

    backgroundPromise.promise.future
  }
}
