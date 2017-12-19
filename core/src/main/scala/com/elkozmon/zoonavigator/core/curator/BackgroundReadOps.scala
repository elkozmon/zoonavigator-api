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

package com.elkozmon.zoonavigator.core.curator

import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.acl.AclId
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.CuratorEvent

import scala.collection.JavaConverters._
import scala.util.Try
import cats.implicits._
import monix.eval.Task
import org.apache.curator.utils.ZKPaths

trait BackgroundReadOps extends BackgroundOps {

  implicit class CuratorBackgroundReadOps(curatorFramework: CuratorFramework) {

    /**
      * @param node path to the root node of the tree to be fetched
      */
    def getTreeBackground(node: ZNodePath): Task[ZNode] = {
      val futureAcl = getAclBackground(node)
      val futureData = getDataBackground(node)
      val futureChildren = getChildrenBackground(node)
        .map(_.data.children)
        .flatMap(Task.traverse(_)(getTreeBackground))

      for {
        metaAcl <- futureAcl
        metaData <- futureData
        children <- futureChildren
      } yield ZNode(metaAcl.data, node, metaData.data, metaData.meta, children)
    }

    def getDataBackground(path: ZNodePath): Task[ZNodeMetaWith[ZNodeData]] =
      curatorFramework.getData
        .forPathBackground(path.path)
        .map { event =>
          ZNodeMetaWith(
            ZNodeData(Option(event.getData).getOrElse(Array.empty)),
            ZNodeMeta.fromStat(event.getStat)
          )
        }

    def getAclBackground(path: ZNodePath): Task[ZNodeMetaWith[ZNodeAcl]] =
      curatorFramework.getACL
        .forPathBackground(path.path)
        .map { event =>
          val acl = ZNodeAcl(
            event.getACLList.asScala.toList
              .map { acl =>
                Acl(
                  AclId(acl.getId.getScheme, acl.getId.getId),
                  Permission.fromZookeeperMask(acl.getPerms)
                )
              }
          )

          val meta = ZNodeMeta.fromStat(event.getStat)

          ZNodeMetaWith(acl, meta)
        }

    def getChildrenBackground(
        path: ZNodePath
    ): Task[ZNodeMetaWith[ZNodeChildren]] = {
      def getChildrenFromEvent(event: CuratorEvent): Try[ZNodeChildren] =
        event.getChildren.asScala.toList
          .traverseU { name =>
            val path = event.getPath
              .stripSuffix(ZKPaths.PATH_SEPARATOR)
              .concat(ZKPaths.PATH_SEPARATOR + name)

            ZNodePath.parse(path)
          }
          .map(ZNodeChildren)

      for {
        event <- curatorFramework.getChildren.forPathBackground(path.path)
        children <- Task.fromTry(getChildrenFromEvent(event))
      } yield ZNodeMetaWith(children, ZNodeMeta.fromStat(event.getStat))
    }
  }
}
