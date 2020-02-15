/*
 * Copyright (C) 2020  Ľuboš Kozmon <https://www.elkozmon.com>
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

package api.controllers

import api.ApiResponse
import api.exceptions.BadRequestException
import api.exceptions.HttpException
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import curator.provider.CuratorFrameworkProvider
import monix.eval.Task
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.mvc._
import schedulers.ComputingScheduler
import session.SessionToken
import session.manager.SessionManager
import zookeeper.ConnectionParams
import zookeeper.session.SessionInfo
import zookeeper.session.ZooKeeperSessionHelper

class ZSessionController(
    httpErrorHandler: HttpErrorHandler,
    zookeeperSessionHelper: ZooKeeperSessionHelper,
    curatorFrameworkProvider: CuratorFrameworkProvider,
    computingScheduler: ComputingScheduler,
    controllerComponents: ControllerComponents,
    implicit val sessionManager: SessionManager
) extends AbstractController(controllerComponents) {

  import api.formats.Json._

  def createSession(): Action[AnyContent] = Action.async { implicit request =>
    val actionTask: Task[SessionInfo] =
      request.body.asJson.map(_.validate[ConnectionParams]) match {
        case Some(JsSuccess(connectionParams, _)) =>
          implicit val sessionToken: SessionToken =
            sessionManager.newSession()

          // create curator framework
          curatorFrameworkProvider
            .getCuratorInstance(connectionParams.connectionString, connectionParams.authInfoList)
            .map { _ =>
              // store connection params to session
              zookeeperSessionHelper
                .setConnectionParams(connectionParams)
                .discard()

              SessionInfo(sessionToken, connectionParams.connectionString)
            }
        case _ =>
          Task.raiseError(new BadRequestException("Invalid request. Session info is missing."))
      }

    val futureApiResponse = actionTask
      .map(ApiResponse.success(_))
      .executeOn(computingScheduler)
      .runToFuture(computingScheduler)

    render.async {
      case Accepts.Json() =>
        import computingScheduler.scheduler

        futureApiResponse
          .map(apiResponse => Ok(Json.toJson(apiResponse)))
          .recoverWith(HttpException.resultHandler(request, httpErrorHandler))
    }
  }

  def deleteSession(): Action[AnyContent] = Action { implicit request =>
    sessionManager
      .getSession(request)
      .foreach(sessionManager.closeSession()(_))

    render {
      case Accepts.Json() =>
        Ok(Json.toJson(ApiResponse.successEmpty))
    }
  }
}
