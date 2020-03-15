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

package config

import java.nio.charset.StandardCharsets

import com.typesafe.config.Config
import play.api.ConfigLoader
import zookeeper.AuthInfo
import zookeeper.ConnectionString

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters._

final case class ApplicationConfig(
    requestTimeout: FiniteDuration,
    autoConnect: Option[String],
    connections: List[ApplicationConfig.Connection]
)

object ApplicationConfig {

  implicit val configLoader: ConfigLoader[ApplicationConfig] =
    (root: Config, path: String) => {
      val config = root.getConfig(path)

      ApplicationConfig(
        config.getDuration("requestTimeout").toScala,
        if (config.hasPath("autoConnect")) {
          Some(config.getString("autoConnect"))
        } else {
          None
        },
        config.getObjectList("connections").asScala.toList.map { o =>
          val c = o.toConfig
          val authList = c.getObjectList("auth").asScala.toList.map { o =>
            val c = o.toConfig
            AuthInfo(c.getString("scheme"), c.getString("id").getBytes(StandardCharsets.UTF_8))
          }
          Connection(c.getString("name"), ConnectionString(c.getString("conn")), authList)
        }
      )
    }

  final case class Connection(name: String, connectionString: ConnectionString, authInfoList: List[AuthInfo])
}
