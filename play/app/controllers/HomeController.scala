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

package controllers

import api.ApiResponse
import api.ApiResponseFactory
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents

class HomeController(
    apiResponseFactory: ApiResponseFactory,
    val controllerComponents: ControllerComponents
) extends BaseController {

  def getHealthCheck: Action[AnyContent] =
    Action { implicit request =>
      val resultReader = apiResponseFactory.okEmpty

      render {
        case Accepts.Json() =>
          resultReader(ApiResponse.writeJson[Nothing])
      }
    }
}
