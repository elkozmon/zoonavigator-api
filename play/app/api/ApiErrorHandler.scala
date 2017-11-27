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

import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc._
import play.api.routing.Router
import play.core.SourceMapper

import scala.concurrent._

class ApiErrorHandler(
    env: Environment,
    config: Configuration,
    sourceMapper: Option[SourceMapper],
    router: => Option[Router],
    apiResponseFactory: ApiResponseFactory
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onProdServerError(
      request: RequestHeader,
      exception: UsefulException
  ): Future[Result] =
    Future.successful(
      apiResponseFactory.internalServerError(
        Some("A server error occurred: " + exception.getMessage)
      )
    )

  override protected def onNotFound(
      request: RequestHeader,
      message: String
  ): Future[Result] =
    Future.successful(
      apiResponseFactory.notFound(Some("Requested resource does not exist"))
    )
}
