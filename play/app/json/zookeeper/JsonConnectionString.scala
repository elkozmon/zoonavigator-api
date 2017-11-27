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

package json.zookeeper

import play.api.libs.json._
import zookeeper.ConnectionString

import scala.language.implicitConversions

final case class JsonConnectionString(underlying: ConnectionString)

object JsonConnectionString {

  private implicit def wrap(
      connectionString: ConnectionString
  ): JsonConnectionString =
    JsonConnectionString(connectionString)

  implicit object ConnectionStringFormat extends Format[JsonConnectionString] {
    override def reads(json: JsValue): JsResult[JsonConnectionString] =
      json match {
        case JsString(connectionString) =>
          JsSuccess(ConnectionString(connectionString))
        case _ =>
          JsError("Invalid connection string format")
      }

    override def writes(o: JsonConnectionString): JsValue =
      JsString(o.underlying.string)
  }

}
