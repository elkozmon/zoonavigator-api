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

package command

import com.elkozmon.zoonavigator.core.command.CommandHandler
import com.elkozmon.zoonavigator.core.command.commands._
import command.dispatcher.DefaultCommandDispatcherProvider
import org.apache.curator.framework.CuratorFramework
import shapeless.HNil

trait CommandModule {

  def createZNodeCommandHandler(curatorFramework: CuratorFramework): CommandHandler[CreateZNodeCommand]

  def deleteZNodeRecursiveCommandHandler(curatorFramework: CuratorFramework): CommandHandler[DeleteZNodeRecursiveCommand]

  def forceDeleteZNodeRecursiveCommandHandler(curatorFramework: CuratorFramework): CommandHandler[ForceDeleteZNodeRecursiveCommand]

  def updateZNodeAclListCommandHandler(curatorFramework: CuratorFramework): CommandHandler[UpdateZNodeAclListCommand]

  def updateZNodeAclListRecursiveCommandHandler(curatorFramework: CuratorFramework): CommandHandler[UpdateZNodeAclListRecursiveCommand]

  def updateZNodeDataCommandHandler(curatorFramework: CuratorFramework): CommandHandler[UpdateZNodeDataCommand]

  val commandDispatcherProvider = new DefaultCommandDispatcherProvider(
    curatorFramework =>
      createZNodeCommandHandler(curatorFramework) ::
        deleteZNodeRecursiveCommandHandler(curatorFramework) ::
        forceDeleteZNodeRecursiveCommandHandler(curatorFramework) ::
        updateZNodeAclListCommandHandler(curatorFramework) ::
        updateZNodeAclListRecursiveCommandHandler(curatorFramework) ::
        updateZNodeDataCommandHandler(curatorFramework) ::
        HNil
  )
}
