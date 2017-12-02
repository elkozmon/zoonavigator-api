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

import java.util
import java.util.Collections

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.transaction._

object Transactions {

  def emptyTransaction(curator: CuratorFramework): CuratorTransactionFinal =
    new CuratorTransactionFinal {
      override def commit(): util.Collection[CuratorTransactionResult] =
        Collections.emptyList()
      override def setData(): TransactionSetDataBuilder =
        curator.inTransaction().setData()
      override def check(): TransactionCheckBuilder =
        curator.inTransaction().check()
      override def delete(): TransactionDeleteBuilder =
        curator.inTransaction().delete()
      override def create(): TransactionCreateBuilder =
        curator.inTransaction().create()
    }
}
