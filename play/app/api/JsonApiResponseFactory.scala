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

package api

import json.api.JsonApiResponse
import org.apache.zookeeper.KeeperException.NoAuthException
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.Result
import play.api.mvc.Results._

class JsonApiResponseFactory extends ApiResponseFactory {

  override def okEmpty: Result =
    Ok(
      Json.toJson(
        JsonApiResponse(
          ApiResponse(
            success = true,
            message = None,
            payload = Option.empty[String]
          )
        )
      )
    )

  override def okPayload[T](payload: T)(implicit fmt: Writes[T]): Result =
    Ok(
      Json.toJson(
        JsonApiResponse(
          ApiResponse(success = true, message = None, payload = Some(payload))
        )
      )
    )

  override def notFound(message: Option[String]): Result =
    NotFound(failureResponse(message))

  override def unauthorized(message: Option[String]): Result =
    Unauthorized(failureResponse(message))
      .withHeaders(("WWW-Authenticate", "Digest"))

  override def forbidden(message: Option[String]): Result =
    Forbidden(failureResponse(message))

  override def badRequest(message: Option[String]): Result =
    BadRequest(failureResponse(message))

  override def internalServerError(message: Option[String]): Result =
    InternalServerError(failureResponse(message))

  override def fromThrowable(throwable: Throwable): Result = {
    val jsonApiResponse = failureResponse(Some(throwable.getMessage))

    throwable match {
      case _: NoAuthException =>
        Forbidden(jsonApiResponse)
      case _ =>
        InternalServerError(jsonApiResponse)
    }
  }

  private def failureResponse(message: Option[String]): JsValue =
    Json.toJson(
      JsonApiResponse(
        ApiResponse(
          success = false,
          message = message,
          payload = Option.empty[String]
        )
      )
    )
}
