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

package serialization.json.api

import akka.http.scaladsl.model.MediaTypes
import akka.util.ByteString
import api.ApiResponse
import play.api.http.Writeable
import play.api.libs.json._

trait JsonApiResponse {

  implicit def apiResponseWrites[T](
      implicit wrt: Writes[T]
  ): OWrites[ApiResponse[T]] =
    o =>
      Json.obj(
        "success" -> o.success,
        "message" -> o.message,
        "payload" -> o.payload.map(wrt.writes)
    )

  implicit def apiResponseWriteable[T](
      implicit wrt: Writes[T]
  ): Writeable[ApiResponse[T]] =
    new Writeable[ApiResponse[T]](
      o => ByteString(Json.toBytes(Json.toJsObject(o))),
      Some(MediaTypes.`application/json`.value)
    )
}
