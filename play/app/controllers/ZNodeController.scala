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

package controllers

import java.util.Base64

import api.ApiResponse
import api.ApiResponseFactory
import api.exceptions.BadRequestException
import cats.free.Cofree
import cats.implicits._
import com.elkozmon.zoonavigator.core.action.actions._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import curator.action.CuratorActionBuilder
import curator.action.CuratorRequest
import modules.action.ActionModule
import monix.eval.Task
import monix.execution.Scheduler
import play.api.http.Writeable
import play.api.libs.json._
import play.api.mvc._
import serialization.Json._
import session.action.SessionActionBuilder

import scala.concurrent.Future

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

  def getNode(path: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(GetZNodeWithChildrenAction(path))
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(apiResponseFactory.fromThrowable[ZNodeWithChildren])
        .runAsync

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson))
      }
    }

  def getChildrenNodes(path: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(GetZNodeChildrenAction(path))
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(
          apiResponseFactory.fromThrowable[ZNodeMetaWith[ZNodeChildren]]
        )
        .runAsync

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(
            _(
              ApiResponse.writeJson(
                apiResponseWrites(zNodeMetaWithWrites(zNodeChildrenWrites))
              )
            )
          )
      }
    }

  def createNode(path: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(CreateZNodeAction(path))
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def duplicateNode(source: ZNodePath, destination: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(DuplicateZNodeRecursiveAction(source, destination))
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def moveNode(source: ZNodePath, destination: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(MoveZNodeRecursiveAction(source, destination))
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def deleteNode(path: ZNodePath, version: ZNodeDataVersion): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(DeleteZNodeRecursiveAction(path, version))
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def deleteChildrenNodes(path: ZNodePath, names: List[String]): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = names
        .traverseU(path.down)
        .toEither
        .left
        .map(apiResponseFactory.fromThrowable)
        .fold(
          Future.successful, { paths =>
            actionDispatcherProvider
              .getDispatcher(curatorRequest.curatorFramework)
              .dispatch(ForceDeleteZNodeRecursiveAction(paths))
              .map(_ => apiResponseFactory.okEmpty)
              .onErrorHandle(apiResponseFactory.fromThrowable)
              .runAsync
          }
        )

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def updateAcl(
      path: ZNodePath,
      version: ZNodeAclVersion,
      recursive: Option[Boolean]
  ): Action[JsValue] =
    newCuratorAction(playBodyParsers.json).async { implicit curatorRequest =>
      val futureResultReader = parseRequestBodyJson[List[Acl]].flatMap {
        jsonAclList =>
          val taskMeta: Task[ZNodeMeta] =
            if (recursive.contains(true)) {
              actionDispatcherProvider
                .getDispatcher(curatorRequest.curatorFramework)
                .dispatch(
                  UpdateZNodeAclListRecursiveAction(
                    path,
                    ZNodeAcl(jsonAclList),
                    version
                  )
                )
            } else {
              actionDispatcherProvider
                .getDispatcher(curatorRequest.curatorFramework)
                .dispatch(
                  UpdateZNodeAclListAction(path, ZNodeAcl(jsonAclList), version)
                )
            }

          taskMeta
            .map(apiResponseFactory.okPayload)
            .onErrorHandle(apiResponseFactory.fromThrowable[ZNodeMeta])
      }.runAsync

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson))
      }
    }

  // TODO use json in body to reuse json znode data reader
  def updateData(path: ZNodePath, version: ZNodeDataVersion): Action[String] =
    newCuratorAction(playBodyParsers.text).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(
          UpdateZNodeDataAction(
            path,
            ZNodeData(Base64.getDecoder.decode(curatorRequest.body)),
            version
          )
        )
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(apiResponseFactory.fromThrowable[ZNodeMeta])
        .runAsync

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson))
      }
    }

  def getExportNodes(paths: List[ZNodePath]): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(ExportZNodesAction(paths))
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(apiResponseFactory.fromThrowable[List[Cofree[List, ZNodeExport]]])
        .runAsync

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson))
      }
    }

  def importNodes(path: ZNodePath): Action[JsValue] =
    newCuratorAction(playBodyParsers.json).async { implicit curatorRequest =>
      val futureResultReader =
        parseRequestBodyJson[List[Cofree[List, ZNodeExport]]].flatMap {
          exportZNodes =>
            actionDispatcherProvider
              .getDispatcher(curatorRequest.curatorFramework)
              .dispatch(ImportZNodesAction(path, exportZNodes))
              .map(_ => apiResponseFactory.okEmpty)
              .onErrorHandle(apiResponseFactory.fromThrowable)
        }.runAsync

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  private def newCuratorAction[B](bodyParser: BodyParser[B])(
      implicit wrt: Writeable[ApiResponse[String]]
  ): ActionBuilder[CuratorRequest, B] =
    sessionActionBuilder(bodyParser) andThen curatorActionBuilder()

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
