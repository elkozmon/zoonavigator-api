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

import java.nio.charset.StandardCharsets
import java.util.Base64

import akka.util.ByteString
import api.{ApiResponse, ApiResponseFactory}
import api.exceptions.BadRequestException
import cats.free.Cofree
import cats.instances.list._
import cats.instances.try_._
import cats.syntax.traverse._
import com.elkozmon.zoonavigator.core.action.actions._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import curator.action.{CuratorActionBuilder, CuratorRequest}
import modules.action.ActionModule
import monix.eval.Task
import monix.execution.Scheduler
import play.api.http.Writeable
import play.api.libs.json._
import play.api.mvc._
import serialization.Json._
import session.action.SessionActionBuilder
import utils.Gzip

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

  private val actionDispatcherProvider =
    actionModule.actionDispatcherProvider

  private val malformedDataException =
    new Exception("Malformed data")

  def getNode(path: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(GetZNodeWithChildrenAction(path))
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(apiResponseFactory.fromThrowable[ZNodeWithChildren])
        .runToFuture

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
        .runToFuture

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
        .runToFuture

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
        .runToFuture

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
        .runToFuture

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
        .runToFuture

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson[Nothing]))
      }
    }

  def deleteChildrenNodes(path: ZNodePath, names: List[String]): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = names
        .traverse(path.down)
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
              .runToFuture
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
      }.runToFuture

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson))
      }
    }

  def updateData(path: ZNodePath, version: ZNodeDataVersion): Action[JsValue] =
    newCuratorAction(playBodyParsers.json).async { implicit curatorRequest =>
      val futureResultReader = parseRequestBodyJson[ZNodeData].flatMap {
        zNodeData =>
          actionDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(
              UpdateZNodeDataAction(
                path,
                zNodeData,
                version
              )
            )
            .map(apiResponseFactory.okPayload)
            .onErrorHandle(apiResponseFactory.fromThrowable[ZNodeMeta])
      }.runToFuture

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
        .map(implicitly[Writes[List[Cofree[List, ZNodeExport]]]].writes)
        .map(Json.toBytes)
        .map(Gzip.compress)
        .map(Base64.getEncoder.encode)
        .map(new String(_, StandardCharsets.UTF_8))
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(apiResponseFactory.fromThrowable[String])
        .runToFuture

      render.async {
        case Accepts.Json() =>
          futureResultReader.map(_(ApiResponse.writeJson))
      }
    }

  def importNodes(path: ZNodePath): Action[ByteString] =
    newCuratorAction(playBodyParsers.byteString).async { implicit curatorRequest =>
      def dispatchImport(exportZNodes: List[Cofree[List, ZNodeExport]]) =
        actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(ImportZNodesAction(path, exportZNodes))
          .map(_ => apiResponseFactory.okEmpty[Nothing])
          .onErrorHandle(apiResponseFactory.fromThrowable[Nothing])

      val importNodesT =
        curatorRequest.contentType match {
          case Some("application/json") =>
            implicitly[Reads[List[Cofree[List, ZNodeExport]]]]
              .reads(Json.parse(curatorRequest.body.toArray))
              .asEither
              .left.map(_ => malformedDataException)
              .toTry
              .traverse(dispatchImport)
              .flatMap(Task.fromTry)

          case Some("application/gzip") =>
            Gzip
              .decompress(curatorRequest.body.toArray)
              .flatMap { byteArray =>
                implicitly[Reads[List[Cofree[List, ZNodeExport]]]]
                  .reads(Json.parse(byteArray))
                  .asEither
                  .left.map(_ => malformedDataException)
                  .toTry
              }
              .traverse(dispatchImport)
              .flatMap(Task.fromTry)

          case _ =>
            Task.now(apiResponseFactory.badRequest(Some(s"Unsupported content type: ${curatorRequest.contentType.getOrElse("?")}")))
        }

      val importNodesF = importNodesT.runToFuture

      render.async {
        case Accepts.Json() =>
          importNodesF.map(_(ApiResponse.writeJson[Nothing]))
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
