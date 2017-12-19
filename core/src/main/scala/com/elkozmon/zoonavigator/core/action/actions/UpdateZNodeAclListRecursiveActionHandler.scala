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
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import monix.eval.Task
import org.apache.curator.framework.CuratorFramework

import scala.collection.JavaConverters._

class UpdateZNodeAclListRecursiveActionHandler(
    curatorFramework: CuratorFramework
) extends ActionHandler[UpdateZNodeAclListRecursiveAction]
    with BackgroundReadOps {

  override def handle(
      action: UpdateZNodeAclListRecursiveAction
  ): Task[ZNodeMeta] = {
    // set acl for the parent node
    val taskMeta =
      setNodeAcl(action.path, action.acl, Some(action.expectedAclVersion))

    // set acl for children nodes recursively
    val taskUnit = setChildrenAclRecursive(action.path, action.acl)

    for {
      meta <- taskMeta
      _ <- taskUnit
    } yield meta
  }

  private def setChildrenAclRecursive(
      parent: ZNodePath,
      acl: ZNodeAcl
  ): Task[Unit] =
    curatorFramework
      .getChildrenBackground(parent)
      .flatMap {
        case ZNodeMetaWith(ZNodeChildren(paths), _) =>
          val aclsFuture = Task.traverse(paths)(setNodeAcl(_, acl, None))
          val childrenFuture =
            Task.traverse(paths)(setChildrenAclRecursive(_, acl))

          for {
            _ <- aclsFuture
            _ <- childrenFuture
          } yield ()
      }

  private def setNodeAcl(
      path: ZNodePath,
      acl: ZNodeAcl,
      aclVersionOpt: Option[ZNodeAclVersion]
  ): Task[ZNodeMeta] =
    aclVersionOpt
      .map(ver => curatorFramework.setACL().withVersion(ver.version.toInt))
      .getOrElse(curatorFramework.setACL())
      .withACL(acl.aclList.map(Acl.toZookeeper).asJava)
      .forPathBackground(path.path)
      .map(event => ZNodeMeta.fromStat(event.getStat))

}
