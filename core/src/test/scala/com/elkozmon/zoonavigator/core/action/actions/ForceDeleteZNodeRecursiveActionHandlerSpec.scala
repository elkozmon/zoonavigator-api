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
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import monix.execution.Scheduler
import org.apache.zookeeper.data.Stat
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class ForceDeleteZNodeRecursiveActionHandlerSpec extends FlatSpec {

  import Scheduler.Implicits.global

  private val curatorFramework =
    TestingCuratorFrameworkProvider.getCuratorFramework(getClass.getName)

  private val actionHandler = new ForceDeleteZNodeRecursiveActionHandler(
    curatorFramework
  )

  private def checkExists(path: String): Option[Stat] =
    Option(curatorFramework.checkExists().forPath(path))

  "ForceDeleteZNodeRecursiveActionHandler" should "delete two sibling ZNodes" in {
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
          .forPath("/test2", "bar".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/test3", "baz".getBytes)
      )
      .discard()

    val action =
      ForceDeleteZNodeRecursiveAction(
        Seq("/test1", "/test2").map(ZNodePath.unsafe)
      )

    Await.result(actionHandler.handle(action).runAsync, Duration.Inf)

    assert(checkExists("/test1").isEmpty)
    assert(checkExists("/test2").isEmpty)
    assert(checkExists("/test3").isDefined)
  }

  it should "delete ZNode with children" in {
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
          .forPath("/test1/child1", "bar".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/test1/child2", "baz".getBytes)
      )
      .discard()

    val action =
      ForceDeleteZNodeRecursiveAction(ZNodePath.unsafe("/test1"))

    Await.result(actionHandler.handle(action).runAsync, Duration.Inf)

    assert(checkExists("/test1/child1").isEmpty)
    assert(checkExists("/test1/child2").isEmpty)
    assert(checkExists("/test1").isEmpty)
  }

  it should "not delete anything if there is an error" in {
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
          .forPath("/test2", "bar".getBytes)
      )
      .discard()

    val action =
      ForceDeleteZNodeRecursiveAction(
        Seq("/test1", "/test2", "/nonexistent").map(ZNodePath.unsafe)
      )

    Await.ready(actionHandler.handle(action).runAsync, Duration.Inf)

    assert(checkExists("/test1").isDefined)
    assert(checkExists("/test2").isDefined)
    assert(checkExists("/nonexistent").isEmpty)
  }
}
