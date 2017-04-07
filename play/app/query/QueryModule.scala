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

package query

import com.elkozmon.zoonavigator.core.query.QueryHandler
import com.elkozmon.zoonavigator.core.query.queries._
import org.apache.curator.framework.CuratorFramework
import query.dispatcher.DefaultQueryDispatcherProvider
import shapeless.HNil

trait QueryModule {

  def getZNodeAclListQueryHandler(curatorFramework: CuratorFramework): QueryHandler[GetZNodeAclQuery]

  def getZNodeDataQueryHandler(curatorFramework: CuratorFramework): QueryHandler[GetZNodeDataQuery]

  def getZNodeMetaQueryHandler(curatorFramework: CuratorFramework): QueryHandler[GetZNodeMetaQuery]

  def getZNodeChildrenQueryHandler(curatorFramework: CuratorFramework): QueryHandler[GetZNodeChildrenQuery]

  val queryDispatcherProvider = new DefaultQueryDispatcherProvider(
    curatorFramework =>
      getZNodeAclListQueryHandler(curatorFramework) ::
        getZNodeDataQueryHandler(curatorFramework) ::
        getZNodeMetaQueryHandler(curatorFramework) ::
        getZNodeChildrenQueryHandler(curatorFramework) ::
        HNil
  )
}
