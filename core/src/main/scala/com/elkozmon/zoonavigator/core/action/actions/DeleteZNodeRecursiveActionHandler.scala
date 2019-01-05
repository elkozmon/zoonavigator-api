/*
 * Copyright (C) 2019  Ľuboš Kozmon
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

package com.elkozmon.zoonavigator.core.action.actions

import com.elkozmon.zoonavigator.core.action.ActionHandler
import com.elkozmon.zoonavigator.core.curator.Implicits._
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import monix.eval.Task
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.CuratorEvent

class DeleteZNodeRecursiveActionHandler(curatorFramework: CuratorFramework)
    extends ActionHandler[DeleteZNodeRecursiveAction] {

  override def handle(action: DeleteZNodeRecursiveAction): Task[Unit] =
    curatorFramework
      .delete()
      .deletingChildrenIfNeeded()
      .withVersion(action.expectedDataVersion.version.toInt)
      .forPathAsync(action.path.path)
      .map(discard[CuratorEvent])
}
