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

package api.formats.json.zookeeper.znode

import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import org.scalatest.FlatSpec
import play.api.libs.json.{JsString, Writes}

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
class JsonZNodePathSpec extends FlatSpec with JsonZNodePath {

  "JsonZNodePath" should "be serialized as a string" in {
    val j = ZNodePath.parse("/node0/node1").get
    val s = implicitly[Writes[j.type]].writes(j)

    assertResult(JsString("/node0/node1"))(s)
  }
}
