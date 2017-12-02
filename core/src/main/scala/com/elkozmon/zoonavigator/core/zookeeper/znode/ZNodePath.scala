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

package com.elkozmon.zoonavigator.core.zookeeper.znode

import org.apache.curator.utils.ZKPaths

import scala.util.Try

final case class ZNodePath(path: String) {

  lazy val name: Try[String] = pathAndNode.map(_.getNode)

  lazy val parent: Try[ZNodePath] = pathAndNode.map(p => ZNodePath(p.getPath))

  def down(name: String): ZNodePath =
    ZNodePath(
      path
        .stripSuffix(ZKPaths.PATH_SEPARATOR)
        .concat(ZKPaths.PATH_SEPARATOR + name)
    )

  private lazy val pathAndNode = Try(ZKPaths.getPathAndNode(path))
}
