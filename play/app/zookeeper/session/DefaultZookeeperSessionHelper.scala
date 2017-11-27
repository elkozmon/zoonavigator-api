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

package zookeeper.session

import session.SessionToken
import session.manager.SessionManager
import zookeeper.ConnectionParams

class DefaultZookeeperSessionHelper extends ZookeeperSessionHelper {

  private final val ConnectionParamsKey = "zkConnectionParams"

  override def setConnectionParams(params: ConnectionParams)(
      implicit token: SessionToken,
      manager: SessionManager
  ): Option[ConnectionParams] =
    manager
      .putSessionData(ConnectionParamsKey, params)
      .collect {
        case cp: ConnectionParams => cp
      }

  override def getConnectionParams(
      implicit token: SessionToken,
      manager: SessionManager
  ): Option[ConnectionParams] =
    manager
      .getSessionData(ConnectionParamsKey)
      .collect {
        case cp: ConnectionParams => cp
      }
}
