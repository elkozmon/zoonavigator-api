/*
 * Copyright (C) 2018  Ľuboš Kozmon
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
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import monix.execution.Scheduler
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.data.Id
import org.scalatest.FlatSpec

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
class MoveZNodeRecursiveActionHandlerSpec extends FlatSpec with CuratorSpec {

  import Scheduler.Implicits.global

  private def actionHandler(implicit curatorFramework: CuratorFramework) =
    new MoveZNodeRecursiveActionHandler(curatorFramework)

  "MoveZNodeRecursiveActionHandler" should "copy child nodes data" in withCurator {
    implicit curatorFramework =>
      curatorFramework
        .transaction()
        .forOperations(
          curatorFramework
            .transactionOp()
            .create()
            .forPath("/foo", "foo".getBytes),
          curatorFramework
            .transactionOp()
            .create()
            .forPath("/foo/bar", "bar".getBytes),
          curatorFramework
            .transactionOp()
            .create()
            .forPath("/foo/baz", "baz".getBytes)
        )
        .discard()

      val action =
        MoveZNodeRecursiveAction(
          ZNodePath.parse("/foo").get,
          ZNodePath.parse("/foo-move").get
        )

      Await.result(actionHandler.handle(action).runAsync, Duration.Inf)

      val bar = new String(curatorFramework.getData.forPath("/foo-move/bar"))
      val baz = new String(curatorFramework.getData.forPath("/foo-move/baz"))

      assertResult("barbaz")(bar + baz)
  }

  it should "remove old nodes" in withCurator { implicit curatorFramework =>
    curatorFramework
      .transaction()
      .forOperations(
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/foo", "foo".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/foo/bar", "bar".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/foo/baz", "baz".getBytes)
      )
      .discard()

    val action =
      MoveZNodeRecursiveAction(
        ZNodePath.parse("/foo").get,
        ZNodePath.parse("/foo-move").get
      )

    Await.result(actionHandler.handle(action).runAsync, Duration.Inf)

    assert(checkExists("/foo").isEmpty).discard()
    assert(checkExists("/foo/bar").isEmpty).discard()
    assert(checkExists("/foo/baz").isEmpty).discard()
  }

  it should "copy ACLs" in withCurator { implicit curatorFramework =>
    val acl = new ACL(
      Permission.toZooKeeperMask(Set(Permission.Admin, Permission.Read)),
      new Id("world", "anyone")
    )

    curatorFramework
      .create()
      .withACL(List(acl).asJava)
      .forPath("/foo", "foo".getBytes)
      .discard()

    val action =
      MoveZNodeRecursiveAction(
        ZNodePath.parse("/foo").get,
        ZNodePath.parse("/foo-move").get
      )

    Await.result(actionHandler.handle(action).runAsync, Duration.Inf)

    assert(
      curatorFramework.getACL
        .forPath("/foo-move")
        .asScala
        .forall(_.equals(acl))
    )
  }
}
