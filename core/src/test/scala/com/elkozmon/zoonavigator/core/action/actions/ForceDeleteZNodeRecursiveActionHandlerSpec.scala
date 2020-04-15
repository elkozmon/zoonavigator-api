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
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import monix.execution.Scheduler
import org.apache.curator.framework.CuratorFramework
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.TryPartial"))
class ForceDeleteZNodeRecursiveActionHandlerSpec extends FlatSpec with CuratorSpec {

  import Scheduler.Implicits.global

  "ForceDeleteZNodeRecursiveActionHandler" should "delete two sibling nodes" in withCurator {
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
            .forPath("/bar", "bar".getBytes),
          curatorFramework
            .transactionOp()
            .create()
            .forPath("/baz", "baz".getBytes)
        )
        .discard()

      val action =
        ForceDeleteZNodeRecursiveAction(Seq("/foo", "/bar").map(ZNodePath.parse _ andThen (_.get)), curatorFramework)

      Await.result((new ForceDeleteZNodeRecursiveActionHandler).handle(action).runToFuture, Duration.Inf)

      assert(checkExists("/foo").isEmpty)
      assert(checkExists("/bar").isEmpty)
      assert(checkExists("/baz").isDefined)
  }

  it should "delete node with its children" in withCurator { implicit curatorFramework =>
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
      ForceDeleteZNodeRecursiveAction(Seq(ZNodePath.parse("/foo").get), curatorFramework)

    Await.result((new ForceDeleteZNodeRecursiveActionHandler).handle(action).runToFuture, Duration.Inf)

    assert(checkExists("/foo").isEmpty)
    assert(checkExists("/foo/bar").isEmpty)
    assert(checkExists("/foo/baz").isEmpty)
  }

  it should "not delete anything if there is an error" in withCurator { implicit curatorFramework =>
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
          .forPath("/bar", "bar".getBytes)
      )
      .discard()

    val action =
      ForceDeleteZNodeRecursiveAction(
        Seq("/foo", "/bar", "/nonexistent").map(ZNodePath.parse _ andThen (_.get)),
        curatorFramework
      )

    Await.ready((new ForceDeleteZNodeRecursiveActionHandler).handle(action).runToFuture, Duration.Inf)

    assert(checkExists("/foo").isDefined)
    assert(checkExists("/bar").isDefined)
    assert(checkExists("/nonexistent").isEmpty)
  }
}
