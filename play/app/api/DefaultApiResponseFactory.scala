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

package api

import org.apache.zookeeper.KeeperException.NoAuthException
import play.api.http.Writeable
import play.api.mvc.Result
import play.api.mvc.Results._

class DefaultApiResponseFactory extends ApiResponseFactory {

  override def okEmpty(implicit wrt: Writeable[ApiResponse[String]]): Result =
    Ok(ApiResponse(success = true, message = None, Option.empty[String]))

  override def okPayload[T](
      payload: T
  )(implicit wrt: Writeable[ApiResponse[T]]): Result =
    Ok(ApiResponse(success = true, message = None, Some(payload)))

  override def notFound(
      message: Option[String]
  )(implicit wrt: Writeable[ApiResponse[String]]): Result =
    NotFound(failureResponse(message))

  override def unauthorized(
      message: Option[String]
  )(implicit wrt: Writeable[ApiResponse[String]]): Result =
    Unauthorized(failureResponse(message))
      .withHeaders(("WWW-Authenticate", "Digest"))

  override def forbidden(
      message: Option[String]
  )(implicit wrt: Writeable[ApiResponse[String]]): Result =
    Forbidden(failureResponse(message))

  override def badRequest(
      message: Option[String]
  )(implicit wrt: Writeable[ApiResponse[String]]): Result =
    BadRequest(failureResponse(message))

  override def internalServerError(
      message: Option[String]
  )(implicit wrt: Writeable[ApiResponse[String]]): Result =
    InternalServerError(failureResponse(message))

  override def fromThrowable(
      throwable: Throwable
  )(implicit wrt: Writeable[ApiResponse[String]]): Result = {
    val apiResponse = failureResponse(Some(throwable.getMessage))

    throwable match {
      case _: NoAuthException =>
        Forbidden(apiResponse)
      case _ =>
        InternalServerError(apiResponse)
    }
  }

  private def failureResponse(message: Option[String]): ApiResponse[String] =
    ApiResponse(success = false, message = message, Option.empty[String])
}
