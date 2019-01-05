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

import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeMeta
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

trait JsonZNodeMeta {

  private val isoDateTimeFormatter = ISODateTimeFormat.dateTime()

  implicit object ZNodeMetaWrites extends Writes[ZNodeMeta] {
    override def writes(o: ZNodeMeta): JsValue =
      Json.obj(
        "creationId" -> o.creationId,
        "creationTime" -> o.creationTime.toString(isoDateTimeFormatter),
        "modifiedId" -> o.modifiedId,
        "modifiedTime" -> o.modifiedTime.toString(isoDateTimeFormatter),
        "dataLength" -> o.dataLength,
        "dataVersion" -> o.dataVersion.version,
        "aclVersion" -> o.aclVersion.version,
        "childrenVersion" -> o.childrenVersion.version,
        "childrenNumber" -> o.childrenNumber,
        "ephemeralOwner" -> o.ephemeralOwner
      )
  }

}
