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

package com.elkozmon.zoonavigator.core.action.actions

import com.elkozmon.zoonavigator.core.curator.TestingCuratorFrameworkProvider
import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.Null"))
class GetZNodeDataActionHandlerSpec extends FlatSpec {

  private val curatorFramework =
    TestingCuratorFrameworkProvider.getCuratorFramework(getClass.getName)

  private val executionContext = ExecutionContext.global

  "GetZNodeDataActionHandler" should "return empty byte array for ZNode with null data" in {
    curatorFramework
      .create()
      .forPath("/nullNode", null)
      .asUnit()

    val handler =
      new GetZNodeDataActionHandler(curatorFramework, executionContext)

    val action = GetZNodeDataAction(ZNodePath.unsafe("/nullNode"))

    val metaWithData = Await.result(handler.handle(action), Duration.Inf)

    assert(metaWithData.data.bytes.isEmpty)
  }
}
