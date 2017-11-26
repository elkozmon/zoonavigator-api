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

package com.elkozmon.zoonavigator.core.action.actions

import java.util.concurrent.Executor

import com.elkozmon.zoonavigator.core.action.ActionHandler
import com.elkozmon.zoonavigator.core.curator.background.BackgroundPromiseFactory
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import org.apache.curator.framework.CuratorFramework

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Try}

class ForceDeleteZNodeRecursiveActionHandler(
  curatorFramework: CuratorFramework,
  backgroundPromiseFactory: BackgroundPromiseFactory,
  executionContextExecutor: ExecutionContextExecutor
) extends ActionHandler[ForceDeleteZNodeRecursiveAction] {

  override def handle(action: ForceDeleteZNodeRecursiveAction): Future[Unit] = {
    val backgroundPromise = backgroundPromiseFactory.newBackgroundPromiseUnit

    Try {
      curatorFramework
        .delete()
        .deletingChildrenIfNeeded()
        .inBackground(
          backgroundPromise.eventCallback,
          executionContextExecutor: Executor
        )
        .withUnhandledErrorListener(
          backgroundPromise.errorListener
        )
        .forPath(
          action.path.path
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
