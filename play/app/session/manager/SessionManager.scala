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

package session.manager

import play.api.mvc.RequestHeader
import session.SessionToken

trait SessionManager {

  def newSession(): SessionToken

  def getSession(rh: RequestHeader): Option[SessionToken]

  def closeSession()(implicit token: SessionToken): Map[String, AnyRef]

  def getSessionData(key: String)(implicit token: SessionToken): Option[AnyRef]

  def putSessionData(key: String, value: AnyRef)(implicit token: SessionToken): Option[AnyRef]

  def removeSessionData(key: String)(implicit token: SessionToken): Option[AnyRef]
}
