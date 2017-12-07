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

class ZNodePath private (val path: String) {

  private val pathAndNode = ZKPaths.getPathAndNode(path)

  val name: ZNodePathSegment = ZNodePathSegment(pathAndNode.getNode)

  lazy val parent: ZNodePath = ZNodePath.unsafe(pathAndNode.getPath)

  def down(name: String): Try[ZNodePath] =
    ZNodePath.parse(
      path
        .stripSuffix(ZKPaths.PATH_SEPARATOR)
        .concat(ZKPaths.PATH_SEPARATOR + name)
    )

  def down(name: ZNodePathSegment): ZNodePath =
    ZNodePath.unsafe(
      path
        .stripSuffix(ZKPaths.PATH_SEPARATOR)
        .concat(ZKPaths.PATH_SEPARATOR + name.string)
    )
}

object ZNodePath {

  def parse(path: String): Try[ZNodePath] = Try(unsafe(path))

  def unsafe(path: String): ZNodePath = new ZNodePath(path)
}
