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

import java.nio.charset.StandardCharsets
import java.util.Base64

import akka.util.ByteString
import api.ApiResponse
import api.ApiResponseFactory
import api.exceptions.BadRequestException
import api.formats.Json._
import cats.free.Cofree
import cats.instances.list._
import cats.instances.try_._
import cats.syntax.traverse._
import com.elkozmon.zoonavigator.core.action.actions._
import com.elkozmon.zoonavigator.core.action.ActionModule
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import curator.action.CuratorActionBuilder
import curator.action.CuratorRequest
import monix.eval.Task
import play.api.http.Writeable
import play.api.libs.json._
import play.api.mvc._
import schedulers.BlockingScheduler
import schedulers.ComputingScheduler
import session.action.SessionActionBuilder
import utils.Gzip

import scala.concurrent.Future

class ZNodeController(
    apiResponseFactory: ApiResponseFactory,
    curatorActionBuilder: CuratorActionBuilder,
    sessionActionBuilder: SessionActionBuilder,
    blockingScheduler: BlockingScheduler,
    computingScheduler: ComputingScheduler,
    actionModule: ActionModule,
    playBodyParsers: PlayBodyParsers,
    controllerComponents: ControllerComponents
) extends AbstractController(controllerComponents) {

  private val actionDispatcher =
    actionModule.actionDispatcher

  private val malformedDataException =
    new Exception("Malformed data")

  def getNode(path: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcher
        .dispatch(GetZNodeWithChildrenAction(path, curatorRequest.curatorFramework))
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .executeOn(blockingScheduler)
        .runToFuture(blockingScheduler)

      render.async {
        case Accepts.Json() =>
          futureResultReader.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  def getChildrenNodes(path: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcher
        .dispatch(GetZNodeChildrenAction(path, curatorRequest.curatorFramework))
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .executeOn(blockingScheduler)
        .runToFuture(blockingScheduler)

      render.async {
        case Accepts.Json() =>
          futureResultReader.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  def createNode(path: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcher
        .dispatch(CreateZNodeAction(path, curatorRequest.curatorFramework))
        .map(_ => apiResponseFactory.okEmpty[Unit])
        .onErrorHandle(apiResponseFactory.fromThrowable[Unit])
        .executeOn(blockingScheduler)
        .runToFuture(blockingScheduler)

      render.async {
        case Accepts.Json() =>
          futureResultReader.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  def duplicateNode(source: ZNodePath, destination: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcher
        .dispatch(DuplicateZNodeRecursiveAction(source, destination, curatorRequest.curatorFramework))
        .map(_ => apiResponseFactory.okEmpty[Unit])
        .onErrorHandle(apiResponseFactory.fromThrowable[Unit])
        .executeOn(blockingScheduler)
        .runToFuture(blockingScheduler)

      render.async {
        case Accepts.Json() =>
          futureResultReader.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  def moveNode(source: ZNodePath, destination: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcher
        .dispatch(MoveZNodeRecursiveAction(source, destination, curatorRequest.curatorFramework))
        .map(_ => apiResponseFactory.okEmpty[Unit])
        .onErrorHandle(apiResponseFactory.fromThrowable[Unit])
        .executeOn(blockingScheduler)
        .runToFuture(blockingScheduler)

      render.async {
        case Accepts.Json() =>
          futureResultReader.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  def deleteNode(path: ZNodePath, version: ZNodeDataVersion): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcher
        .dispatch(DeleteZNodeRecursiveAction(path, version, curatorRequest.curatorFramework))
        .map(_ => apiResponseFactory.okEmpty[Unit])
        .onErrorHandle(apiResponseFactory.fromThrowable[Unit])
        .executeOn(blockingScheduler)
        .runToFuture(blockingScheduler)

      render.async {
        case Accepts.Json() =>
          futureResultReader.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  def deleteChildrenNodes(path: ZNodePath, names: List[String]): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = names
        .traverse(path.down)
        .toEither
        .left
        .map(apiResponseFactory.fromThrowable[Unit])
        .fold(
          Future.successful, { paths =>
            actionDispatcher
              .dispatch(ForceDeleteZNodeRecursiveAction(paths, curatorRequest.curatorFramework))
              .map(_ => apiResponseFactory.okEmpty[Unit])
              .onErrorHandle(apiResponseFactory.fromThrowable[Unit])
              .executeOn(blockingScheduler)
              .runToFuture(blockingScheduler)
          }
        )

      render.async {
        case Accepts.Json() =>
          futureResultReader.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  def updateAcl(path: ZNodePath, version: ZNodeAclVersion, recursive: Option[Boolean]): Action[JsValue] =
    newCuratorAction(playBodyParsers.json).async { implicit curatorRequest =>
      val futureResultReader = parseRequestBodyJson[List[Acl]]
        .flatMap { jsonAclList =>
          val taskMeta: Task[ZNodeMeta] =
            if (recursive.contains(true)) {
              actionDispatcher
                .dispatch(
                  UpdateZNodeAclListRecursiveAction(
                    path,
                    ZNodeAcl(jsonAclList),
                    version,
                    curatorRequest.curatorFramework
                  )
                )
            } else {
              actionDispatcher
                .dispatch(
                  UpdateZNodeAclListAction(path, ZNodeAcl(jsonAclList), version, curatorRequest.curatorFramework)
                )
            }

          taskMeta
            .map(apiResponseFactory.okPayload)
            .onErrorHandle(apiResponseFactory.fromThrowable[ZNodeMeta])
        }
        .executeOn(blockingScheduler)
        .runToFuture(blockingScheduler)

      render.async {
        case Accepts.Json() =>
          futureResultReader.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  def updateData(path: ZNodePath, version: ZNodeDataVersion): Action[JsValue] =
    newCuratorAction(playBodyParsers.json).async { implicit curatorRequest =>
      val futureResultReader = parseRequestBodyJson[ZNodeData]
        .flatMap { zNodeData =>
          actionDispatcher
            .dispatch(UpdateZNodeDataAction(path, zNodeData, version, curatorRequest.curatorFramework))
            .map(apiResponseFactory.okPayload)
            .onErrorHandle(apiResponseFactory.fromThrowable[ZNodeMeta])
        }
        .executeOn(blockingScheduler)
        .runToFuture(blockingScheduler)

      render.async {
        case Accepts.Json() =>
          futureResultReader.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  def getExportNodes(paths: List[ZNodePath]): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val futureResultReader = actionDispatcher
        .dispatch(ExportZNodesAction(paths, curatorRequest.curatorFramework))
        .map(implicitly[Writes[List[Cofree[List, ZNodeExport]]]].writes)
        .map(Json.toBytes)
        .map(Gzip.compress)
        .map(Base64.getEncoder.encode)
        .map(new String(_, StandardCharsets.UTF_8))
        .map(apiResponseFactory.okPayload)
        .onErrorHandle(apiResponseFactory.fromThrowable[String])
        .executeOn(blockingScheduler)
        .runToFuture(blockingScheduler)

      render.async {
        case Accepts.Json() =>
          futureResultReader.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  def importNodes(path: ZNodePath): Action[ByteString] =
    newCuratorAction(playBodyParsers.byteString).async { implicit curatorRequest =>
      def dispatchImport(exportZNodes: List[Cofree[List, ZNodeExport]]) =
        actionDispatcher
          .dispatch(ImportZNodesAction(path, exportZNodes, curatorRequest.curatorFramework))
          .map(_ => apiResponseFactory.okEmpty[Unit])
          .onErrorHandle(apiResponseFactory.fromThrowable[Unit])

      val importNodesT =
        curatorRequest.contentType match {
          case Some("application/json") =>
            implicitly[Reads[List[Cofree[List, ZNodeExport]]]]
              .reads(Json.parse(curatorRequest.body.toArray))
              .asEither
              .left
              .map(_ => malformedDataException)
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
                  .left
                  .map(_ => malformedDataException)
                  .toTry
              }
              .traverse(dispatchImport)
              .flatMap(Task.fromTry)

          case _ =>
            Task.now(
              apiResponseFactory
                .badRequest(Some(s"Unsupported content type: ${curatorRequest.contentType.getOrElse("?")}"))
            )
        }

      val importNodesF = importNodesT
        .executeOn(blockingScheduler)
        .runToFuture(blockingScheduler)

      render.async {
        case Accepts.Json() =>
          importNodesF.asResultAsync(asJsonApiResponse)(computingScheduler)
      }
    }

  private def newCuratorAction[B](
      bodyParser: BodyParser[B]
  )(implicit wrt: Writeable[ApiResponse[String]]): ActionBuilder[CuratorRequest, B] =
    sessionActionBuilder(bodyParser) andThen curatorActionBuilder()

  private def parseRequestBodyJson[T](implicit request: Request[JsValue], reads: Reads[T]): Task[T] =
    request.body.validateOpt[T] match {
      case JsSuccess(Some(value), _) =>
        Task.now(value)
      case JsError(_) =>
        Task.raiseError(new BadRequestException("Malformed request body"))
      case JsSuccess(None, _) =>
        Task.raiseError(new BadRequestException("Missing request body"))
    }
}
