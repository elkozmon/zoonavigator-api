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

import com.elkozmon.zoonavigator.core.curator.background.BackgroundPromiseFactory
import com.elkozmon.zoonavigator.core.action.ActionHandler
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.znode.{ZNodeData, ZNodeMeta, ZNodeMetaWith}
import org.apache.curator.framework.CuratorFramework

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Try}

class GetZNodeDataActionHandler(
  curatorFramework: CuratorFramework,
  backgroundPromiseFactory: BackgroundPromiseFactory,
  executionContextExecutor: ExecutionContextExecutor
) extends ActionHandler[GetZNodeDataAction] {

  override def handle(action: GetZNodeDataAction): Future[ZNodeMetaWith[ZNodeData]] = {
    val backgroundPromise = backgroundPromiseFactory.newBackgroundPromise {
      event =>
        val meta = ZNodeMeta.fromStat(event.getStat)
        val data = ZNodeData(Option(event.getData).getOrElse(Array.empty))

        ZNodeMetaWith(data, meta)
    }

    Try {
      curatorFramework
        .getData
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
