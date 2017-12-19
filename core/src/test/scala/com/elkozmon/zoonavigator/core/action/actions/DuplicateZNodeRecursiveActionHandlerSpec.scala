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

import com.elkozmon.zoonavigator.core.curator.TestingCuratorFrameworkProvider
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import monix.execution.Scheduler
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.data.Id
import org.scalatest.FlatSpec

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

class DuplicateZNodeRecursiveActionHandlerSpec extends FlatSpec {

  import Scheduler.Implicits.global

  private val curatorFramework =
    TestingCuratorFrameworkProvider.getCuratorFramework(getClass.getName)

  private val duplicateActionHandler = new DuplicateZNodeRecursiveActionHandler(
    curatorFramework
  )

  "DuplicateZNodeRecursiveActionHandler" should "copy 2nd level ZNodes" in {
    curatorFramework
      .transaction()
      .forOperations(
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/test1", "foo".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/test1/bar", "bar".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/test1/baz", "baz".getBytes)
      )
      .asUnit()

    val action =
      DuplicateZNodeRecursiveAction(
        ZNodePath.unsafe("/test1"),
        ZNodePath.unsafe("/test1-copy")
      )

    Await.result(duplicateActionHandler.handle(action).runAsync, Duration.Inf)

    val bar = new String(curatorFramework.getData.forPath("/test1-copy/bar"))
    val baz = new String(curatorFramework.getData.forPath("/test1-copy/baz"))

    assertResult("barbaz")(bar + baz)
  }

  it should "copy ZNodes ACLs" in {
    val acl = new ACL(
      Permission.toZookeeperMask(Set(Permission.Admin, Permission.Read)),
      new Id("world", "anyone")
    )

    curatorFramework
      .create()
      .withACL(List(acl).asJava)
      .forPath("/test2", "foo".getBytes)
      .asUnit()

    val action =
      DuplicateZNodeRecursiveAction(
        ZNodePath.unsafe("/test2"),
        ZNodePath.unsafe("/test2-copy")
      )

    Await.result(duplicateActionHandler.handle(action).runAsync, Duration.Inf)

    assert(
      curatorFramework.getACL
        .forPath("/test2-copy")
        .asScala
        .forall(_.equals(acl))
    )
  }
}
