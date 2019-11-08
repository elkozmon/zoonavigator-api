/*
 * Copyright (C) 2019  Ľuboš Kozmon <https://www.elkozmon.com>
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

import config.HttpContext
import play.api.http.HttpErrorHandler
import play.api.mvc._

class FrontendController(
    assets: Assets,
    httpContext: HttpContext,
    errorHandler: HttpErrorHandler,
    val controllerComponents: ControllerComponents
) extends BaseController {

  def index: Action[AnyContent] = assets.at("index.html")

  def assetOrDefault(resource: String): Action[AnyContent] = {
    val path = {
      val p = httpContext.context match {
        case "/" => resource
        case ctx => resource.stripPrefix(ctx)
      }

      p.stripPrefix("/").prepended('/')
    }

    if (path.startsWith("/api/") || path.equals("/api")) {
      Action.async { request =>
        errorHandler.onClientError(request, NOT_FOUND, "Not found")
      }
    } else if (path.contains(".")) {
      assets.at(path)
    } else {
      index
    }
  }
}
