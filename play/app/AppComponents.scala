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

import java.util.concurrent.TimeUnit

import api.controllers.FrontendController
import api.controllers.ZNodeController
import api.controllers.ZSessionController
import api.ApiErrorHandler
import api.ApiResponseFactory
import api.DefaultApiResponseFactory
import com.elkozmon.zoonavigator.core.action.ActionHandler
import com.elkozmon.zoonavigator.core.action.actions._
import com.softwaremill.macwire._
import config.HttpContext
import controllers.AssetsComponents
import curator.action.CuratorActionBuilder
import curator.provider._
import loggers.AppLogger
import monix.execution.Scheduler
import org.slf4j.LoggerFactory
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.LoggerConfigurator
import play.api.http.HttpErrorHandler
import play.api.routing.Router
import play.core.SourceMapper
import play.filters.HttpFiltersComponents
import router.Routes
import schedulers._
import session.SessionInactivityTimeout
import session.action.SessionActionBuilder
import session.manager.ExpiringSessionManager
import session.manager.SessionManager
import zookeeper.session.DefaultZooKeeperSessionHelper
import zookeeper.session.ZooKeeperSessionHelper

import scala.concurrent.duration.FiniteDuration

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AssetsComponents
    with AppModule {

  LoggerConfigurator(context.environment.classLoader)
    .foreach(_.configure(context.environment))

  private lazy val httpContext: HttpContext =
    HttpContext(
      configuration
        .getOptional[String]("play.http.context")
        .getOrElse("/")
        .stripPrefix("/")
        .stripSuffix("/")
        .prepended('/')
    )

  override lazy val httpErrorHandler: HttpErrorHandler = {
    //noinspection ScalaUnusedSymbol
    def routerProvider: Option[Router] = Option(router)

    //noinspection ScalaUnusedSymbol
    def sourceMapper: Option[SourceMapper] = devContext.map(_.sourceMapper)

    wire[ApiErrorHandler]
  }

  override lazy val router: Router = {
    //noinspection ScalaUnusedSymbol
    val prefix = httpContext.context

    wire[Routes]
  }

  lazy val sessionInactivityTimeout: SessionInactivityTimeout =
    SessionInactivityTimeout(
      new FiniteDuration(
        context.initialConfiguration
          .getOptional[Long]("play.http.session.maxAge")
          .getOrElse(5 * 60 * 1000),
        TimeUnit.MILLISECONDS
      )
    )

  lazy val scheduler: Scheduler =
    Scheduler(actorSystem.dispatcher)

  lazy val curatorCacheMaxAge: CuratorCacheMaxAge =
    CuratorCacheMaxAge(
      FiniteDuration(
        context.initialConfiguration
          .getOptional[Long]("zookeeper.client.maxAge")
          .getOrElse(5000L),
        TimeUnit.MILLISECONDS
      )
    )

  lazy val curatorConnectTimeout: CuratorConnectTimeout =
    CuratorConnectTimeout(
      FiniteDuration(
        context.initialConfiguration
          .getOptional[Long]("zookeeper.client.connectTimeout")
          .getOrElse(5000L),
        TimeUnit.MILLISECONDS
      )
    )

  override val appLogger: AppLogger =
    AppLogger(LoggerFactory.getLogger("application"))

  override val apiResponseFactory: ApiResponseFactory =
    wire[DefaultApiResponseFactory]

  override val sessionManager: SessionManager =
    wire[ExpiringSessionManager]

  override val curatorFrameworkProvider: CuratorFrameworkProvider =
    wire[CacheCuratorFrameworkProvider]

  override val zookeeperSessionHelper: ZooKeeperSessionHelper =
    wire[DefaultZooKeeperSessionHelper]

  override lazy val curatorActionBuilder: CuratorActionBuilder =
    wire[CuratorActionBuilder]

  override lazy val sessionActionBuilder: SessionActionBuilder =
    wire[SessionActionBuilder]

  override lazy val frontendController: FrontendController =
    wire[FrontendController]

  override lazy val zNodeController: ZNodeController =
    wire[ZNodeController]

  override lazy val zSessionController: ZSessionController =
    wire[ZSessionController]

  override lazy val blockingScheduler: BlockingScheduler =
    DefaultBlockingScheduler(Scheduler.io("zoonavigator-io"))

  override lazy val computingScheduler: ComputingScheduler =
    DefaultComputingScheduler(Scheduler(executionContext))

  override lazy val getZNodeWithChildrenActionHandler: ActionHandler[GetZNodeWithChildrenAction] =
    wire[GetZNodeWithChildrenActionHandler]

  override lazy val getZNodeAclActionHandler: ActionHandler[GetZNodeAclAction] =
    wire[GetZNodeAclActionHandler]

  override lazy val getZNodeChildrenActionHandler: ActionHandler[GetZNodeChildrenAction] =
    wire[GetZNodeChildrenActionHandler]

  override lazy val getZNodeDataActionHandler: ActionHandler[GetZNodeDataAction] =
    wire[GetZNodeDataActionHandler]

  override lazy val getZNodeMetaActionHandler: ActionHandler[GetZNodeMetaAction] =
    wire[GetZNodeMetaActionHandler]

  override lazy val createZNodeActionHandler: ActionHandler[CreateZNodeAction] =
    wire[CreateZNodeActionHandler]

  override lazy val deleteZNodeRecursiveActionHandler: ActionHandler[DeleteZNodeRecursiveAction] =
    wire[DeleteZNodeRecursiveActionHandler]

  override lazy val forceDeleteZNodeRecursiveActionHandler: ActionHandler[ForceDeleteZNodeRecursiveAction] =
    wire[ForceDeleteZNodeRecursiveActionHandler]

  override lazy val duplicateZNodeRecursiveActionHandler: ActionHandler[DuplicateZNodeRecursiveAction] =
    wire[DuplicateZNodeRecursiveActionHandler]

  override lazy val moveZNodeRecursiveActionHandler: ActionHandler[MoveZNodeRecursiveAction] =
    wire[MoveZNodeRecursiveActionHandler]

  override lazy val updateZNodeAclListActionHandler: ActionHandler[UpdateZNodeAclListAction] =
    wire[UpdateZNodeAclListActionHandler]

  override lazy val updateZNodeAclListRecursiveActionHandler: ActionHandler[UpdateZNodeAclListRecursiveAction] =
    wire[UpdateZNodeAclListRecursiveActionHandler]

  override lazy val updateZNodeDataActionHandler: ActionHandler[UpdateZNodeDataAction] =
    wire[UpdateZNodeDataActionHandler]

  override lazy val exportZNodesActionHandler: ActionHandler[ExportZNodesAction] =
    wire[ExportZNodesActionHandler]

  override lazy val importZNodesActionHandler: ActionHandler[ImportZNodesAction] =
    wire[ImportZNodesActionHandler]
}
