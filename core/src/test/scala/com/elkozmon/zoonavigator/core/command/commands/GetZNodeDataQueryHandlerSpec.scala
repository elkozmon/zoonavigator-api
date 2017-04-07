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

import com.elkozmon.zoonavigator.core.curator.TestingCuratorFrameworkProvider
import com.elkozmon.zoonavigator.core.curator.background.DefaultBackgroundPromiseFactory
import com.elkozmon.zoonavigator.core.query.queries.{GetZNodeDataQuery, GetZNodeDataQueryHandler}
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import org.scalatest.FlatSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

@SuppressWarnings(Array("org.wartremover.warts.Null"))
class GetZNodeDataQueryHandlerSpec extends FlatSpec {

  private val curatorFramework = TestingCuratorFrameworkProvider.getCuratorFramework(getClass.getName)

  private val executionContext = ExecutionContext.global

  private val backgroundPromiseFactory = new DefaultBackgroundPromiseFactory

  "GetZNodeDataQueryHandler" should "return empty byte array for ZNode with null data" in {
    curatorFramework
      .create()
      .forPath("/nullNode", null)
      .asUnit()

    val handler = new GetZNodeDataQueryHandler(
      curatorFramework,
      backgroundPromiseFactory,
      executionContext
    )

    val command = GetZNodeDataQuery(ZNodePath("/nullNode"))

    val metaWithData = Await.result(handler.handle(command), 1 second)

    assert(metaWithData.data.bytes.isEmpty)
  }
}
