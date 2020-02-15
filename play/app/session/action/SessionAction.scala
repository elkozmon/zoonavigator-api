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

package session.action

import cats.instances.either._
import cats.instances.future._
import cats.syntax.traverse._
import play.api.http.HttpErrorHandler
import play.api.mvc._
import session.manager.SessionManager

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SessionAction[B](httpErrorHandler: HttpErrorHandler, sessionManager: SessionManager, val parser: BodyParser[B])(
    implicit val executionContext: ExecutionContext
) extends ActionBuilder[SessionRequest, B]
    with ActionRefiner[Request, SessionRequest] {

  override protected def refine[A](request: Request[A]): Future[Either[Result, SessionRequest[A]]] =
    sessionManager
      .getSession(request)
      .map(new SessionRequest(_, sessionManager, request))
      .toLeft(httpErrorHandler.onClientError(request, 401, "Session has expired."))
      .sequence
      .map(_.swap)
}
