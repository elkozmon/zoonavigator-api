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

sealed trait Scheme

object Scheme {

  object Zookeeper {
    private[Scheme] final val World = "world"
    private[Scheme] final val Auth = "auth"
    private[Scheme] final val Digest = "digest"
    private[Scheme] final val Ip = "ip"
  }

  case object World extends Scheme

  case object Auth extends Scheme

  case object Digest extends Scheme

  case object Ip extends Scheme

  def fromZookeeperScheme(scheme: String): Scheme =
    scheme match {
      case Zookeeper.World => Scheme.World
      case Zookeeper.Auth => Scheme.Auth
      case Zookeeper.Digest => Scheme.Digest
      case Zookeeper.Ip => Scheme.Ip
    }

  def toZookeeperScheme(scheme: Scheme): String =
    scheme match {
      case Scheme.World => Zookeeper.World
      case Scheme.Auth => Zookeeper.Auth
      case Scheme.Digest => Zookeeper.Digest
      case Scheme.Ip => Zookeeper.Ip
    }
}
