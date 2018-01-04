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

package json.zookeeper

import play.api.libs.functional.syntax._
import play.api.libs.json.JsPath
import play.api.libs.json.Reads
import zookeeper.ConnectionParams

final case class JsonConnectionParams(underlying: ConnectionParams)

object JsonConnectionParams {

  implicit val connectionParamsReads: Reads[JsonConnectionParams] = (
    (JsPath \ "connectionString")
      .read[JsonConnectionString]
      .map(_.underlying) and
      (JsPath \ "authInfo").read[List[JsonAuthInfo]].map(_.map(_.underlying))
  )(ConnectionParams.apply _) map (JsonConnectionParams(_))
}
