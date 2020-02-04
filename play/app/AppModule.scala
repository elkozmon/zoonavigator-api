import api.controllers.FrontendController
import api.controllers.ZNodeController
import api.controllers.ZSessionController
import api.ApiResponseFactory
import com.elkozmon.zoonavigator.core.action.ActionModule
import curator.action.CuratorActionBuilder
import curator.provider.CuratorFrameworkProvider
import loggers.AppLogger
import play.api.BuiltInComponentsFromContext
import schedulers.BlockingScheduler
import schedulers.ComputingScheduler
import session.action.SessionActionBuilder
import session.manager.SessionManager
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

import zookeeper.session.ZooKeeperSessionHelper

//noinspection ScalaUnusedSymbol
trait AppModule extends ActionModule {
  self: BuiltInComponentsFromContext =>

  val actionModule: ActionModule = this

  val appLogger: AppLogger

  val apiResponseFactory: ApiResponseFactory

  val sessionManager: SessionManager

  val curatorFrameworkProvider: CuratorFrameworkProvider

  val zookeeperSessionHelper: ZooKeeperSessionHelper

  val curatorActionBuilder: CuratorActionBuilder

  val sessionActionBuilder: SessionActionBuilder

  val frontendController: FrontendController

  val zNodeController: ZNodeController

  val zSessionController: ZSessionController

  val blockingScheduler: BlockingScheduler

  val computingScheduler: ComputingScheduler
}
