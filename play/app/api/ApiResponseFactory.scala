/*
 * Copyright (C) 2019  Ľuboš Kozmon <https://www.elkozmon.com>
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

import cats.data.Reader
import play.api.http.Writeable
import play.api.mvc.Result

trait ApiResponseFactory {

  type GenericResult[T] = Reader[Writeable[ApiResponse[T]], Result]

  def okEmpty[T]: GenericResult[T]

  def okPayload[T](payload: T): GenericResult[T]

  def notFound[T](message: Option[String]): GenericResult[T]

  def unauthorized[T](message: Option[String]): GenericResult[T]

  def forbidden[T](message: Option[String]): GenericResult[T]

  def badRequest[T](message: Option[String]): GenericResult[T]

  def internalServerError[T](message: Option[String]): GenericResult[T]

  def fromThrowable[T](throwable: Throwable): GenericResult[T]
}
