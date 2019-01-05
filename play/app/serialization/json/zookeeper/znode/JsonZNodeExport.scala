/*
 * Copyright (C) 2019  Ľuboš Kozmon
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

package serialization.json.zookeeper.znode

import com.elkozmon.zoonavigator.core.zookeeper.znode._
import play.api.libs.json._
import play.api.libs.functional.syntax._

trait JsonZNodeExport
    extends JsonZNodePath
    with JsonZNodeAcl
    with JsonZNodeData {

  private val zNodeExportReads = (
    (JsPath \ "acl").read[ZNodeAcl] and
      (JsPath \ "path").read[ZNodePath] and
      (JsPath \ "data").read[ZNodeData]
  ).apply(ZNodeExport.apply _)

  implicit object ZNodeExportFormat extends OFormat[ZNodeExport] {
    override def writes(o: ZNodeExport): JsObject =
      Json.obj(
        "acl" -> Json.toJson(o.acl),
        "path" -> Json.toJson(o.path),
        "data" -> Json.toJson(o.data)
      )

    override def reads(json: JsValue): JsResult[ZNodeExport] =
      zNodeExportReads.reads(json)
  }
}
