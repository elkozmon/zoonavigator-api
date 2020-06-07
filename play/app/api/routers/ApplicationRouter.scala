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

package api.routers

import api.controllers.FrontendController
import config.PlayHttpContext
import play.api.http.HttpErrorHandler
import play.api.mvc.DefaultActionBuilder
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter

class ApplicationRouter(
    apiRouter: ApiRouter,
    frontendController: FrontendController,
    httpContext: PlayHttpContext,
    httpErrorHandler: HttpErrorHandler,
    actionBuilder: DefaultActionBuilder
) extends SimpleRouter {

  private val ctxUrlPrefix = httpContext.context.stripSuffix("/")
  private val apiUrlPrefix = ctxUrlPrefix.concat("/api")

  private val prefixedApiRouter = apiRouter.withPrefix(apiUrlPrefix)

  override def routes: Routes = {
    case p if p.uri.equals(apiUrlPrefix) || p.uri.startsWith(apiUrlPrefix.concat("/")) =>
      prefixedApiRouter.routes(p)

    case p if p.uri.startsWith(ctxUrlPrefix) =>
      frontendController.assetOrDefault(p.uri.stripPrefix(ctxUrlPrefix))

    case p =>
      actionBuilder.async(httpErrorHandler.onClientError(p, 404, "Not found"))
  }
}
