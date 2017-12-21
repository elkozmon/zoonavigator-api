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

import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.curator.TestingCuratorFrameworkProvider
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.acl.AclId
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeAcl
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeAclVersion
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import monix.execution.Scheduler
import org.scalatest.FlatSpec

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UpdateZNodeAclListRecursiveActionHandlerSpec extends FlatSpec {

  import Scheduler.Implicits.global

  private val curatorFramework =
    TestingCuratorFrameworkProvider.getCuratorFramework(getClass.getName)

  private val actionHandler = new UpdateZNodeAclListRecursiveActionHandler(
    curatorFramework
  )

  "UpdateZNodeAclListRecursiveActionHandler" should "set root ZNode ACL" in {
    val initAcl =
      ZNodeAcl(List(Acl(AclId("world", "anyone"), Permission.All))).aclList
        .map(Acl.toZookeeper)
        .asJava

    curatorFramework
      .transaction()
      .forOperations(
        curatorFramework
          .transactionOp()
          .create()
          .withACL(initAcl)
          .forPath("/test1", "foo".getBytes)
      )
      .asUnit()

    val newAcl =
      ZNodeAcl(
        List(
          Acl(AclId("world", "anyone"), Set(Permission.Admin, Permission.Read))
        )
      )

    val action = UpdateZNodeAclListRecursiveAction(
      ZNodePath.unsafe("/test1"),
      newAcl,
      ZNodeAclVersion(0L)
    )

    Await.result(actionHandler.handle(action).runAsync, Duration.Inf).asUnit()

    val currentAclList = curatorFramework.getACL
      .forPath("/test1")
      .asScala
      .toList
      .map(Acl.fromZookeeper)

    assertResult(newAcl.aclList)(currentAclList)
  }

  it should "set children ZNode ACL" in {
    val initAcl =
      ZNodeAcl(List(Acl(AclId("world", "anyone"), Permission.All))).aclList
        .map(Acl.toZookeeper)
        .asJava

    curatorFramework
      .transaction()
      .forOperations(
        curatorFramework
          .transactionOp()
          .create()
          .withACL(initAcl)
          .forPath("/test2", "foo".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .withACL(initAcl)
          .forPath("/test2/child", "bar".getBytes)
      )
      .asUnit()

    val newAcl =
      ZNodeAcl(
        List(
          Acl(AclId("world", "anyone"), Set(Permission.Admin, Permission.Read))
        )
      )

    val action = UpdateZNodeAclListRecursiveAction(
      ZNodePath.unsafe("/test2"),
      newAcl,
      ZNodeAclVersion(0L)
    )

    Await.result(actionHandler.handle(action).runAsync, Duration.Inf).asUnit()

    val currentAclList = curatorFramework.getACL
      .forPath("/test2/child")
      .asScala
      .toList
      .map(Acl.fromZookeeper)

    assertResult(newAcl.aclList)(currentAclList)
  }
}
