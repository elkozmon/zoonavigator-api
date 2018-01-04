/*
 * Copyright (C) 2018  Ľuboš Kozmon
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

import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import play.api.libs.json._

import scala.language.implicitConversions

final case class JsonPermission(underlying: Permission)

object JsonPermission {

  private final val Create = "create"
  private final val Read = "read"
  private final val Write = "write"
  private final val Delete = "delete"
  private final val Admin = "admin"

  private implicit def wrap(permission: Permission): JsonPermission =
    JsonPermission(permission)

  implicit object PermissionFormat extends Format[JsonPermission] {
    override def reads(json: JsValue): JsResult[JsonPermission] =
      json match {
        case JsString(Create) => JsSuccess(Permission.Create)
        case JsString(Read)   => JsSuccess(Permission.Read)
        case JsString(Write)  => JsSuccess(Permission.Write)
        case JsString(Delete) => JsSuccess(Permission.Delete)
        case JsString(Admin)  => JsSuccess(Permission.Admin)
        case _                => JsError("Invalid permission format")
      }

    override def writes(o: JsonPermission): JsValue =
      o.underlying match {
        case Permission.Create => JsString(Create)
        case Permission.Read   => JsString(Read)
        case Permission.Write  => JsString(Write)
        case Permission.Delete => JsString(Delete)
        case Permission.Admin  => JsString(Admin)
      }
  }

}
