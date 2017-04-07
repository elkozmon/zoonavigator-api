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

package json.zookeeper.acl

import com.elkozmon.zoonavigator.core.zookeeper.acl.Scheme
import play.api.libs.json._

import scala.language.implicitConversions

final case class JsonScheme(underlying: Scheme)

object JsonScheme {

  private final val World = "world"
  private final val Auth = "auth"
  private final val Digest = "digest"
  private final val Ip = "ip"

  private implicit def wrap(scheme: Scheme): JsonScheme =
    JsonScheme(scheme)

  implicit object SchemeFormat extends Format[JsonScheme] {
    override def reads(json: JsValue): JsResult[JsonScheme] =
      json match {
        case JsString(World) => JsSuccess(Scheme.World)
        case JsString(Auth) => JsSuccess(Scheme.Auth)
        case JsString(Digest) => JsSuccess(Scheme.Digest)
        case JsString(Ip) => JsSuccess(Scheme.Ip)
        case _ => JsError("Invalid scheme format")
      }

    override def writes(o: JsonScheme): JsValue =
      o.underlying match {
        case Scheme.World => JsString(World)
        case Scheme.Auth => JsString(Auth)
        case Scheme.Digest => JsString(Digest)
        case Scheme.Ip => JsString(Ip)
      }
  }

}
