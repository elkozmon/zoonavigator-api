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

package com.elkozmon.zoonavigator.core.curator

import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import org.apache.curator.framework.api.BackgroundPathAndBytesable
import org.apache.curator.framework.api.CuratorEvent

import scala.language.implicitConversions

trait PathAndBytesableOps {

  implicit def toPathableAsync[T](
      bp: BackgroundPathAndBytesable[T]
  ): PathableAsync =
    path =>
      tryTaskCreate[CuratorEvent] { (scheduler, callback) =>
        bp.inBackground(newEventCallback(callback), scheduler)
          .withUnhandledErrorListener(newErrorListener(callback))
          .forPath(path)
          .asUnit()
    }

  implicit def toPathAndBytesableAsync[T](
      bp: BackgroundPathAndBytesable[T]
  ): PathAndBytesableAsync =
    (path, bytes) =>
      tryTaskCreate[CuratorEvent] { (scheduler, callback) =>
        bp.inBackground(newEventCallback(callback), scheduler)
          .withUnhandledErrorListener(newErrorListener(callback))
          .forPath(path, bytes)
          .asUnit()
    }
}
