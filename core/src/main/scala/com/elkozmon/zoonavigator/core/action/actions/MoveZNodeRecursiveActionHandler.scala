/*
 * Copyright (C) 2018  Ľuboš Kozmon
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
import com.elkozmon.zoonavigator.core.utils.ZooKeeperUtils
import com.elkozmon.zoonavigator.core.utils.CommonUtils.discard
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNode
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import monix.eval.Task
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.CuratorEvent
import org.apache.curator.framework.api.transaction.CuratorOp

import scala.collection.JavaConverters._

class MoveZNodeRecursiveActionHandler(curatorFramework: CuratorFramework)
    extends ActionHandler[MoveZNodeRecursiveAction] {

  override def handle(action: MoveZNodeRecursiveAction): Task[Unit] =
    for {
      tree <- curatorFramework
        .walkTreeAsync(curatorFramework.getZNodeAsync)(action.source)
      unit <- moveTree(action.destination, tree)
    } yield unit

  private def moveTree(
      dest: ZNodePath,
      tree: Cofree[List, ZNode]
  ): Task[Unit] = {
    val deleteOps: Seq[CuratorOp] = tree
      .reduceMap((node: ZNode) => List(deleteZNodeOp(node.path)))
      .reverse

    val createOps: Seq[CuratorOp] = ZooKeeperUtils
      .rewriteZNodePaths(dest, tree)
      .reduceMap((node: ZNode) => List(createZNodeOp(node)))

    curatorFramework
      .transaction()
      .forOperationsAsync(deleteOps ++ createOps)
      .map(discard[CuratorEvent])
  }

  private def createZNodeOp(node: ZNode): CuratorOp =
    curatorFramework
      .transactionOp()
      .create()
      .withACL(node.acl.aclList.map(Acl.toZooKeeper).asJava)
      .forPath(node.path.path, node.data.bytes)

  private def deleteZNodeOp(path: ZNodePath): CuratorOp =
    curatorFramework
      .transactionOp()
      .delete()
      .forPath(path.path)
}
