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
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.{ACLable, BackgroundPathable}
import org.apache.zookeeper.data.{ACL, Id, Stat}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Try}

class UpdateZNodeAclListRecursiveCommandHandler(
  curatorFramework: CuratorFramework,
  backgroundPromiseFactory: BackgroundPromiseFactory,
  implicit val executionContextExecutor: ExecutionContextExecutor
) extends CommandHandler[UpdateZNodeAclListRecursiveCommand] {

  override def handle(command: UpdateZNodeAclListRecursiveCommand): Future[ZNodeMeta] = {
    // set acl for the parent node
    val futureMeta = setNodeAcl(
      command.path,
      command.acl,
      Some(command.expectedAclVersion)
    )

    // set acl for children nodes recursively
    val futureUnit = setChildrenAclRecursive(
      command.path,
      command.acl
    )

    for {
      meta <- futureMeta
      unit <- futureUnit
    } yield {
      meta
    }
  }

  private def setChildrenAclRecursive(
    parent: ZNodePath,
    acl: ZNodeAcl
  ): Future[Unit] = {
    for {
      children <- getNodeChildren(parent)
      _ <- Future.sequence(children.map(setNodeAcl(_, acl, None)))
      _ <- Future.sequence(children.map(setChildrenAclRecursive(_, acl)))
    } yield ()
  }

  private def setNodeAcl(
    path: ZNodePath,
    acl: ZNodeAcl,
    aclVersionOpt: Option[ZNodeAclVersion]
  ): Future[ZNodeMeta] = {
    val backgroundPromise = backgroundPromiseFactory.newBackgroundPromise {
      event =>
        ZNodeMeta.fromStat(event.getStat)
    }

    Try {
      val aclBuilder: ACLable[BackgroundPathable[Stat]] =
        aclVersionOpt match {
          case Some(aclVersion) =>
            curatorFramework.setACL().withVersion(aclVersion.version.toInt)
          case None =>
            curatorFramework.setACL()
        }

      aclBuilder
        .withACL(
          acl
            .aclList
            .map {
              rawAcl =>
                new ACL(
                  Permission.toZookeeperMask(rawAcl.permissions),
                  new Id(
                    rawAcl.aclId.scheme,
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
          path.path
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

  private def getNodeChildren(path: ZNodePath): Future[List[ZNodePath]] = {
    val backgroundPromise = backgroundPromiseFactory.newBackgroundPromise {
      event =>
        event
          .getChildren
          .asScala
          .map(name => ZNodePath(s"${path.path}/$name"))
          .toList
    }

    Try {
      curatorFramework
        .getChildren
        .inBackground(
          backgroundPromise.eventCallback,
          executionContextExecutor: Executor
        )
        .withUnhandledErrorListener(
          backgroundPromise.errorListener
        )
        .forPath(
          path.path
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
