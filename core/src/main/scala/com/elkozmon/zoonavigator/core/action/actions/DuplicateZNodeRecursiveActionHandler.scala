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

import com.elkozmon.zoonavigator.core.action.ActionHandler
import com.elkozmon.zoonavigator.core.curator.BackgroundReadOps
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import monix.eval.Task
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.transaction._

import scala.collection.JavaConverters._

class DuplicateZNodeRecursiveActionHandler(curatorFramework: CuratorFramework)
    extends ActionHandler[DuplicateZNodeRecursiveAction]
    with BackgroundReadOps {

  override def handle(action: DuplicateZNodeRecursiveAction): Task[Unit] =
    for {
      tree <- curatorFramework.getTreeBackground(action.source)
      _ <- createTree(
        action.destination.parent,
        tree.copy(path = action.destination)
      )
    } yield ()

  /**
    * @param dir  directory where to create the tree's root node
    * @param tree tree to create
    */
  private def createTree(dir: ZNodePath, tree: ZNode): Task[Unit] = {
    val transactions = createTransactions(dir, tree, List.empty)

    curatorFramework
      .transaction()
      .forOperationsBackground(transactions)
      .map(_.asUnit())
  }

  /**
    * @param dir         directory where to create the tree's root node
    * @param tree        tree to create
    * @param ops         accumulative list of transactions
    * @return prepared transaction
    */
  private def createTransactions(
      dir: ZNodePath,
      tree: ZNode,
      ops: List[CuratorOp]
  ): List[CuratorOp] = {
    val rootNode = dir.down(tree.path.name)
    val nextOp = curatorFramework
      .transactionOp()
      .create()
      .withACL(tree.acl.aclList.map(Acl.toZookeeper).asJava)
      .forPath(rootNode.path, tree.data.bytes)

    tree.children
      .foldRight(ops :+ nextOp)(createTransactions(rootNode, _, _))
  }
}
