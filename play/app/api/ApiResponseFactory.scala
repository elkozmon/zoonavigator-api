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

import play.api.libs.json.Writes
import play.api.mvc.Result

trait ApiResponseFactory {

  def okEmpty: Result

  def okPayload[T](payload: T)(implicit fmt: Writes[T]): Result

  def notFound(message: Option[String]): Result

  def forbidden(message: Option[String]): Result

  def badRequest(message: Option[String]): Result

  def internalServerError(message: Option[String]): Result

  def fromThrowable(throwable: Throwable): Result
}
