/*
 * Copyright (C) 2017  Ľuboš Kozmon
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
import com.elkozmon.zoonavigator.core.command.commands._
import com.elkozmon.zoonavigator.core.query.queries._
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import command.CommandModule
import curator.action.{CuratorActionBuilder, CuratorRequest}
import json.zookeeper.acl.JsonAcl
import json.zookeeper.znode._
import play.api.libs.json.{JsError, JsSuccess, Reads}
import play.api.mvc._
import query.QueryModule
import session.action.SessionActionBuilder

import scala.concurrent.Future
import scala.util.control.NonFatal

class ZNodeController(
  apiResponseFactory: ApiResponseFactory,
  curatorActionBuilder: CuratorActionBuilder,
  sessionActionBuilder: SessionActionBuilder,
  queryModule: QueryModule,
  commandModule: CommandModule
) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val queryDispatcherProvider = queryModule.queryDispatcherProvider

  private val commandDispatcherProvider = commandModule.commandDispatcherProvider

  private val recoverResult: PartialFunction[Throwable, Result] = {
    case throwable =>
      apiResponseFactory.fromThrowable(throwable)
  }

  def getAcl: Action[AnyContent] =
    newCuratorAction.async {
      implicit curatorRequest =>
        getRequiredQueryParam("path").fold(Future.successful, { path =>
          queryDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(GetZNodeAclQuery(ZNodePath(path)))
            .map {
              metaWithAcl =>
                val jsonMetaWithAcl = JsonZNodeMetaWith(
                  metaWithAcl.map(JsonZNodeAcl(_))
                )

                apiResponseFactory.okPayload(jsonMetaWithAcl)
            }
            .recover(recoverResult)
        })
    }

  def getData: Action[AnyContent] =
    newCuratorAction.async {
      implicit curatorRequest =>
        getRequiredQueryParam("path").fold(Future.successful, { path =>
          queryDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(GetZNodeDataQuery(ZNodePath(path)))
            .map {
              metaWithData =>
                val jsonMetaWithData = JsonZNodeMetaWith(
                  metaWithData.map(JsonZNodeData(_))
                )

                apiResponseFactory.okPayload(jsonMetaWithData)
            }
            .recover(recoverResult)
        })
    }

  def getMeta: Action[AnyContent] =
    newCuratorAction.async {
      implicit curatorRequest =>
        getRequiredQueryParam("path").fold(Future.successful, { path =>
          queryDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(GetZNodeMetaQuery(ZNodePath(path)))
            .map(meta => apiResponseFactory.okPayload(JsonZNodeMeta(meta)))
            .recover {
              case NonFatal(throwable) =>
                apiResponseFactory.fromThrowable(throwable)
            }
        })
    }

  def getChildren: Action[AnyContent] =
    newCuratorAction.async {
      implicit curatorRequest =>
        getRequiredQueryParam("path").fold(Future.successful, { path =>
          queryDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(GetZNodeChildrenQuery(ZNodePath(path)))
            .map {
              metaWithChildren =>
                val jsonMetaWithChildren = JsonZNodeMetaWith(
                  metaWithChildren.map(JsonZNodeChildren(_))
                )

                apiResponseFactory.okPayload(jsonMetaWithChildren)
            }
            .recover(recoverResult)
        })
    }

  def create(): Action[AnyContent] =
    newCuratorAction.async {
      implicit curatorRequest =>
        getRequiredQueryParam("path").fold(Future.successful, { path =>
          commandDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(CreateZNodeCommand(ZNodePath(path)))
            .map(_ => apiResponseFactory.okEmpty)
            .recover(recoverResult)
        })
    }

  def delete(): Action[AnyContent] =
    newCuratorAction.async {
      implicit curatorRequest =>
        val eitherResult = for {
          path <- getRequiredQueryParam("path").right
          version <- getRequiredQueryParam("version").right.map(_.toLong).right
        } yield {
          commandDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(
              DeleteZNodeRecursiveCommand(
                ZNodePath(path),
                ZNodeDataVersion(version)
              )
            )
            .map(_ => apiResponseFactory.okEmpty)
            .recover(recoverResult)
        }

        eitherResult.fold(Future.successful, identity)
    }

  def deleteChildren(): Action[AnyContent] =
    newCuratorAction.async {
      implicit curatorRequest =>
        val eitherResult = for {
          path <- getRequiredQueryParam("path").right.map(_.stripSuffix("/")).right
          names <- getRequiredQueryParam("names").right.map(_.split("/")).right
        } yield {
          names.map {
            name =>
              val zNodePath = ZNodePath(s"$path/$name")

              commandDispatcherProvider
                .getDispatcher(curatorRequest.curatorFramework)
                .dispatch(ForceDeleteZNodeRecursiveCommand(zNodePath))
          }
        }

        eitherResult.fold(Future.successful, { futures =>
          Future
            .sequence(futures.toList)
            .map(_ => apiResponseFactory.okEmpty)
            .recover(recoverResult)
        })
    }

  def updateAcl(): Action[AnyContent] =
    newCuratorAction.async {
      implicit curatorRequest =>
        val eitherResult = for {
          jsonAclList <- parseRequestBodyJson[List[JsonAcl]].right
          path <- getRequiredQueryParam("path").right
          version <- getRequiredQueryParam("version").right.map(_.toLong).right
        } yield {
          val recursive = curatorRequest.getQueryString("recursive").isDefined

          val futureMeta: Future[ZNodeMeta] =
            if (recursive) {
              commandDispatcherProvider
                .getDispatcher(curatorRequest.curatorFramework)
                .dispatch(
                  UpdateZNodeAclListRecursiveCommand(
                    ZNodePath(path),
                    ZNodeAcl(jsonAclList.map(_.underlying)),
                    ZNodeAclVersion(version)
                  )
                )
            } else {
              commandDispatcherProvider
                .getDispatcher(curatorRequest.curatorFramework)
                .dispatch(
                  UpdateZNodeAclListCommand(
                    ZNodePath(path),
                    ZNodeAcl(jsonAclList.map(_.underlying)),
                    ZNodeAclVersion(version)
                  )
                )
            }

          futureMeta
            .map(meta => apiResponseFactory.okPayload(JsonZNodeMeta(meta)))
            .recover(recoverResult)
        }

        eitherResult.fold(Future.successful, identity)
    }

  def updateData(): Action[AnyContent] =
    newCuratorAction.async {
      implicit curatorRequest =>
        val eitherResult = for {
          path <- getRequiredQueryParam("path").right
          version <- getRequiredQueryParam("version").right.map(_.toLong).right
        } yield {
          commandDispatcherProvider
            .getDispatcher(curatorRequest.curatorFramework)
            .dispatch(
              UpdateZNodeDataCommand(
                ZNodePath(path),
                ZNodeData(
                  curatorRequest
                    .body
                    .asText
                    .map(_.getBytes)
                    .getOrElse(Array.empty)
                ),
                ZNodeDataVersion(version)
              )
            )
            .map(meta => apiResponseFactory.okPayload(JsonZNodeMeta(meta)))
            .recover(recoverResult)
        }

        eitherResult.fold(Future.successful, identity)
    }

  private def newCuratorAction: ActionBuilder[CuratorRequest] =
    sessionActionBuilder() andThen curatorActionBuilder()

  private def getRequiredQueryParam(name: String)(implicit request: Request[_]): Either[Result, String] =
    request
      .getQueryString(name)
      .toRight(apiResponseFactory.badRequest(Some(s"Missing '$name' query string parameter")))

  private def parseRequestBodyJson[T](implicit request: Request[AnyContent], reads: Reads[T]): Either[Result, T] =
    request
      .body
      .asJson
      .map(_.validate[T]) match {
      case Some(JsSuccess(value, _)) =>
        Right(value)
      case Some(JsError(_)) =>
        Left(apiResponseFactory.badRequest(Some("Malformed request body")))
      case None =>
        Left(apiResponseFactory.badRequest(Some("Missing request body")))
    }
}
