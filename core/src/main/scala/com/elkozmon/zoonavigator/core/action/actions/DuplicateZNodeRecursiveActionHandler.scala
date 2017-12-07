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
import com.elkozmon.zoonavigator.core.curator.Transactions
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.transaction._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

// TODO make transactions asynchronous using curator-async
class DuplicateZNodeRecursiveActionHandler(
    curatorFramework: CuratorFramework,
    implicit val executionContextExecutor: ExecutionContextExecutor
) extends ActionHandler[DuplicateZNodeRecursiveAction]
    with BackgroundReadOps {

  override def handle(action: DuplicateZNodeRecursiveAction): Future[Unit] =
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
  private def createTree(dir: ZNodePath, tree: ZNode): Future[Unit] = {
    val transaction = createTreeTransaction(
      dir,
      tree,
      Transactions.emptyTransaction(curatorFramework)
    )

    Future(transaction.commit().asUnit())
  }

  /**
    * @param dir         directory where to create the tree's root node
    * @param tree        tree to create
    * @param transaction transaction builder
    * @return prepared transaction
    */
  private def createTreeTransaction(
      dir: ZNodePath,
      tree: ZNode,
      transaction: CuratorTransactionFinal
  ): CuratorTransactionFinal = {
    val rootNode = dir.down(tree.path.name)

    tree.children
      .foldRight {
        transaction
          .create()
          .withACL(tree.acl.aclList.map(Acl.toZookeeper).asJava)
          .forPath(rootNode.path, tree.data.bytes)
          .and()
      }(createTreeTransaction(rootNode, _, _))
  }
}
