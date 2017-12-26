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
import org.apache.zookeeper.data.Stat
import org.scalatest.FlatSpec

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

class MoveZNodeRecursiveActionHandlerSpec extends FlatSpec {

  import Scheduler.Implicits.global

  private val curatorFramework =
    TestingCuratorFrameworkProvider.getCuratorFramework(getClass.getName)

  private val actionHandler = new MoveZNodeRecursiveActionHandler(
    curatorFramework
  )

  private def checkExists(path: String): Option[Stat] =
    Option(curatorFramework.checkExists().forPath(path))

  "MoveZNodeRecursiveActionHandler" should "copy child nodes" in {
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
      .discard()

    val action =
      MoveZNodeRecursiveAction(
        ZNodePath.unsafe("/test1"),
        ZNodePath.unsafe("/test1-move")
      )

    Await.result(actionHandler.handle(action).runAsync, Duration.Inf)

    val bar = new String(curatorFramework.getData.forPath("/test1-move/bar"))
    val baz = new String(curatorFramework.getData.forPath("/test1-move/baz"))

    assertResult("barbaz")(bar + baz)
  }

  it should "remove old nodes" in {
    curatorFramework
      .transaction()
      .forOperations(
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/test2", "foo".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/test2/bar", "bar".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/test2/baz", "baz".getBytes)
      )
      .discard()

    val action =
      MoveZNodeRecursiveAction(
        ZNodePath.unsafe("/test2"),
        ZNodePath.unsafe("/test2-move")
      )

    Await.result(actionHandler.handle(action).runAsync, Duration.Inf)

    assert(checkExists("/test2").isEmpty).discard()
    assert(checkExists("/test2/bar").isEmpty).discard()
    assert(checkExists("/test2/baz").isEmpty).discard()
  }

  it should "copy ACLs" in {
    val acl = new ACL(
      Permission.toZookeeperMask(Set(Permission.Admin, Permission.Read)),
      new Id("world", "anyone")
    )

    curatorFramework
      .create()
      .withACL(List(acl).asJava)
      .forPath("/test3", "foo".getBytes)
      .discard()

    val action =
      MoveZNodeRecursiveAction(
        ZNodePath.unsafe("/test3"),
        ZNodePath.unsafe("/test3-move")
      )

    Await.result(actionHandler.handle(action).runAsync, Duration.Inf)

    assert(
      curatorFramework.getACL
        .forPath("/test3-move")
        .asScala
        .forall(_.equals(acl))
    )
  }
}
