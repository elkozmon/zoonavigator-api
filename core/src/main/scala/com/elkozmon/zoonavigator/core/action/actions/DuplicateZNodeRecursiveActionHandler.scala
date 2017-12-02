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
import com.elkozmon.zoonavigator.core.action.actions.DuplicateZNodeRecursiveActionHandler.ZNodeTree
import com.elkozmon.zoonavigator.core.curator.BackgroundOps
import com.elkozmon.zoonavigator.core.curator.Transactions
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.transaction._
import org.apache.zookeeper.data.ACL

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

// TODO make transactions asynchronous using curator-async
class DuplicateZNodeRecursiveActionHandler(
    curatorFramework: CuratorFramework,
    implicit val executionContextExecutor: ExecutionContextExecutor
) extends ActionHandler[DuplicateZNodeRecursiveAction]
    with BackgroundOps {

  override def handle(action: DuplicateZNodeRecursiveAction): Future[Unit] =
    for {
      tree <- getTree(action.source)
      destName <- Future.fromTry(action.destination.name)
      destDir <- Future.fromTry(action.destination.parent)
      unit <- createTree(destDir, tree.copy(name = destName))
    } yield unit

  private def getData(path: ZNodePath): Future[Array[Byte]] =
    curatorFramework.getData
      .forPathBackground(path.path)
      .map(_.getData)

  private def getAcl(path: ZNodePath): Future[List[ACL]] =
    curatorFramework.getACL
      .forPathBackground(path.path)
      .map(_.getACLList.asScala.toList)

  private def getChildren(path: ZNodePath): Future[List[ZNodePath]] =
    curatorFramework.getChildren
      .forPathBackground(path.path)
      .map { event =>
        val path = event.getPath.stripSuffix("/")

        event.getChildren.asScala
          .map(name => ZNodePath(s"$path/$name"))
          .toList
      }

  /**
    * @param node path to the root node of the tree to be fetched
    */
  private def getTree(node: ZNodePath): Future[ZNodeTree] = {
    val futureAcl = getAcl(node)
    val futureData = getData(node)
    val futureChildren = getChildren(node).flatMap(Future.traverse(_)(getTree))

    for {
      acl <- futureAcl
      name <- Future.fromTry(node.name)
      data <- futureData
      children <- futureChildren
    } yield ZNodeTree(name, data, acl, children)
  }

  /**
    * @param dir  directory where to create the tree's root node
    * @param tree tree to create
    */
  private def createTree(dir: ZNodePath, tree: ZNodeTree): Future[Unit] = {
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
      tree: ZNodeTree,
      transaction: CuratorTransactionFinal
  ): CuratorTransactionFinal = {
    val rootNode = dir.down(tree.name)

    tree.children
      .foldRight {
        transaction
          .create()
          .withACL(tree.acl.asJava)
          .forPath(rootNode.path, tree.data)
          .and()
      }(createTreeTransaction(rootNode, _, _))
  }
}

object DuplicateZNodeRecursiveActionHandler {

  private case class ZNodeTree(
      name: String,
      data: Array[Byte],
      acl: List[ACL],
      children: List[ZNodeTree]
  )

}
