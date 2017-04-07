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

import api.ApiResponseFactory
import com.softwaremill.macwire._
import command.CommandModule
import controllers._
import curator.action.CuratorActionBuilder
import curator.provider.CuratorFrameworkProvider
import filters.FiltersModule
import query.QueryModule
import session.action.SessionActionBuilder
import session.manager.SessionManager
import zookeeper.session.ZookeeperSessionHelper

//noinspection ScalaUnusedSymbol
trait AppModule extends CommandModule with QueryModule with FiltersModule {

  private val commandModule: CommandModule = this

  private val queryModule: QueryModule = this

  val apiResponseFactory: ApiResponseFactory

  val sessionManager: SessionManager

  val curatorFrameworkProvider: CuratorFrameworkProvider

  val zookeeperSessionHelper: ZookeeperSessionHelper

  val curatorActionBuilder: CuratorActionBuilder

  val sessionActionBuilder: SessionActionBuilder

  lazy val homeController: HomeController =
    wire[HomeController]

  lazy val zNodeController: ZNodeController =
    wire[ZNodeController]

  lazy val zSessionController: ZSessionController =
    wire[ZSessionController]
}
