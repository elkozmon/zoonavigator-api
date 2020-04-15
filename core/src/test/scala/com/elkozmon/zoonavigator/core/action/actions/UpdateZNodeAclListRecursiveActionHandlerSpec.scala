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

import com.elkozmon.zoonavigator.core.curator.CuratorSpec
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.acl.AclId
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeAcl
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeAclVersion
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import monix.execution.Scheduler
import org.apache.curator.framework.CuratorFramework
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
class UpdateZNodeAclListRecursiveActionHandlerSpec extends FlatSpec with CuratorSpec {

  import Scheduler.Implicits.global

  "UpdateZNodeAclListRecursiveActionHandler" should "set root node ACLs" in withCurator { curatorFramework =>
    val initAcl =
      ZNodeAcl(List(Acl(AclId("world", "anyone"), Permission.All))).aclList
        .map(Acl.toZooKeeper)
        .asJava

    curatorFramework
      .transaction()
      .forOperations(
        curatorFramework
          .transactionOp()
          .create()
          .withACL(initAcl)
          .forPath("/foo", "foo".getBytes)
      )
      .discard()

    val newAcl =
      ZNodeAcl(List(Acl(AclId("world", "anyone"), Set(Permission.Admin, Permission.Read))))

    val action =
      UpdateZNodeAclListRecursiveAction(ZNodePath.parse("/foo").get, newAcl, ZNodeAclVersion(0L), curatorFramework)

    Await
      .result((new UpdateZNodeAclListRecursiveActionHandler).handle(action).runToFuture, Duration.Inf)
      .discard()

    val currentAclList = curatorFramework.getACL
      .forPath("/foo")
      .asScala
      .toList
      .map(Acl.fromZooKeeper)

    assertResult(newAcl.aclList)(currentAclList)
  }

  it should "set children node ACLs" in withCurator { curatorFramework =>
    val initAcl =
      ZNodeAcl(List(Acl(AclId("world", "anyone"), Permission.All))).aclList
        .map(Acl.toZooKeeper)
        .asJava

    curatorFramework
      .transaction()
      .forOperations(
        curatorFramework
          .transactionOp()
          .create()
          .withACL(initAcl)
          .forPath("/foo", "foo".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .withACL(initAcl)
          .forPath("/foo/bar", "bar".getBytes)
      )
      .discard()

    val newAcl =
      ZNodeAcl(List(Acl(AclId("world", "anyone"), Set(Permission.Admin, Permission.Read))))

    val action =
      UpdateZNodeAclListRecursiveAction(ZNodePath.parse("/foo").get, newAcl, ZNodeAclVersion(0L), curatorFramework)

    Await
      .result((new UpdateZNodeAclListRecursiveActionHandler).handle(action).runToFuture, Duration.Inf)
      .discard()

    val currentAclList = curatorFramework.getACL
      .forPath("/foo/bar")
      .asScala
      .toList
      .map(Acl.fromZooKeeper)

    assertResult(newAcl.aclList)(currentAclList)
  }
}
