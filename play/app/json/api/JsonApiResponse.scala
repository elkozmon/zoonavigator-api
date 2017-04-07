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

package json.api

import api.ApiResponse
import play.api.libs.json.{JsValue, Json, Writes}

final case class JsonApiResponse[T](underlying: ApiResponse[T])(implicit val fmt: Writes[T])

object JsonApiResponse {

  implicit def apiResponseWrites[T]: Writes[JsonApiResponse[T]] =
    new Writes[JsonApiResponse[T]] {
      override def writes(o: JsonApiResponse[T]): JsValue = {
        Json.obj(
          "success" -> o.underlying.success,
          "message" -> o.underlying.message,
          "payload" -> o.underlying.payload.map(o.fmt.writes)
        )
      }
    }

}
