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

package curator.action

import api.ApiResponseFactory
import cats.implicits._
import curator.provider.CuratorFrameworkProvider
import monix.execution.Scheduler
import play.api.mvc._
import session.action.SessionRequest
import zookeeper.ConnectionParams
import zookeeper.session.ZooKeeperSessionHelper

import scala.concurrent.Future

class CuratorAction(
    apiResponseFactory: ApiResponseFactory,
    zookeeperSessionHelper: ZooKeeperSessionHelper,
    curatorFrameworkProvider: CuratorFrameworkProvider
)(implicit val executionContext: Scheduler)
    extends ActionRefiner[SessionRequest, CuratorRequest] {

  override protected def refine[A](
      request: SessionRequest[A]
  ): Future[Either[Result, CuratorRequest[A]]] = {
    val futureOrFuture: Either[Future[Result], Future[CuratorRequest[A]]] =
      zookeeperSessionHelper
        .getConnectionParams(request.sessionToken, request.sessionManager)
        .toRight(
          Future.successful(
            apiResponseFactory.unauthorized(Some("Session was lost."))
          )
        )
        .right
        .map {
          case ConnectionParams(connectionString, authInfoList) =>
            curatorFrameworkProvider
              .getCuratorInstance(connectionString, authInfoList)
              .map(new CuratorRequest(_, request))
              .runAsync
        }

    futureOrFuture.bisequence
  }
}
