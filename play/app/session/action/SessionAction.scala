/*
 * Copyright (C) 2019  Ľuboš Kozmon
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

package session.action

import api.ApiResponse
import api.ApiResponseFactory
import play.api.http.Writeable
import play.api.mvc._
import session.manager.SessionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SessionAction[B](
    apiResponseFactory: ApiResponseFactory,
    sessionManager: SessionManager,
    val parser: BodyParser[B]
)(
    implicit val executionContext: ExecutionContext,
    apiResponseWriter: Writeable[ApiResponse[String]]
) extends ActionBuilder[SessionRequest, B]
    with ActionRefiner[Request, SessionRequest] {

  override protected def refine[A](
      request: Request[A]
  ): Future[Either[Result, SessionRequest[A]]] =
    Future.successful[Either[Result, SessionRequest[A]]](
      sessionManager
        .getSession(request)
        .toRight(
          apiResponseFactory
            .unauthorized(Some("Session has expired."))
            .apply(ApiResponse.writeJson[Nothing])
        )
        .right
        .map(new SessionRequest(_, sessionManager, request))
    )
}
