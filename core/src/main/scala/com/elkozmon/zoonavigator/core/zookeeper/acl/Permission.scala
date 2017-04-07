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

package com.elkozmon.zoonavigator.core.zookeeper.acl

import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import org.apache.zookeeper.ZooDefs.Perms

import scala.collection.mutable

sealed trait Permission

object Permission {

  final val All: Set[Permission] = Set(Create, Read, Write, Delete, Admin)

  case object Create extends Permission

  case object Read extends Permission

  case object Write extends Permission

  case object Delete extends Permission

  case object Admin extends Permission

  def toZookeeperMask(permissions: Set[Permission]): Int =
    permissions
      .map {
        case Permission.Create => Perms.CREATE
        case Permission.Read => Perms.READ
        case Permission.Write => Perms.WRITE
        case Permission.Delete => Perms.DELETE
        case Permission.Admin => Perms.ADMIN
      }
      .fold(0)(_ | _)

  def fromZookeeperMask(bitmask: Int): Set[Permission] =
    if (bitmask == Perms.ALL) {
      Permission.All
    } else {
      val mutableSet = mutable.Set.empty[Permission]

      if ((bitmask & Perms.CREATE) != 0) {
        mutableSet.add(Permission.Create).asUnit()
      }

      if ((bitmask & Perms.READ) != 0) {
        mutableSet.add(Permission.Read).asUnit()
      }

      if ((bitmask & Perms.WRITE) != 0) {
        mutableSet.add(Permission.Write).asUnit()
      }

      if ((bitmask & Perms.DELETE) != 0) {
        mutableSet.add(Permission.Delete).asUnit()
      }

      if ((bitmask & Perms.ADMIN) != 0) {
        mutableSet.add(Permission.Admin).asUnit()
      }

      mutableSet.toSet
    }
}
