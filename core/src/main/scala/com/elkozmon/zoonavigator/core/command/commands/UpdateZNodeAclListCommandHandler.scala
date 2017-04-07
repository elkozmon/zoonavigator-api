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

package com.elkozmon.zoonavigator.core.command.commands

import java.util.concurrent.Executor

import com.elkozmon.zoonavigator.core.command.CommandHandler
import com.elkozmon.zoonavigator.core.curator.background.BackgroundPromiseFactory
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.acl.{Permission, Scheme}
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeMeta
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.data.{ACL, Id}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Try}

class UpdateZNodeAclListCommandHandler(
  curatorFramework: CuratorFramework,
  backgroundPromiseFactory: BackgroundPromiseFactory,
  executionContextExecutor: ExecutionContextExecutor
) extends CommandHandler[UpdateZNodeAclListCommand] {

  override def handle(command: UpdateZNodeAclListCommand): Future[ZNodeMeta] = {
    val backgroundPromise = backgroundPromiseFactory.newBackgroundPromise {
      event =>
        ZNodeMeta.fromStat(event.getStat)
    }

    Try {
      curatorFramework
        .setACL()
        .withVersion(
          command.expectedAclVersion.version.toInt
        )
        .withACL(
          command
            .acl
            .aclList
            .map {
              rawAcl =>
                new ACL(
                  Permission.toZookeeperMask(rawAcl.permissions),
                  new Id(
                    Scheme.toZookeeperScheme(rawAcl.aclId.scheme),
                    rawAcl.aclId.id
                  )
                )
            }
            .asJava
        )
        .inBackground(
          backgroundPromise.eventCallback,
          executionContextExecutor: Executor
        )
        .withUnhandledErrorListener(
          backgroundPromise.errorListener
        )
        .forPath(
          command.path.path
        )
        .asUnit()
    } match {
      case Failure(throwable) =>
        backgroundPromise
          .promise
          .tryFailure(throwable)
          .asUnit()
      case _ =>
    }

    backgroundPromise.promise.future
  }
}
