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

import com.elkozmon.zoonavigator.core.action.ActionHandler
import com.elkozmon.zoonavigator.core.curator.Implicits._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeMeta
import monix.eval.Task
import org.apache.curator.framework.CuratorFramework

import scala.jdk.CollectionConverters._

class UpdateZNodeAclListActionHandler extends ActionHandler[UpdateZNodeAclListAction] {

  override def handle(action: UpdateZNodeAclListAction): Task[ZNodeMeta] =
    action.curatorFramework
      .setACL()
      .withVersion(action.expectedAclVersion.version.toInt)
      .withACL(action.acl.aclList.map(Acl.toZooKeeper).asJava)
      .forPathAsync(action.path.path)
      .map(event => ZNodeMeta.fromStat(event.getStat))
}
