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

import java.nio.charset.StandardCharsets

import modules.action.ActionModule
import api.ApiResponse
import cats.implicits._
import com.elkozmon.zoonavigator.core.action.actions._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import api.ApiResponseFactory
import api.exceptions.BadRequestException
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import curator.action.CuratorActionBuilder
import curator.action.CuratorRequest
import monix.eval.Task
import monix.execution.Scheduler
import play.api.http.Writeable
import play.api.libs.json._
import play.api.mvc._
import serialization.Json._
import session.action.SessionActionBuilder

class ZNodeController(
    apiResponseFactory: ApiResponseFactory,
    curatorActionBuilder: CuratorActionBuilder,
    sessionActionBuilder: SessionActionBuilder,
    actionModule: ActionModule,
    playBodyParsers: PlayBodyParsers,
    val controllerComponents: ControllerComponents,
    implicit val scheduler: Scheduler
) extends BaseController {

  private val actionDispatcherProvider = actionModule.actionDispatcherProvider

  def getNode: Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader =
        getRequiredQueryParam("path")
          .flatMap(parseZNodePath)
          .flatMap { path =>
            actionDispatcherProvider
              .getDispatcher(curatorRequest.curatorFramework)
              .dispatch(GetZNodeWithChildrenAction(path))
          }
          .map(apiResponseFactory.okPayload)
          .onErrorHandle(apiResponseFactory.fromThrowable[ZNodeWithChildren])
          .runAsync

      render.async {
        case Accepts.Json =>
          futureResultReader.map(_(ApiResponse.writeJson))
      }
    }

  def getChildrenNodes: Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader =
        getRequiredQueryParam("path")
          .flatMap(parseZNodePath)
          .flatMap { path =>
            actionDispatcherProvider
              .getDispatcher(curatorRequest.curatorFramework)
              .dispatch(GetZNodeChildrenAction(path))
          }
          .map(apiResponseFactory.okPayload)
          .onErrorHandle(
            apiResponseFactory.fromThrowable[ZNodeMetaWith[ZNodeChildren]]
          )
          .runAsync

      render.async {
        case Accepts.Json =>
          futureResultReader.map(
            _(
              ApiResponse.writeJson(
                apiResponseWrites(zNodeMetaWithWrites(zNodeChildrenWrites))
              )
            )
          )
      }
    }

  def createNode(): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = getRequiredQueryParam("path")
        .flatMap(parseZNodePath)
        .flatMap { path =>
          actionDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(CreateZNodeAction(path))
        }
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync

      render.async {
        case Accepts.Json =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def duplicateNode(): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val actionTask = for {
        source <- getRequiredQueryParam("source").flatMap(parseZNodePath)
        destin <- getRequiredQueryParam("destination").flatMap(parseZNodePath)
        action <- actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(DuplicateZNodeRecursiveAction(source, destin))
      } yield action

      val futureResultReader = actionTask
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync

      render.async {
        case Accepts.Json =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def moveNode(): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val actionTask = for {
        source <- getRequiredQueryParam("source").flatMap(parseZNodePath)
        destin <- getRequiredQueryParam("destination").flatMap(parseZNodePath)
        action <- actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(MoveZNodeRecursiveAction(source, destin))
      } yield action

      val futureResultReader = actionTask
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync

      render.async {
        case Accepts.Json =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def deleteNode(): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val actionTask = for {
        path    <- getRequiredQueryParam("path").flatMap(parseZNodePath)
        version <- getRequiredQueryParam("version").map(_.toLong)
        action <- actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(DeleteZNodeRecursiveAction(path, ZNodeDataVersion(version)))
      } yield action

      val futureResultReader = actionTask
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync

      render.async {
        case Accepts.Json =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def deleteChildrenNodes(): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val actionTask = for {
        path  <- getRequiredQueryParam("path").flatMap(parseZNodePath)
        names <- getRequiredQueryParam("names").map(_.split("/"))
        paths <- Task.fromTry(names.toList.traverseU(path.down))
        action <- actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(ForceDeleteZNodeRecursiveAction(paths))
      } yield action

      val futureResultReader = actionTask
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync

      render.async {
        case Accepts.Json =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def updateAcl(): Action[JsValue] =
    newCuratorAction(playBodyParsers.json).async { implicit curatorRequest =>
      val actionTask = for {
        path    <- getRequiredQueryParam("path").flatMap(parseZNodePath)
        version <- getRequiredQueryParam("version").map(_.toLong)
        aclList <- parseRequestBodyJson[List[Acl]]
        recursive = curatorRequest.getQueryString("recursive").isDefined
        action <- if (recursive) {
          actionDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(
              UpdateZNodeAclListRecursiveAction(
                path,
                ZNodeAcl(aclList),
                ZNodeAclVersion(version)
              )
            )
        } else {
          actionDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(
              UpdateZNodeAclListAction(
                path,
                ZNodeAcl(aclList),
                ZNodeAclVersion(version)
              )
            )
        }
      } yield action

      val futureResultReader = actionTask
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(apiResponseFactory.fromThrowable[ZNodeMeta])
        .runAsync

      render.async {
        case Accepts.Json =>
          futureResultReader.map(_(ApiResponse.writeJson))
      }
    }

  def updateData(): Action[String] =
    newCuratorAction(playBodyParsers.text).async { implicit curatorRequest =>
      val actionTask = for {
        path    <- getRequiredQueryParam("path").flatMap(parseZNodePath)
        version <- getRequiredQueryParam("version").map(_.toLong)
        action <- actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(
            UpdateZNodeDataAction(
              path,
              ZNodeData(curatorRequest.body.getBytes(StandardCharsets.UTF_8)),
              ZNodeDataVersion(version)
            )
          )
      } yield action

      val futureResultReader = actionTask
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(apiResponseFactory.fromThrowable[ZNodeMeta])
        .runAsync

      render.async {
        case Accepts.Json =>
          futureResultReader.map(_(ApiResponse.writeJson))
      }
    }

  def exportNodes(): Action[String] = ???

  def importNodes(): Action[String] = ???

  private def newCuratorAction[B](bodyParser: BodyParser[B])(
      implicit wrt: Writeable[ApiResponse[String]]
  ): ActionBuilder[CuratorRequest, B] =
    sessionActionBuilder(bodyParser) andThen curatorActionBuilder()

  private def getRequiredQueryParam(
      name: String
  )(implicit request: Request[_]): Task[String] =
    Task.fromTry(
      request
        .getQueryString(name)
        .toRight(
          new BadRequestException(s"Missing '$name' query string parameter")
        )
        .toTry
    )

  private def parseZNodePath(path: String): Task[ZNodePath] =
    Task.fromTry(ZNodePath.parse(path))

  private def parseRequestBodyJson[T](
      implicit request: Request[JsValue],
      reads: Reads[T]
  ): Task[T] =
    request.body.validateOpt[T] match {
      case JsSuccess(Some(value), _) =>
        Task.now(value)
      case JsError(_) =>
        Task.raiseError(new BadRequestException("Malformed request body"))
      case JsSuccess(None, _) =>
        Task.raiseError(new BadRequestException("Missing request body"))
    }
}
