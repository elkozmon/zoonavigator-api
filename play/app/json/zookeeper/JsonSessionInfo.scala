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

import play.api.libs.json.{JsValue, Json, Writes}
import zookeeper.session.SessionInfo

final case class JsonSessionInfo(underlying: SessionInfo)

object JsonSessionInfo {

  implicit object SessionInfoWrites extends Writes[JsonSessionInfo] {
    override def writes(o: JsonSessionInfo): JsValue =
      Json.obj(
        "token" -> JsonSessionToken(o.underlying.token),
        "connectionString" -> JsonConnectionString(o.underlying.connectionString)
      )
  }

}
