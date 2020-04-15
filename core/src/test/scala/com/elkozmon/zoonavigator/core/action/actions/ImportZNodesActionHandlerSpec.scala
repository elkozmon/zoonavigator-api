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

import cats._
import cats.free.Cofree
import com.elkozmon.zoonavigator.core.curator.CuratorSpec
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.acl.AclId
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission._
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import monix.execution.Scheduler
import org.apache.curator.framework.CuratorFramework
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

@SuppressWarnings(
  Array(
    "org.wartremover.warts.TryPartial",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.TraversableOps"
  )
)
class ImportZNodesActionHandlerSpec extends FlatSpec with CuratorSpec {

  import Scheduler.Implicits.global

  private def getExportNode(path: String, data: String, acls: List[Acl]): ZNodeExport =
    ZNodeExport(ZNodeAcl(acls), ZNodePath.parse(path).get, ZNodeData(data.getBytes))

  "ImportZNodeActionHandler" should "import two sibling nodes" in withCurator { curatorFramework =>
    val fooAclDefault =
      List(Acl(AclId("world", "anyone"), Set(Read, Create)))
    val barAclDefault =
      List(Acl(AclId("world", "anyone"), Set(Read, Create, Delete)))

    val exported =
      List(
        Cofree(getExportNode("/foo", "foo", fooAclDefault), Now(List.empty[Cofree[List, ZNodeExport]])),
        Cofree(getExportNode("/bar", "bar", barAclDefault), Now(List.empty[Cofree[List, ZNodeExport]]))
      )

    val action =
      ImportZNodesAction(ZNodePath.parse("/").get, exported, curatorFramework)

    Await.result((new ImportZNodesActionHandler).handle(action).runToFuture, Duration.Inf)

    val fooData = new String(curatorFramework.getData.forPath("/foo"))
    val fooAcl = curatorFramework.getACL
      .forPath("/foo")
      .asScala
      .map(Acl.fromZooKeeper)

    val barData = new String(curatorFramework.getData.forPath("/bar"))
    val barAcl = curatorFramework.getACL
      .forPath("/bar")
      .asScala
      .map(Acl.fromZooKeeper)

    assertResult("foo")(fooData)
    assertResult(fooAclDefault)(fooAcl)

    assertResult("bar")(barData)
    assertResult(barAclDefault)(barAcl)
  }

  it should "import one node with child" in withCurator { curatorFramework =>
    val fooAclDefault =
      List(Acl(AclId("world", "anyone"), Set(Read, Create)))
    val barAclDefault =
      List(Acl(AclId("world", "anyone"), Set(Read, Create, Delete)))

    val exported =
      List(
        Cofree(
          getExportNode("/foo", "foo", fooAclDefault),
          Now(List(Cofree(getExportNode("/foo/bar", "bar", barAclDefault), Now(List.empty[Cofree[List, ZNodeExport]]))))
        )
      )

    val action =
      ImportZNodesAction(ZNodePath.parse("/").get, exported, curatorFramework)

    Await.result((new ImportZNodesActionHandler).handle(action).runToFuture, Duration.Inf)

    val fooData = new String(curatorFramework.getData.forPath("/foo"))
    val fooAcl = curatorFramework.getACL
      .forPath("/foo")
      .asScala
      .map(Acl.fromZooKeeper)

    val barData = new String(curatorFramework.getData.forPath("/foo/bar"))
    val barAcl = curatorFramework.getACL
      .forPath("/foo/bar")
      .asScala
      .map(Acl.fromZooKeeper)

    assertResult("foo")(fooData)
    assertResult(fooAclDefault)(fooAcl)

    assertResult("bar")(barData)
    assertResult(barAclDefault)(barAcl)
  }

  it should "import node as a child of 'import' ZNode" in withCurator { curatorFramework =>
    val fooAclDefault =
      List(Acl(AclId("world", "anyone"), Set(Read, Create)))
    val barAclDefault =
      List(Acl(AclId("world", "anyone"), Set(Read, Create, Delete)))
    val bazAclDefault =
      List(Acl(AclId("world", "anyone"), Set(Read, Create, Write, Delete)))

    val exported =
      List(
        Cofree(
          getExportNode("/foo", "foo", fooAclDefault),
          Now(List(Cofree(getExportNode("/foo/bar", "bar", barAclDefault), Now(List.empty[Cofree[List, ZNodeExport]]))))
        ),
        Cofree(getExportNode("/baz", "baz", bazAclDefault), Now(List.empty[Cofree[List, ZNodeExport]]))
      )

    // create "import" container node
    curatorFramework.createContainers("/import")

    val action =
      ImportZNodesAction(ZNodePath.parse("/import").get, exported, curatorFramework)

    Await.result((new ImportZNodesActionHandler).handle(action).runToFuture, Duration.Inf)

    val fooData = new String(curatorFramework.getData.forPath("/import/foo"))
    val fooAcl = curatorFramework.getACL
      .forPath("/import/foo")
      .asScala
      .map(Acl.fromZooKeeper)

    val barData =
      new String(curatorFramework.getData.forPath("/import/foo/bar"))
    val barAcl = curatorFramework.getACL
      .forPath("/import/foo/bar")
      .asScala
      .map(Acl.fromZooKeeper)

    val bazData = new String(curatorFramework.getData.forPath("/import/baz"))
    val bazAcl = curatorFramework.getACL
      .forPath("/import/baz")
      .asScala
      .map(Acl.fromZooKeeper)

    assertResult("foo")(fooData)
    assertResult(fooAclDefault)(fooAcl)

    assertResult("bar")(barData)
    assertResult(barAclDefault)(barAcl)

    assertResult("baz")(bazData)
    assertResult(bazAclDefault)(bazAcl)
  }

  it should "import node creating its non-existent target parent" in withCurator { curatorFramework =>
    val fooAclDefault =
      List(Acl(AclId("world", "anyone"), Set(Read, Create)))

    val exported =
      List(Cofree(getExportNode("/foo", "foo", fooAclDefault), Now(List.empty[Cofree[List, ZNodeExport]])))

    val action =
      ImportZNodesAction(ZNodePath.parse("/non-existent-parent").get, exported, curatorFramework)

    Await.result((new ImportZNodesActionHandler).handle(action).runToFuture, Duration.Inf)

    val fooData = new String(curatorFramework.getData.forPath("/non-existent-parent/foo"))

    assertResult("foo")(fooData)
  }
}
