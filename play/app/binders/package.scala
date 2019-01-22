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

import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeAclVersion
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeDataVersion
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import play.api.mvc.QueryStringBindable

/**
  * TODO custom errors (https://github.com/playframework/playframework/issues/8459)
  */
package object binders {

  implicit def zNodePathBinder(
      implicit string: QueryStringBindable[String]
  ): QueryStringBindable[ZNodePath] = new QueryStringBindable[ZNodePath] {
    override def bind(
        key: String,
        params: Map[String, Seq[String]]
    ): Option[Either[String, ZNodePath]] =
      string
        .bind(key, params)
        .map(_.flatMap(ZNodePath.parse(_).toEither.left.map(_.getMessage)))

    override def unbind(key: String, value: ZNodePath): String = value.path
  }

  implicit def zNodeAclVersionBinder(
      implicit long: QueryStringBindable[Long]
  ): QueryStringBindable[ZNodeAclVersion] =
    new QueryStringBindable[ZNodeAclVersion] {
      override def bind(
          key: String,
          params: Map[String, Seq[String]]
      ): Option[Either[String, ZNodeAclVersion]] =
        long.bind(key, params).map(_.map(ZNodeAclVersion))

      override def unbind(key: String, value: ZNodeAclVersion): String =
        value.version.toString
    }

  implicit def zNodeDataVersionBinder(
      implicit long: QueryStringBindable[Long]
  ): QueryStringBindable[ZNodeDataVersion] =
    new QueryStringBindable[ZNodeDataVersion] {
      override def bind(
          key: String,
          params: Map[String, Seq[String]]
      ): Option[Either[String, ZNodeDataVersion]] =
        long.bind(key, params).map(_.map(ZNodeDataVersion))

      override def unbind(key: String, value: ZNodeDataVersion): String =
        value.version.toString
    }
}
