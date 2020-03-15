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

package api.controllers

import config.ApplicationConfig
import play.api.libs.json._
import play.api.mvc.AbstractController
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc._

class ApplicationController(applicationConfig: ApplicationConfig, controllerComponents: ControllerComponents)
    extends AbstractController(controllerComponents) {

  import api.formats.Json._

  def getConfig: Action[AnyContent] =
    Action { implicit request =>
      render {
        case Accepts.Json() =>
          Ok(Json.toJson(applicationConfig))
      }
    }
}
