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

package controllers

import api.ApiResponse
import api.ApiResponseFactory
import api.exceptions.BadRequestException
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import curator.provider.CuratorFrameworkProvider
import monix.eval.Task
import monix.execution.Scheduler
import play.api.libs.json.JsSuccess
import play.api.mvc._
import serialization.Json._
import session.SessionToken
import session.manager.SessionManager
import zookeeper.ConnectionParams
import zookeeper.session.SessionInfo
import zookeeper.session.ZooKeeperSessionHelper

class ZSessionController(
    apiResponseFactory: ApiResponseFactory,
    zookeeperSessionHelper: ZooKeeperSessionHelper,
    curatorFrameworkProvider: CuratorFrameworkProvider,
    val controllerComponents: ControllerComponents,
    implicit val scheduler: Scheduler,
    implicit val sessionManager: SessionManager
) extends BaseController {

  def createSession(): Action[AnyContent] = Action.async { implicit request =>
    val actionTask: Task[SessionInfo] =
      request.body.asJson.map(_.validate[ConnectionParams]) match {
        case Some(JsSuccess(connectionParams, _)) =>
          implicit val sessionToken: SessionToken =
            sessionManager.newSession()

          // create curator framework
          curatorFrameworkProvider
            .getCuratorInstance(
              connectionParams.connectionString,
              connectionParams.authInfoList
            )
            .map { _ =>
              // store connection params to session
              zookeeperSessionHelper
                .setConnectionParams(connectionParams)
                .discard()

              SessionInfo(sessionToken, connectionParams.connectionString)
            }
        case _ =>
          Task.raiseError(
            new BadRequestException("Invalid request. Session info is missing.")
          )
      }

    val futureResultReader = actionTask
      .map(apiResponseFactory.okPayload)
      .onErrorHandle(apiResponseFactory.fromThrowable[SessionInfo])
      .runAsync

    render.async {
      case Accepts.Json() =>
        futureResultReader.map(_(ApiResponse.writeJson))
    }
  }

  def deleteSession(): Action[AnyContent] = Action { implicit request =>
    sessionManager
      .getSession(request)
      .foreach(sessionManager.closeSession()(_))

    val resultReader =
      apiResponseFactory.okEmpty

    render {
      case Accepts.Json() =>
        resultReader(ApiResponse.writeJson[Nothing])
    }
  }
}
