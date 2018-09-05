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
import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import curator.action.{CuratorActionBuilder, CuratorRequest}
import monix.eval.Task
import monix.execution.Scheduler
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

  def get: Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      getRequiredQueryParam("path")
        .flatMap(parseZNodePath)
        .fold(
          Future.successful, { path =>
            actionDispatcherProvider
              .getDispatcher(curatorRequest.curatorFramework)
              .dispatch(GetZNodeWithChildrenAction(path))
              .map(apiResponseFactory.okPayload[ZNodeWithChildren])
              .onErrorHandle(apiResponseFactory.fromThrowable)
              .runAsync
          }
        )
    }

  def getChildren: Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      getRequiredQueryParam("path")
        .flatMap(parseZNodePath)
        .fold(
          Future.successful, { path =>
            actionDispatcherProvider
              .getDispatcher(curatorRequest.curatorFramework)
              .dispatch(GetZNodeChildrenAction(path))
              .map(
                apiResponseFactory.okPayload[ZNodeMetaWith[ZNodeChildren]](_)(
                  apiResponseWriteable(zNodeMetaWithWrites(zNodeChildrenWrites))
                )
              )
              .onErrorHandle(apiResponseFactory.fromThrowable)
              .runAsync
          }
        )
    }

  def create(): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      getRequiredQueryParam("path")
        .flatMap(parseZNodePath)
        .fold(
          Future.successful, { path =>
            actionDispatcherProvider
              .getDispatcher(curatorRequest.curatorFramework)
              .dispatch(CreateZNodeAction(path))
              .map(_ => apiResponseFactory.okEmpty)
              .onErrorHandle(apiResponseFactory.fromThrowable)
              .runAsync
          }
        )
    }

  def duplicate(): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val eitherResult = for {
        source <- getRequiredQueryParam("source")
          .flatMap(parseZNodePath)
        destination <- getRequiredQueryParam("destination")
          .flatMap(parseZNodePath)
      } yield {
        actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(DuplicateZNodeRecursiveAction(source, destination))
          .map(_ => apiResponseFactory.okEmpty)
          .onErrorHandle(apiResponseFactory.fromThrowable)
          .runAsync
      }

      eitherResult.fold(Future.successful, identity)
    }

  def move(): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val eitherResult = for {
        source <- getRequiredQueryParam("source")
          .flatMap(parseZNodePath)
        destination <- getRequiredQueryParam("destination")
          .flatMap(parseZNodePath)
      } yield {
        actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(MoveZNodeRecursiveAction(source, destination))
          .map(_ => apiResponseFactory.okEmpty)
          .onErrorHandle(apiResponseFactory.fromThrowable)
          .runAsync
      }

      eitherResult.fold(Future.successful, identity)
    }

  def delete(): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val eitherResult = for {
        path <- getRequiredQueryParam("path").flatMap(parseZNodePath)
        version <- getRequiredQueryParam("version").map(_.toLong)
      } yield {
        actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(DeleteZNodeRecursiveAction(path, ZNodeDataVersion(version)))
          .map(_ => apiResponseFactory.okEmpty)
          .onErrorHandle(apiResponseFactory.fromThrowable)
          .runAsync
      }

      eitherResult.fold(Future.successful, identity)
    }

  def deleteChildren(): Action[Unit] =
    newCuratorAction(playBodyParsers.empty).async { implicit curatorRequest =>
      val eitherResult = for {
        path <- getRequiredQueryParam("path").flatMap(parseZNodePath)
        names <- getRequiredQueryParam("names").map(_.split("/"))
        paths <- names.toList
          .traverseU(path.down)
          .toEither
          .left
          .map(apiResponseFactory.fromThrowable)
      } yield {
        actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(ForceDeleteZNodeRecursiveAction(paths))
          .map(_ => apiResponseFactory.okEmpty)
          .onErrorHandle(apiResponseFactory.fromThrowable)
          .runAsync
      }

      eitherResult.fold(Future.successful, identity)
    }

  def updateAcl(): Action[JsValue] =
    newCuratorAction(playBodyParsers.json).async { implicit curatorRequest =>
      val eitherResult = for {
        path <- getRequiredQueryParam("path").flatMap(parseZNodePath)
        version <- getRequiredQueryParam("version").map(_.toLong)
        aclList <- parseRequestBodyJson[List[Acl]]
      } yield {
        val recursive = curatorRequest.getQueryString("recursive").isDefined

        val taskMeta: Task[ZNodeMeta] =
          if (recursive) {
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

        taskMeta
          .map(apiResponseFactory.okPayload[ZNodeMeta])
          .onErrorHandle(apiResponseFactory.fromThrowable)
          .runAsync
      }

      eitherResult.fold(Future.successful, identity)
    }

  def updateData(): Action[String] =
    newCuratorAction(playBodyParsers.text).async { implicit curatorRequest =>
      val eitherResult = for {
        path <- getRequiredQueryParam("path").flatMap(parseZNodePath)
        version <- getRequiredQueryParam("version").map(_.toLong)
      } yield {
        actionDispatcherProvider
          .getDispatcher(curatorRequest.curatorFramework)
          .dispatch(
            UpdateZNodeDataAction(
              path,
              ZNodeData(curatorRequest.body.getBytes(StandardCharsets.UTF_8)),
              ZNodeDataVersion(version)
            )
          )
          .map(apiResponseFactory.okPayload[ZNodeMeta])
          .onErrorHandle(apiResponseFactory.fromThrowable)
          .runAsync
      }

      eitherResult.fold(Future.successful, identity)
    }

  private def newCuratorAction[B](
      bodyParser: BodyParser[B]
  ): ActionBuilder[CuratorRequest, B] =
    sessionActionBuilder(bodyParser) andThen curatorActionBuilder()

  private def getRequiredQueryParam(
      name: String
  )(implicit request: Request[_]): Either[Result, String] =
    request
      .getQueryString(name)
      .toRight(
        apiResponseFactory
          .badRequest(Some(s"Missing '$name' query string parameter"))
      )

  private def parseZNodePath(path: String): Either[Result, ZNodePath] =
    ZNodePath
      .parse(path)
      .toEither
      .left
      .map(apiResponseFactory.fromThrowable)

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
