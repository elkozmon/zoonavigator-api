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

package api.formats

import cats.data.Reader
import play.api.http.DefaultWriteables
import play.api.http.Writeable
import play.api.mvc.Result

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait Formats {

  implicit class WriteableResult[A](reader: Reader[Writeable[A], Result]) {

    def asResult(w: Writeable[A]): Result =
      reader(w)
  }

  implicit class AsyncWriteableResult[A](futureReader: Future[Reader[Writeable[A], Result]]) {

    def asResultAsync(w: Writeable[A])(ec: ExecutionContext): Future[Result] =
      futureReader.map(_.asResult(w))(ec)
  }
}
