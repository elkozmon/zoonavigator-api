/*
 * Copyright (C) 2020  Ľuboš Kozmon <https://www.elkozmon.com>
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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.Assertions
import play.api.libs.json.JsString
import play.api.libs.json.Writes

import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeData

import java.util.Base64

class JsonZNodeDataSpec extends AnyFlatSpec with Assertions with JsonZNodeData {

  "JsonZNodeData" should "be serialized as a base64 string" in {
    val fooBytes = "foo".getBytes

    val j = ZNodeData(fooBytes)
    val s = implicitly[Writes[ZNodeData]].writes(j)

    assertResult(JsString(Base64.getEncoder.encodeToString(fooBytes)))(s)
  }
}
