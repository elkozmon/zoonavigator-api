/*
 * Copyright (C) 2020  Ľuboš Kozmon <https://www.elkozmon.com>
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

import java.nio.charset.StandardCharsets
import java.util

final case class ZNodeData(bytes: Array[Byte]) {

  override def hashCode(): Int =
    util.Arrays.hashCode(bytes)

  override def equals(other: scala.Any): Boolean =
    other match {
      case that: ZNodeData =>
        (that canEqual this) && bytes.sameElements(that.bytes)
      case _ =>
        false
    }

  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[ZNodeData]

  override def toString: String =
    s"ZNodeData(${new String(bytes, StandardCharsets.UTF_8)})"
}
