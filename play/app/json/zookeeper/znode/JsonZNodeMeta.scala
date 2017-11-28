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

package json.zookeeper.znode

import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeMeta
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

final case class JsonZNodeMeta(underlying: ZNodeMeta)

object JsonZNodeMeta {

  private val isoDateTimeFormatter = ISODateTimeFormat.dateTime()

  implicit object ZNodeMetaWrites extends Writes[JsonZNodeMeta] {
    override def writes(o: JsonZNodeMeta): JsValue =
      Json.obj(
        "creationId" -> o.underlying.creationId,
        "creationTime" -> o.underlying.creationTime
          .toString(isoDateTimeFormatter),
        "modifiedId" -> o.underlying.modifiedId,
        "modifiedTime" -> o.underlying.modifiedTime
          .toString(isoDateTimeFormatter),
        "dataLength" -> o.underlying.dataLength,
        "dataVersion" -> o.underlying.dataVersion.version,
        "aclVersion" -> o.underlying.aclVersion.version,
        "childrenVersion" -> o.underlying.childrenVersion.version,
        "childrenNumber" -> o.underlying.childrenNumber,
        "ephemeralOwner" -> o.underlying.ephemeralOwner
      )
  }

}
