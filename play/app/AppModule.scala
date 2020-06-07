import api.controllers.FrontendController
import api.controllers.ApiController
import com.elkozmon.zoonavigator.core.action.ActionModule
import curator.provider.CuratorFrameworkProvider
import loggers.AppLogger
import play.api.BuiltInComponentsFromContext
import schedulers.BlockingScheduler
import schedulers.ComputingScheduler
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

//noinspection ScalaUnusedSymbol
trait AppModule extends ActionModule {
  self: BuiltInComponentsFromContext =>

  val actionModule: ActionModule = this

  val appLogger: AppLogger

  val curatorFrameworkProvider: CuratorFrameworkProvider

  val frontendController: FrontendController

  val apiController: ApiController

  val blockingScheduler: BlockingScheduler

  val computingScheduler: ComputingScheduler
}
