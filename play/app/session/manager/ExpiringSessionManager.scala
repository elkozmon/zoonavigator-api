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

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import api.ApiResponseFactory
import com.google.common.cache.CacheBuilder
import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import session.SessionInactivityTimeout
import session.SessionToken

import scala.collection.mutable
import scala.jdk.CollectionConverters._

class ExpiringSessionManager(apiResponseFactory: ApiResponseFactory, sessionInactivityTimeout: SessionInactivityTimeout)
    extends SessionManager {

  private val sessionCache =
    CacheBuilder
      .newBuilder()
      .expireAfterAccess(sessionInactivityTimeout.duration.toMillis, TimeUnit.MILLISECONDS)
      .build[SessionToken, mutable.Map[String, AnyRef]]()
      .asMap()
      .asScala

  override def newSession(): SessionToken = {
    val nextLong =
      ExpiringSessionManager.sessionIdCounter.getAndIncrement().toString
    val nextUuid = UUID.randomUUID().toString

    SessionToken(nextLong + nextUuid)
  }

  override def getSession(requestHeader: RequestHeader): Option[SessionToken] =
    requestHeader.headers
      .get(HeaderNames.AUTHORIZATION)
      .map(SessionToken)

  override def closeSession()(implicit token: SessionToken): Map[String, AnyRef] =
    sessionCache
      .remove(token)
      .getOrElse(mutable.Map.empty)
      .toMap

  override def getSessionData(key: String)(implicit token: SessionToken): Option[AnyRef] =
    sessionCache
      .get(token)
      .flatMap(_.get(key))

  override def putSessionData(key: String, value: AnyRef)(implicit token: SessionToken): Option[AnyRef] =
    sessionCache
      .getOrElseUpdate(token, mutable.Map.empty)
      .put(key, value)

  override def removeSessionData(key: String)(implicit token: SessionToken): Option[AnyRef] =
    sessionCache
      .get(token)
      .flatMap(_.remove(key))
}

object ExpiringSessionManager {
  private val sessionIdCounter = new AtomicLong(0L)
}
