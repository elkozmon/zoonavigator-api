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

package action.dispatcher

import com.elkozmon.zoonavigator.core.action.ActionDispatcher
import org.apache.curator.framework.CuratorFramework
import shapeless.HList

class DefaultActionDispatcherProvider[L <: HList](fn: CuratorFramework => L)
  extends ActionDispatcherProvider[L] {

  override def getDispatcher(curatorFramework: CuratorFramework): ActionDispatcher[L] =
    new ActionDispatcher(fn(curatorFramework))
}
