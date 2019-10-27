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

import api._
import com.elkozmon.zoonavigator.core.action.ActionHandler
import com.elkozmon.zoonavigator.core.action.actions._
import com.softwaremill.macwire._
import controllers._
import curator.action.CuratorActionBuilder
import curator.provider._
import modules.AppModule
import monix.execution.Scheduler
import org.apache.curator.framework.CuratorFramework
import play.api.{BuiltInComponentsFromContext, LoggerConfigurator}
import play.api.ApplicationLoader.Context
import play.api.http.HttpErrorHandler
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.core.SourceMapper
import play.filters.cors.{CORSConfig, CORSFilter}
import router.Routes
import session.SessionInactivityTimeout
import session.action.SessionActionBuilder
import session.manager.{ExpiringSessionManager, SessionManager}
import zookeeper.session.{DefaultZooKeeperSessionHelper, ZooKeeperSessionHelper}

import scala.concurrent.duration.FiniteDuration

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AssetsComponents
    with AppModule {

  LoggerConfigurator(context.environment.classLoader)
    .foreach(_.configure(context.environment))

  private lazy val httpContext: String = configuration
    .getOptional[String]("play.http.context")
    .getOrElse("/")

  override lazy val httpErrorHandler: HttpErrorHandler = {
    //noinspection ScalaUnusedSymbol
    def routerProvider: Option[Router] = Option(router)

    //noinspection ScalaUnusedSymbol
    def sourceMapper: Option[SourceMapper] = devContext.map(_.sourceMapper)

    wire[ApiErrorHandler]
  }

  override lazy val router: Router =
    wire[Routes].withPrefix(httpContext)

  override def corsFilter: CORSFilter = {
    //noinspection ScalaUnusedSymbol
    val prefixes: Seq[String] = Seq(httpContext)

    //noinspection ScalaUnusedSymbol
    val corsConfig: CORSConfig = CORSConfig.fromConfiguration(configuration)

    wire[CORSFilter]
  }

  override lazy val httpFilters: Seq[EssentialFilter] =
    filters.filters

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

  override def getZNodeWithChildrenActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[GetZNodeWithChildrenAction] =
    wire[GetZNodeWithChildrenActionHandler]

  override def getZNodeAclActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[GetZNodeAclAction] =
    wire[GetZNodeAclActionHandler]

  override def getZNodeChildrenActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[GetZNodeChildrenAction] =
    wire[GetZNodeChildrenActionHandler]

  override def getZNodeDataActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[GetZNodeDataAction] =
    wire[GetZNodeDataActionHandler]

  override def getZNodeMetaActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[GetZNodeMetaAction] =
    wire[GetZNodeMetaActionHandler]

  override def createZNodeActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[CreateZNodeAction] =
    wire[CreateZNodeActionHandler]

  override def deleteZNodeRecursiveActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[DeleteZNodeRecursiveAction] =
    wire[DeleteZNodeRecursiveActionHandler]

  override def forceDeleteZNodeRecursiveActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[ForceDeleteZNodeRecursiveAction] =
    wire[ForceDeleteZNodeRecursiveActionHandler]

  override def duplicateZNodeRecursiveActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[DuplicateZNodeRecursiveAction] =
    wire[DuplicateZNodeRecursiveActionHandler]

  override def moveZNodeRecursiveActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[MoveZNodeRecursiveAction] =
    wire[MoveZNodeRecursiveActionHandler]

  override def updateZNodeAclListActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[UpdateZNodeAclListAction] =
    wire[UpdateZNodeAclListActionHandler]

  override def updateZNodeAclListRecursiveActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[UpdateZNodeAclListRecursiveAction] =
    wire[UpdateZNodeAclListRecursiveActionHandler]

  override def updateZNodeDataActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[UpdateZNodeDataAction] =
    wire[UpdateZNodeDataActionHandler]

  override def exportZNodesActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[ExportZNodesAction] =
    wire[ExportZNodesActionHandler]

  override def importZNodesActionHandler(
      curatorFramework: CuratorFramework
  ): ActionHandler[ImportZNodesAction] =
    wire[ImportZNodesActionHandler]
}
