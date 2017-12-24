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
import com.elkozmon.zoonavigator.core.curator.Implicits._
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import monix.eval.Task
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.CuratorEvent
import org.apache.curator.framework.api.transaction._

import scala.collection.JavaConverters._

class DuplicateZNodeRecursiveActionHandler(curatorFramework: CuratorFramework)
    extends ActionHandler[DuplicateZNodeRecursiveAction] {

  override def handle(action: DuplicateZNodeRecursiveAction): Task[Unit] =
    for {
      tree <- curatorFramework
        .walkTreeAsync(curatorFramework.getZNodeAsync)(action.source)
        .map(rewritePaths(action.destination, _))
      unit <- createTree(tree)
    } yield unit

  private def createTree(tree: Cofree[List, ZNode]): Task[Unit] = {
    val ops: Seq[CuratorOp] =
      tree.reduceMap((node: ZNode) => List(createZNode(node)))

    curatorFramework
      .transaction()
      .forOperationsAsync(ops)
      .map(discard[CuratorEvent])
  }

  private def rewritePaths(
      path: ZNodePath,
      tree: Cofree[List, ZNode]
  ): Cofree[List, ZNode] =
    Cofree(
      tree.head.copy(path = path),
      tree.tail.map(_.map(c => rewritePaths(path.down(c.head.path.name), c)))
    )

  private def createZNode(node: ZNode): CuratorOp =
    curatorFramework
      .transactionOp()
      .create()
      .withACL(node.acl.aclList.map(Acl.toZookeeper).asJava)
      .forPath(node.path.path, node.data.bytes)
}
