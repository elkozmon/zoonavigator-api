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

import cats.free.Cofree
import cats._
import cats.implicits._
import com.elkozmon.zoonavigator.core.curator.CuratorSpec
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.acl.AclId
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import monix.execution.Scheduler
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@SuppressWarnings(
  Array(
    "org.wartremover.warts.TryPartial",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.TraversableOps"
  )
)
class ExportZNodesActionHandlerSpec extends FlatSpec with CuratorSpec {

  import Scheduler.Implicits.global

  private def getDefaultExportNode(path: String, data: String): ZNodeExport =
    ZNodeExport(
      ZNodeAcl(List(Acl(AclId("world", "anyone"), Permission.All))),
      ZNodePath.parse(path).get,
      ZNodeData(data.getBytes)
    )

  "ExportZNodeActionHandler" should "export two sibling nodes" in withCurator { curatorFramework =>
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
      ExportZNodesAction(Seq(ZNodePath.parse("/foo").get, ZNodePath.parse("/bar").get), curatorFramework)

    val exported =
      Await.result((new ExportZNodesActionHandler).handle(action).runToFuture, Duration.Inf)

    assertResult {
      List(
        Cofree(getDefaultExportNode("/foo", "foo"), Now(List.empty[Cofree[List, ZNodeExport]])),
        Cofree(getDefaultExportNode("/bar", "bar"), Now(List.empty[Cofree[List, ZNodeExport]]))
      )
    }(exported.map(_.forceAll))
  }

  it should "export one node with child" in withCurator { curatorFramework =>
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
          .forPath("/foo/bar", "bar".getBytes)
      )
      .discard()

    val action =
      ExportZNodesAction(Seq(ZNodePath.parse("/foo").get), curatorFramework)

    val exported =
      Await.result((new ExportZNodesActionHandler).handle(action).runToFuture, Duration.Inf)

    assertResult {
      List(
        Cofree(
          getDefaultExportNode("/foo", "foo"),
          Now(List(Cofree(getDefaultExportNode("/foo/bar", "bar"), Now(List.empty[Cofree[List, ZNodeExport]]))))
        )
      )
    }(exported.map(_.forceAll))
  }

  it should "export nodes as root nodes (path of the parent node is cut out)" in withCurator { curatorFramework =>
    curatorFramework
      .transaction()
      .forOperations(
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/export", Array.emptyByteArray),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/export/foo", "foo".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/export/foo/bar", "bar".getBytes),
        curatorFramework
          .transactionOp()
          .create()
          .forPath("/export/baz", "baz".getBytes)
      )
      .discard()

    val action =
      ExportZNodesAction(Seq(ZNodePath.parse("/export/foo").get, ZNodePath.parse("/export/baz").get), curatorFramework)

    val exported =
      Await.result((new ExportZNodesActionHandler).handle(action).runToFuture, Duration.Inf)

    assertResult {
      List(
        Cofree(
          getDefaultExportNode("/foo", "foo"),
          Now(List(Cofree(getDefaultExportNode("/foo/bar", "bar"), Now(List.empty[Cofree[List, ZNodeExport]]))))
        ),
        Cofree(getDefaultExportNode("/baz", "baz"), Now(List.empty[Cofree[List, ZNodeExport]]))
      )
    }(exported.map(_.forceAll))
  }
}
