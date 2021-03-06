package io.github.yannick_cw.http4s_prometheus

import cats.effect.IO
import io.prometheus.client.CollectorRegistry
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpService, Method, Request, Uri}
import org.scalatest.{FlatSpec, Matchers}

class MetricsServiceSpec extends FlatSpec with Http4sDsl[IO] with Matchers {

  val registry = new CollectorRegistry()

  val testService: HttpService[IO] = HttpService[IO] {
    case GET -> Root => Ok()
  }
  val testRequest: Request[IO] = Request[IO](Method.GET, Uri.uri("/"))

  val metrics = MetricsMiddleware(registry = registry)

  behavior of "MetricsService"

  it should "return metrics as text" in {
    metrics.collect("collector", testService).orNotFound(testRequest).unsafeRunSync()

    MetricsService[IO](registry)
      .orNotFound(testRequest)
      .flatMap(_.as[String])
      .unsafeRunSync() should include("""http_requests_total{service="collector",status="200",} 1.0""")
  }
}
