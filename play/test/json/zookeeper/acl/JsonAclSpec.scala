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

import com.elkozmon.zoonavigator.core.zookeeper.acl.{Acl, AclId, Permission}
import org.scalatest.FlatSpec
import play.api.libs.json._
import com.elkozmon.zoonavigator.core.utils.CommonUtils._

import scala.language.postfixOps

class JsonAclSpec extends FlatSpec {

  private val jsonAcl =
    JsonAcl(Acl(AclId("world", "anyone"), Permission.All))

  private val stringAcl =
    """{"id":"anyone","scheme":"world","permissions":[]}"""

  "Serialized JsonAcl" should "be a JSON object with 'id' field" in {
    val j = implicitly[Writes[jsonAcl.type]].writes(jsonAcl)

    assert(j \ "id" isDefined)
  }

  it should "be a JSON object with 'scheme' field" in {
    val j = implicitly[Writes[jsonAcl.type]].writes(jsonAcl)

    assert(j \ "scheme" isDefined)
  }

  it should "be a JSON object with 'permissions' field" in {
    val j = implicitly[Writes[jsonAcl.type]].writes(jsonAcl)

    assert(j \ "permissions" isDefined)
  }

  "JsonAcl" should "be able to deserialize simple Acl" in {
    val j = implicitly[Reads[JsonAcl]].reads(Json.parse(stringAcl))

    assert(j.isSuccess).discard()

    j.map(_.underlying).foreach { acl =>
      assertResult("anyone")(acl.aclId.id).discard()
      assertResult("world")(acl.aclId.scheme).discard()
    }
  }
}
