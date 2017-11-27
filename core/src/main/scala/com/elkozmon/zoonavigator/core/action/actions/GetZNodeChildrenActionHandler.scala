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
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import org.apache.curator.framework.CuratorFramework

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Try

class GetZNodeChildrenActionHandler(
    curatorFramework: CuratorFramework,
    backgroundPromiseFactory: BackgroundPromiseFactory,
    executionContextExecutor: ExecutionContextExecutor
) extends ActionHandler[GetZNodeChildrenAction] {

  override def handle(
      action: GetZNodeChildrenAction
  ): Future[ZNodeMetaWith[ZNodeChildren]] = {
    val backgroundPromise = backgroundPromiseFactory.newBackgroundPromise {
      event =>
        val path = event.getPath.stripSuffix("/")
        val meta = ZNodeMeta.fromStat(event.getStat)
        val children = ZNodeChildren(
          event.getChildren.asScala
            .map(name => ZNode(ZNodePath(s"$path/$name")))
            .toList
        )

        ZNodeMetaWith(children, meta)
    }

    Try {
      curatorFramework.getChildren
        .inBackground(
          backgroundPromise.eventCallback,
          executionContextExecutor: Executor
        )
        .withUnhandledErrorListener(backgroundPromise.errorListener)
        .forPath(action.path.path)
        .asUnit()
    } match {
      case Failure(throwable) =>
        backgroundPromise.promise
          .tryFailure(throwable)
          .asUnit()
      case _ =>
    }

    backgroundPromise.promise.future
  }
}
