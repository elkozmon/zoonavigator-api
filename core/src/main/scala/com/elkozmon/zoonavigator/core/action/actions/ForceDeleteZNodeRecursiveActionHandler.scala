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

import cats.free.Cofree
import cats.implicits._
import com.elkozmon.zoonavigator.core.action.ActionHandler
import com.elkozmon.zoonavigator.core.curator.BackgroundReadOps
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import monix.eval.Task
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.transaction.CuratorOp

class ForceDeleteZNodeRecursiveActionHandler(curatorFramework: CuratorFramework)
    extends ActionHandler[ForceDeleteZNodeRecursiveAction]
    with BackgroundReadOps {

  override def handle(action: ForceDeleteZNodeRecursiveAction): Task[Unit] =
    Task
      .gatherUnordered(
        action.paths.map(curatorFramework.getTreeBackground(Task.now))
      )
      .flatMap(deleteTrees)

  private def deleteTrees(trees: List[Cofree[List, ZNodePath]]): Task[Unit] = {
    val ops: Seq[CuratorOp] = trees
      .flatMap(_.reduceMap((path: ZNodePath) => List(deleteZNode(path))))
      .reverse

    curatorFramework
      .transaction()
      .forOperationsBackground(ops)
      .map(_.asUnit())
  }

  private def deleteZNode(path: ZNodePath): CuratorOp =
    curatorFramework
      .transactionOp()
      .delete()
      .forPath(path.path)
}
