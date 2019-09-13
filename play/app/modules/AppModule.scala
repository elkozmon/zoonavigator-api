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

package modules

import api.ApiResponseFactory
import controllers.{FrontendController, ZNodeController, ZSessionController}
import curator.action.CuratorActionBuilder
import curator.provider.CuratorFrameworkProvider
import modules.action.ActionModule
import modules.filters.FiltersModule
import play.api.BuiltInComponentsFromContext
import session.action.SessionActionBuilder
import session.manager.SessionManager
import zookeeper.session.ZooKeeperSessionHelper

//noinspection ScalaUnusedSymbol
trait AppModule extends ActionModule with FiltersModule {
  self: BuiltInComponentsFromContext =>

  val actionModule: ActionModule = this

  val apiResponseFactory: ApiResponseFactory

  val sessionManager: SessionManager

  val curatorFrameworkProvider: CuratorFrameworkProvider

  val zookeeperSessionHelper: ZooKeeperSessionHelper

  val curatorActionBuilder: CuratorActionBuilder

  val sessionActionBuilder: SessionActionBuilder

  val frontendController: FrontendController

  val zNodeController: ZNodeController

  val zSessionController: ZSessionController
}
