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

import action.ActionModule
import api.ApiResponseFactory
import cats.implicits._
import com.elkozmon.zoonavigator.core.action.actions._
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import curator.action.CuratorActionBuilder
import curator.action.CuratorRequest
import json.zookeeper.acl.JsonAcl
import json.zookeeper.znode._
import json.zookeeper.znode.JsonZNodeChildren._
import json.zookeeper.znode.JsonZNodePath._
import monix.eval.Task
import monix.execution.Scheduler
import play.api.libs.json._
import play.api.mvc._
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

  def get(path: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(GetZNodeWithChildrenAction(path))
        .map { node =>
          val jsonNode = JsonZNodeWithChildren(node)

          apiResponseFactory.okPayload(jsonNode)
        }
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync
    }

  def getChildren(path: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(GetZNodeChildrenAction(path))
        .map { metaWithChildren =>
          val jsonMetaWithChildren = JsonZNodeMetaWith(metaWithChildren.map(JsonZNodeChildren(_)))

          apiResponseFactory.okPayload(jsonMetaWithChildren)
        }
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync
    }

  def create(path: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(CreateZNodeAction(path))
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync
    }

  def duplicate(source: ZNodePath, destination: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(DuplicateZNodeRecursiveAction(source, destination))
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync
    }

  def move(source: ZNodePath, destination: ZNodePath): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(MoveZNodeRecursiveAction(source, destination))
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync
    }

  def delete(path: ZNodePath, version: ZNodeDataVersion): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(DeleteZNodeRecursiveAction(path, version))
        .map(_ => apiResponseFactory.okEmpty)
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync
    }

  def deleteChildren(path: ZNodePath, names: List[String]): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      names
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
    }

  def updateAcl(
      path: ZNodePath,
      version: ZNodeAclVersion,
      recursive: Option[Boolean]
  ): Action[JsValue] =
    newCuratorAction(playBodyParsers.json).async { implicit curatorRequest =>
      val eitherResult = parseRequestBodyJson[List[JsonAcl]].map {
        jsonAclList =>
          val taskMeta: Task[ZNodeMeta] =
            if (recursive.contains(true)) {
              actionDispatcherProvider
                .getDispatcher(curatorRequest.curatorFramework)
                .dispatch(
                  UpdateZNodeAclListRecursiveAction(
                    path,
                    ZNodeAcl(jsonAclList.map(_.underlying)),
                    version
                  )
                )
            } else {
              actionDispatcherProvider
                .getDispatcher(curatorRequest.curatorFramework)
                .dispatch(
                  UpdateZNodeAclListAction(
                    path,
                    ZNodeAcl(jsonAclList.map(_.underlying)),
                    version
                  )
                )
            }

          taskMeta
            .map(meta => apiResponseFactory.okPayload(JsonZNodeMeta(meta)))
            .onErrorHandle(apiResponseFactory.fromThrowable)
            .runAsync
      }

      eitherResult.fold(Future.successful, identity)
    }

  def updateData(path: ZNodePath, version: ZNodeDataVersion): Action[String] =
    newCuratorAction(playBodyParsers.text).async { implicit curatorRequest =>
      actionDispatcherProvider
        .getDispatcher(curatorRequest.curatorFramework)
        .dispatch(
          UpdateZNodeDataAction(
            path,
            ZNodeData(curatorRequest.body.getBytes(StandardCharsets.UTF_8)),
            version
          )
        )
        .map(meta => apiResponseFactory.okPayload(JsonZNodeMeta(meta)))
        .onErrorHandle(apiResponseFactory.fromThrowable)
        .runAsync
    }

  private def newCuratorAction[B](
      bodyParser: BodyParser[B]
  ): ActionBuilder[CuratorRequest, B] =
    sessionActionBuilder(bodyParser) andThen curatorActionBuilder()

  private def parseRequestBodyJson[T](
      implicit request: Request[JsValue],
      reads: Reads[T]
  ): Either[Result, T] =
    request.body
      .validateOpt[T] match {
      case JsSuccess(Some(value), _) =>
        Right(value)
      case JsError(_) =>
        Left(apiResponseFactory.badRequest(Some("Malformed request body")))
      case JsSuccess(None, _) =>
        Left(apiResponseFactory.badRequest(Some("Missing request body")))
    }
}
