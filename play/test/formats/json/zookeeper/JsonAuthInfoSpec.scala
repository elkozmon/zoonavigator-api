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

package api.formats.json.zookeeper

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.Assertions
import play.api.libs.json._
import zookeeper.AuthInfo

class JsonAuthInfoSpec extends AnyFlatSpec with Assertions with JsonAuthInfo {

  "JsonZNodeWithChildren" should "deserialize simple auth info" in {
    val s = """{"scheme":"world","id":"anyone"}"""
    val j = implicitly[Reads[AuthInfo]].reads(Json.parse(s))

    assert(j.isSuccess)
  }
}
