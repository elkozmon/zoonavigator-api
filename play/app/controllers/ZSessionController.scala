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

package controllers

import api.ApiResponseFactory
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import curator.provider.CuratorFrameworkProvider
import monix.execution.Scheduler
import play.api.libs.json.JsSuccess
import play.api.mvc._
import serialization.Json._
import session.SessionToken
import session.manager.SessionManager
import zookeeper.ConnectionParams
import zookeeper.session.{SessionInfo, ZooKeeperSessionHelper}

import scala.concurrent.Future

class ZSessionController(
    apiResponseFactory: ApiResponseFactory,
    zookeeperSessionHelper: ZooKeeperSessionHelper,
    curatorFrameworkProvider: CuratorFrameworkProvider,
    val controllerComponents: ControllerComponents,
    implicit val scheduler: Scheduler,
    implicit val sessionManager: SessionManager
) extends BaseController {

  def create(): Action[AnyContent] = Action.async { request =>
    import api.ApiResponse.writeJson

    request.body.asJson.map(_.validate[ConnectionParams]) match {
      case Some(JsSuccess(connectionParams, _)) =>
        implicit val sessionToken: SessionToken = sessionManager.newSession()

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

            val sessionInfo =
              SessionInfo(sessionToken, connectionParams.connectionString)

            apiResponseFactory.okPayload(sessionInfo)
          }
          .onErrorHandle(apiResponseFactory.fromThrowable)
          .runAsync
      case _ =>
        Future.successful(
          apiResponseFactory
            .badRequest(Some("Invalid request. Session info is missing."))
        )
    }
  }

  def delete() = Action { request =>
    import api.ApiResponse.writeJson

    sessionManager
      .getSession(request)
      .foreach(sessionManager.closeSession()(_))

    apiResponseFactory.okEmpty
  }
}
