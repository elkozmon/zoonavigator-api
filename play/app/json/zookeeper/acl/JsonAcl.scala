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

import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.acl.AclId
import play.api.libs.functional.syntax._
import play.api.libs.json._

final case class JsonAcl(underlying: Acl)

object JsonAcl {

  private final val IdKey = "id"
  private final val SchemeKey = "scheme"
  private final val PermissionsKey = "permissions"

  implicit object AclFormat extends Format[JsonAcl] {

    private implicit val aclIdReads: Reads[AclId] = (
      (JsPath \ SchemeKey).read[String] and
        (JsPath \ IdKey).read[String]
    )(AclId.apply _)

    private implicit val aclReads: Reads[Acl] = (
      JsPath.read[AclId] and
        (JsPath \ PermissionsKey)
          .read[List[JsonPermission]]
          .map(_.map(_.underlying).toSet)
    )(Acl.apply _)

    override def reads(json: JsValue): JsResult[JsonAcl] =
      json.validate[Acl].map(JsonAcl(_))

    override def writes(o: JsonAcl): JsValue = {
      val jsonPermissions = o.underlying.permissions.toList
        .map(perm => JsonPermission(perm))

      Json.obj(
        IdKey -> o.underlying.aclId.id.mkString,
        SchemeKey -> o.underlying.aclId.scheme,
        PermissionsKey -> Json.toJson(jsonPermissions)
      )
    }
  }

}
