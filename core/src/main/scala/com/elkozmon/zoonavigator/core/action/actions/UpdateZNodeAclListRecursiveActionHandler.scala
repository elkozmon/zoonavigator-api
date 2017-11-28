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
import com.elkozmon.zoonavigator.core.curator.BackgroundOps
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.data._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

class UpdateZNodeAclListRecursiveActionHandler(
    curatorFramework: CuratorFramework,
    implicit val executionContextExecutor: ExecutionContextExecutor
) extends ActionHandler[UpdateZNodeAclListRecursiveAction]
    with BackgroundOps {

  override def handle(
      action: UpdateZNodeAclListRecursiveAction
  ): Future[ZNodeMeta] = {
    // set acl for the parent node
    val futureMeta =
      setNodeAcl(action.path, action.acl, Some(action.expectedAclVersion))

    // set acl for children nodes recursively
    val futureUnit = setChildrenAclRecursive(action.path, action.acl)

    for {
      meta <- futureMeta
      unit <- futureUnit
    } yield {
      meta
    }
  }

  private def setChildrenAclRecursive(
      parent: ZNodePath,
      acl: ZNodeAcl
  ): Future[Unit] =
    for {
      children <- getNodeChildren(parent)
      _ <- Future.sequence(children.map(setNodeAcl(_, acl, None)))
      _ <- Future.sequence(children.map(setChildrenAclRecursive(_, acl)))
    } yield ()

  private def setNodeAcl(
      path: ZNodePath,
      acl: ZNodeAcl,
      aclVersionOpt: Option[ZNodeAclVersion]
  ): Future[ZNodeMeta] =
    aclVersionOpt
      .map(ver => curatorFramework.setACL().withVersion(ver.version.toInt))
      .getOrElse(curatorFramework.setACL())
      .withACL(acl.aclList.map { rawAcl =>
        new ACL(
          Permission.toZookeeperMask(rawAcl.permissions),
          new Id(rawAcl.aclId.scheme, rawAcl.aclId.id)
        )
      }.asJava)
      .forPathBackground(path.path)
      .map(event => ZNodeMeta.fromStat(event.getStat))

  private def getNodeChildren(path: ZNodePath): Future[List[ZNodePath]] =
    curatorFramework.getChildren
      .forPathBackground(path.path)
      .map { event =>
        event.getChildren.asScala
          .map(name => ZNodePath(s"${path.path}/$name"))
          .toList
      }
}
