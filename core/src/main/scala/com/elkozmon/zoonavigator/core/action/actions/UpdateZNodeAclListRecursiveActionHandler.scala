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
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import monix.eval.Task
import org.apache.curator.framework.CuratorFramework

import scala.jdk.CollectionConverters._

class UpdateZNodeAclListRecursiveActionHandler(
    curatorFramework: CuratorFramework
) extends ActionHandler[UpdateZNodeAclListRecursiveAction] {

  override def handle(
      action: UpdateZNodeAclListRecursiveAction
  ): Task[ZNodeMeta] =
    for {
      tree <- curatorFramework.walkTreeAsync(Task.now)(action.path)
      meta <- setNodeAcl(tree.head, action.acl, Some(action.expectedAclVersion))
      _ <- Task.gatherUnordered(
        tree.forceTail
          .reduceMap(path => List(setNodeAcl(path, action.acl, None)))
      )
    } yield meta

  private def setNodeAcl(
      path: ZNodePath,
      acl: ZNodeAcl,
      aclVersionOpt: Option[ZNodeAclVersion]
  ): Task[ZNodeMeta] =
    aclVersionOpt
      .map(ver => curatorFramework.setACL().withVersion(ver.version.toInt))
      .getOrElse(curatorFramework.setACL())
      .withACL(acl.aclList.map(Acl.toZooKeeper).asJava)
      .forPathAsync(path.path)
      .map(event => ZNodeMeta.fromStat(event.getStat))

}
