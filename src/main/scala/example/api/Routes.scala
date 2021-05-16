package example.api

import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.Server
import zio.Task
import zio.interop.catz._

object Routes {

  private val dsl = Http4sDsl[Task]
  import dsl._

  def routes = HttpRoutes
    .of[Task] {
      case GET -> Root / "users" / IntVar(id) => {
        Created("A value here")
      }
      case POST -> Root / "users" => {
        Created("And another one here")
      }
    }
    .orNotFound

}
