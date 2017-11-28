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
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeMeta
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.data.Id

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

class UpdateZNodeAclListActionHandler(
    curatorFramework: CuratorFramework,
    implicit val executionContextExecutor: ExecutionContextExecutor
) extends ActionHandler[UpdateZNodeAclListAction]
    with BackgroundOps {

  override def handle(action: UpdateZNodeAclListAction): Future[ZNodeMeta] =
    curatorFramework
      .setACL()
      .withVersion(action.expectedAclVersion.version.toInt)
      .withACL(action.acl.aclList.map { rawAcl =>
        new ACL(
          Permission.toZookeeperMask(rawAcl.permissions),
          new Id(rawAcl.aclId.scheme, rawAcl.aclId.id)
        )
      }.asJava)
      .forPathBackground(action.path.path)
      .map(event => ZNodeMeta.fromStat(event.getStat))
}
