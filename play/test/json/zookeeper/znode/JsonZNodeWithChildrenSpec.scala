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

package json.zookeeper.znode

import com.elkozmon.zoonavigator.core.zookeeper.acl.Acl
import com.elkozmon.zoonavigator.core.zookeeper.acl.AclId
import com.elkozmon.zoonavigator.core.zookeeper.acl.Permission
import com.elkozmon.zoonavigator.core.zookeeper.znode._
import org.joda.time.DateTime
import org.scalatest.FlatSpec
import play.api.libs.json._

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
class JsonZNodeWithChildrenSpec extends FlatSpec {

  private val jsonZNodeMetaWith = JsonZNodeWithChildren(
    ZNodeWithChildren(
      ZNode(
        ZNodeAcl(List(Acl(AclId("world", "anyone"), Permission.All))),
        ZNodePath.parse("/hello").get,
        ZNodeData(Array.emptyByteArray),
        ZNodeMeta(
          0L,
          DateTime.now(),
          0L,
          DateTime.now(),
          0,
          ZNodeDataVersion(0L),
          ZNodeAclVersion(0L),
          ZNodeChildrenVersion(0L),
          0,
          0L
        )
      ),
      ZNodeChildren(List.empty)
    )
  )

  "Serialized JsonZNodeWithChildren" should "be a JSON object with 'children' field" in {
    val j = implicitly[Writes[jsonZNodeMetaWith.type]].writes(jsonZNodeMetaWith)

    assert(j \ "children" isDefined)
  }
}
