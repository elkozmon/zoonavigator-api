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

import java.nio.charset.StandardCharsets

import json.zookeeper.acl.JsonScheme
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}
import zookeeper.AuthInfo

final case class JsonAuthInfo(underlying: AuthInfo)

object JsonAuthInfo {

  implicit val authInfoReads: Reads[JsonAuthInfo] = (
    (JsPath \ "scheme").read[JsonScheme].map(_.underlying) and
      (JsPath \ "id").read[String].map(_.getBytes(StandardCharsets.UTF_8))
    ) (AuthInfo.apply _) map (JsonAuthInfo(_))
}
