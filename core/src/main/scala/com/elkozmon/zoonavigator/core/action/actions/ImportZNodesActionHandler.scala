/*
 * Copyright (C) 2019  Ľuboš Kozmon <https://www.elkozmon.com>
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

import cats.implicits._
import com.elkozmon.zoonavigator.core.action.ActionHandler
import com.elkozmon.zoonavigator.core.curator.Implicits._
import com.elkozmon.zoonavigator.core.utils.CommonUtils.discard
import com.elkozmon.zoonavigator.core.utils.ZooKeeperUtils
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeExport
import monix.eval.Task
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.CuratorEvent
import org.apache.curator.framework.api.transaction.CuratorOp
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.KeeperException.Code

import scala.collection.JavaConverters._

class ImportZNodesActionHandler(curatorFramework: CuratorFramework)
    extends ActionHandler[ImportZNodesAction] {

  override def handle(action: ImportZNodesAction): Task[Unit] =
    for {
      _ <- curatorFramework
        .checkExists()
        .forPathAsync(action.path.path)
        .map(discard[CuratorEvent])
        .onErrorRecoverWith {
          case e: KeeperException if e.code() == Code.NONODE =>
            curatorFramework
              .create()
              .creatingParentContainersIfNeeded()
              .forPathAsync(action.path.path)
              .map(discard[CuratorEvent])
        }
      trees <- Task.wander(action.nodes) { tree =>
        Task.fromTry(
          action.path
            .down(tree.head.path.name)
            .flatMap(ZooKeeperUtils.rewriteZNodePaths(_, tree))
        )
      }
      ops <- Task.now[List[CuratorOp]](
        trees
          .flatMap(
            _.reduceMap((node: ZNodeExport) => List(createZNodeOp(node)))
          )
      )
      unit <- curatorFramework
        .transaction()
        .forOperationsAsync(ops)
        .map(discard[CuratorEvent])
    } yield unit

  private def createZNodeOp(node: ZNodeExport): CuratorOp =
    curatorFramework
      .transactionOp()
      .create()
      .withACL(node.acl.aclList.map(Acl.toZooKeeper).asJava)
      .forPath(node.path.path, node.data.bytes)
}
