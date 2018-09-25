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

import api.exceptions.BadRequestException
import cats.data.Reader
import org.apache.zookeeper.KeeperException.NoAuthException
import play.api.mvc.Results._

class DefaultApiResponseFactory extends ApiResponseFactory {

  override def okEmpty[T]: GenericResult[T] =
    Reader(Ok(ApiResponse(success = true, message = None, Option.empty[T]))(_))

  override def okPayload[T](payload: T): GenericResult[T] =
    Reader(Ok(ApiResponse(success = true, message = None, Some(payload)))(_))

  override def notFound[T](message: Option[String]): GenericResult[T] =
    Reader(NotFound(failureResponse[T](message))(_))

  override def unauthorized[T](message: Option[String]): GenericResult[T] =
    Reader(Unauthorized(failureResponse[T](message))(_).withHeaders(("WWW-Authenticate", "Digest")))

  override def forbidden[T](message: Option[String]): GenericResult[T] =
    Reader(Forbidden(failureResponse[T](message))(_))

  override def badRequest[T](message: Option[String]): GenericResult[T] =
    Reader(BadRequest(failureResponse[T](message))(_))

  override def internalServerError[T](message: Option[String]): GenericResult[T] =
    Reader(InternalServerError(failureResponse[T](message))(_))

  override def fromThrowable[T](throwable: Throwable): GenericResult[T] = {
    val apiResponse = failureResponse[T](Some(throwable.getMessage))

    throwable match {
      case _: NoAuthException =>
        Reader(Forbidden(apiResponse)(_))
      case _: BadRequestException =>
        Reader(BadRequest(apiResponse)(_))
      case _ =>
        Reader(InternalServerError(apiResponse)(_))
    }
  }

  private def failureResponse[T](message: Option[String]): ApiResponse[T] =
    ApiResponse(success = false, message = message, None)
}
