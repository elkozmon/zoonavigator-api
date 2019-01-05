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

package serialization

import serialization.json.api.JsonApiResponse
import serialization.json.cats.JsonCofree
import serialization.json.zookeeper.{JsonConnectionParams, JsonSessionInfo}
import serialization.json.zookeeper.acl.JsonAcl
import serialization.json.zookeeper.znode._

object Json
    extends JsonApiResponse
    with JsonZNodeWithChildren
    with JsonZNodeMetaWith
    with JsonZNodeMeta
    with JsonZNodeChildren
    with JsonZNode
    with JsonSessionInfo
    with JsonConnectionParams
    with JsonAcl
    with JsonZNodeExport
    with JsonCofree
