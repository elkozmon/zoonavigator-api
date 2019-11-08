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

package com.elkozmon.zoonavigator.core.curator

import cats.Eval
import cats.free.Cofree
import cats.syntax.traverse._
import cats.instances.list._
import cats.instances.try_._
import com.elkozmon.zoonavigator.core.curator.Implicits._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.acl.AclId
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import monix.eval.Task
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.CuratorEvent
import org.apache.curator.utils.ZKPaths

import scala.jdk.CollectionConverters._
import scala.util.Try

trait CuratorOps {

  implicit class CuratorAsyncOps(curatorFramework: CuratorFramework) {

    def walkTreeAsync[T](
        fn: ZNodePath => Task[T]
    )(node: ZNodePath): Task[Cofree[List, T]] = {
      val taskT = fn(node)
      val taskChildren = for {
        paths <- getChildrenAsync(node).map(_.data.children)
        trees <- Task.wanderUnordered(paths)(walkTreeAsync(fn)).map(Eval.now)
      } yield trees

      Task.mapBoth(taskT, taskChildren)(Cofree(_, _))
    }

    def getZNodeAsync(path: ZNodePath): Task[ZNode] = {
      val taskAcl = getAclAsync(path)
      val taskData = getDataAsync(path)

      Task.mapBoth(taskAcl, taskData) { (acl, data) =>
        ZNode(acl.data, path, data.data, data.meta)
      }
    }

    def getZNodeWithChildrenAsync(path: ZNodePath): Task[ZNodeWithChildren] = {
      val taskZNode = getZNodeAsync(path)
      val taskZNodeChildren = getChildrenAsync(path).map(_.data)

      Task.mapBoth(taskZNode, taskZNodeChildren) { (node, children) =>
        ZNodeWithChildren(node, children)
      }
    }

    def getDataAsync(path: ZNodePath): Task[ZNodeMetaWith[ZNodeData]] =
      curatorFramework.getData
        .forPathAsync(path.path)
        .map { event =>
          ZNodeMetaWith(
            ZNodeData(Option(event.getData).getOrElse(Array.empty)),
            ZNodeMeta.fromStat(event.getStat)
          )
        }

    def getAclAsync(path: ZNodePath): Task[ZNodeMetaWith[ZNodeAcl]] =
      curatorFramework.getACL
        .forPathAsync(path.path)
        .map { event =>
          val acl = ZNodeAcl(
            event.getACLList.asScala.toList
              .map { acl =>
                Acl(
                  AclId(acl.getId.getScheme, acl.getId.getId),
                  Permission.fromZooKeeperMask(acl.getPerms)
                )
              }
          )

          val meta = ZNodeMeta.fromStat(event.getStat)

          ZNodeMetaWith(acl, meta)
        }

    def getChildrenAsync(
        path: ZNodePath
    ): Task[ZNodeMetaWith[ZNodeChildren]] = {
      def getChildrenFromEvent(event: CuratorEvent): Try[ZNodeChildren] =
        event.getChildren.asScala.toList
          .traverse { name =>
            val path = event.getPath
              .stripSuffix(ZKPaths.PATH_SEPARATOR)
              .concat(ZKPaths.PATH_SEPARATOR + name)

            ZNodePath.parse(path)
          }
          .map(ZNodeChildren)

      curatorFramework
        .getChildren
        .forPathAsync(path.path)
        .flatMap { event =>
          val meta = ZNodeMeta.fromStat(event.getStat)
          val childrenTask = Task.fromTry(getChildrenFromEvent(event))

          childrenTask.map(ZNodeMetaWith(_, meta))
        }
    }
  }
}
