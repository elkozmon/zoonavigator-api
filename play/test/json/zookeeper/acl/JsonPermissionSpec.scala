/*
 * Copyright (C) 2018  Ľuboš Kozmon
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

package json.zookeeper.acl

import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import org.scalatest.FlatSpec
import play.api.libs.json._

class JsonPermissionSpec extends FlatSpec {

  "JsonPermission" should "serialize Create permission as a JSON string 'create'" in {
    val j = implicitly[Writes[JsonPermission]].writes(JsonPermission(Permission.Create))

    assertResult(JsString("create"))(j)
  }

  it should "serialize Write permission as a JSON string 'write'" in {
    val j = implicitly[Writes[JsonPermission]].writes(JsonPermission(Permission.Write))

    assertResult(JsString("write"))(j)
  }

  it should "serialize Delete permission as a JSON string 'delete'" in {
    val j = implicitly[Writes[JsonPermission]].writes(JsonPermission(Permission.Delete))

    assertResult(JsString("delete"))(j)
  }

  it should "serialize Read permission as a JSON string 'read'" in {
    val j = implicitly[Writes[JsonPermission]].writes(JsonPermission(Permission.Read))

    assertResult(JsString("read"))(j)
  }

  it should "serialize Admin permission as a JSON string 'admin'" in {
    val j = implicitly[Writes[JsonPermission]].writes(JsonPermission(Permission.Admin))

    assertResult(JsString("admin"))(j)
  }

  it should "deserialize 'create' string as Create permission" in {
    val j = implicitly[Reads[JsonPermission]].reads(JsString("create"))

    assertResult(JsSuccess(JsonPermission(Permission.Create)))(j)
  }

  it should "deserialize 'write' string as Write permission" in {
    val j = implicitly[Reads[JsonPermission]].reads(JsString("write"))

    assertResult(JsSuccess(JsonPermission(Permission.Write)))(j)
  }

  it should "deserialize 'delete' string as Delete permission" in {
    val j = implicitly[Reads[JsonPermission]].reads(JsString("delete"))

    assertResult(JsSuccess(JsonPermission(Permission.Delete)))(j)
  }

  it should "deserialize 'read' string as Read permission" in {
    val j = implicitly[Reads[JsonPermission]].reads(JsString("read"))

    assertResult(JsSuccess(JsonPermission(Permission.Read)))(j)
  }

  it should "deserialize 'admin' string as Admin permission" in {
    val j = implicitly[Reads[JsonPermission]].reads(JsString("admin"))

    assertResult(JsSuccess(JsonPermission(Permission.Admin)))(j)
  }
}
