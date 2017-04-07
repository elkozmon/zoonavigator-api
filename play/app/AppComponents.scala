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

import java.util.concurrent.TimeUnit

import api.{ApiErrorHandler, ApiResponseFactory, JsonApiResponseFactory}
import com.elkozmon.zoonavigator.core.command.CommandHandler
import com.elkozmon.zoonavigator.core.command.commands._
import com.elkozmon.zoonavigator.core.curator.background.{BackgroundPromiseFactory, DefaultBackgroundPromiseFactory}
import com.elkozmon.zoonavigator.core.query.QueryHandler
import com.elkozmon.zoonavigator.core.query.queries._
import com.softwaremill.macwire._
import controllers.Assets
import curator.action.CuratorActionBuilder
import curator.provider.{CacheCuratorFrameworkProvider, CuratorCacheMaxAge, CuratorConnectTimeout, CuratorFrameworkProvider}
import org.apache.curator.framework.CuratorFramework
import play.api.ApplicationLoader.Context
import play.api._
import play.api.http.HttpErrorHandler
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.cors.{CORSConfig, CORSFilter}
import router.Routes
import session.SessionInactivityTimeout
import session.action.SessionActionBuilder
import session.manager.{ExpiringSessionManager, SessionManager}
import zookeeper.session.{DefaultZookeeperSessionHelper, ZookeeperSessionHelper}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class AppComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with AppModule {

  LoggerConfigurator(context.environment.classLoader)
    .foreach(_.configure(context.environment))

  lazy val assets: Assets = wire[Assets]

  private lazy val httpContext: String = configuration
    .getString("play.http.context")
    .getOrElse("/")

  override lazy val httpErrorHandler: HttpErrorHandler = {
    //noinspection ScalaUnusedSymbol
    def routerProvider: Option[Router] = Option(router)

    wire[ApiErrorHandler]
  }

  override lazy val router: Router =
    wire[Routes].withPrefix(httpContext)

  lazy val corsConfig: CORSConfig =
    CORSConfig.fromConfiguration(configuration)

  override def corsFilter: CORSFilter = {
    //noinspection ScalaUnusedSymbol
    val prefixes: Seq[String] = Seq(httpContext)
    wire[CORSFilter]
  }

  override lazy val httpFilters: Seq[EssentialFilter] =
    filters.filters

  lazy val sessionInactivityTimeout: SessionInactivityTimeout =
    SessionInactivityTimeout(
      new FiniteDuration(
        context.initialConfiguration
          .getLong("play.http.session.maxAge")
          .getOrElse(5 * 60 * 1000),
        TimeUnit.MILLISECONDS
      )
    )

  lazy val executionContextExecutor: ExecutionContextExecutor =
    ExecutionContext.global

  lazy val backgroundPromiseFactory: BackgroundPromiseFactory =
    wire[DefaultBackgroundPromiseFactory]

  lazy val curatorCacheMaxAge: CuratorCacheMaxAge =
    CuratorCacheMaxAge(
      FiniteDuration(
        context
          .initialConfiguration
          .getMilliseconds("zookeeper.client.maxAge")
          .getOrElse(5000L),
        TimeUnit.MILLISECONDS
      )
    )

  lazy val curatorConnectTimeout: CuratorConnectTimeout =
    CuratorConnectTimeout(
      FiniteDuration(
        context
          .initialConfiguration
          .getMilliseconds("zookeeper.client.connectTimeout")
          .getOrElse(5000L),
        TimeUnit.MILLISECONDS
      )
    )

  override val apiResponseFactory: ApiResponseFactory =
    wire[JsonApiResponseFactory]

  override val sessionManager: SessionManager =
    wire[ExpiringSessionManager]

  override val curatorFrameworkProvider: CuratorFrameworkProvider =
    wire[CacheCuratorFrameworkProvider]

  override val zookeeperSessionHelper: ZookeeperSessionHelper =
    wire[DefaultZookeeperSessionHelper]

  override lazy val curatorActionBuilder: CuratorActionBuilder =
    wire[CuratorActionBuilder]

  override lazy val sessionActionBuilder: SessionActionBuilder =
    wire[SessionActionBuilder]

  override def getZNodeAclListQueryHandler(curatorFramework: CuratorFramework): QueryHandler[GetZNodeAclQuery] =
    wire[GetZNodeAclQueryHandler]

  override def getZNodeChildrenQueryHandler(curatorFramework: CuratorFramework): QueryHandler[GetZNodeChildrenQuery] =
    wire[GetZNodeChildrenQueryHandler]

  override def getZNodeDataQueryHandler(curatorFramework: CuratorFramework): QueryHandler[GetZNodeDataQuery] =
    wire[GetZNodeDataQueryHandler]

  override def getZNodeMetaQueryHandler(curatorFramework: CuratorFramework): QueryHandler[GetZNodeMetaQuery] =
    wire[GetZNodeMetaQueryHandler]

  override def createZNodeCommandHandler(curatorFramework: CuratorFramework): CommandHandler[CreateZNodeCommand] =
    wire[CreateZNodeCommandHandler]

  override def deleteZNodeRecursiveCommandHandler(curatorFramework: CuratorFramework): CommandHandler[DeleteZNodeRecursiveCommand] =
    wire[DeleteZNodeRecursiveCommandHandler]

  override def forceDeleteZNodeRecursiveCommandHandler(curatorFramework: CuratorFramework): CommandHandler[ForceDeleteZNodeRecursiveCommand] =
    wire[ForceDeleteZNodeRecursiveCommandHandler]

  override def updateZNodeAclListCommandHandler(curatorFramework: CuratorFramework): CommandHandler[UpdateZNodeAclListCommand] =
    wire[UpdateZNodeAclListCommandHandler]

  override def updateZNodeAclListRecursiveCommandHandler(curatorFramework: CuratorFramework): CommandHandler[UpdateZNodeAclListRecursiveCommand] =
    wire[UpdateZNodeAclListRecursiveCommandHandler]

  override def updateZNodeDataCommandHandler(curatorFramework: CuratorFramework): CommandHandler[UpdateZNodeDataCommand] =
    wire[UpdateZNodeDataCommandHandler]
}
