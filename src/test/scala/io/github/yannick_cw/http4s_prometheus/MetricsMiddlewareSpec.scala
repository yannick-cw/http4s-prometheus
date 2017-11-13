package io.github.yannick_cw.http4s_prometheus

import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import cats.effect.IO
import cats.implicits._
import io.prometheus.client.CollectorRegistry
import org.http4s.{HttpService, Method, Request, Response, Uri}
import org.http4s.dsl.Http4sDsl

class MetricsMiddlewareSpec extends FlatSpec with Matchers with Http4sDsl[IO] with BeforeAndAfterEach {

  behavior of "MetricsMiddleware"

  override protected def afterEach(): Unit = CollectorRegistry.defaultRegistry.clear()

  val registry: CollectorRegistry = CollectorRegistry.defaultRegistry

  val testService: HttpService[IO] = HttpService[IO] {
    case GET -> Root => Ok()
  }

  val failingTestService: HttpService[IO] = HttpService[IO] {
    case GET -> Root => IO.raiseError[Response[IO]](new Exception("test exception"))
  }

  val testRequest: Request[IO] = Request[IO](Method.GET, Uri.uri("/"))

  it should "count the total http requests with given default service name and status" in {
    val requests = 25
    val metrics  = MetricsMiddleware(registry = registry)
    List
      .fill(requests)(testRequest)
      .traverse(metrics.collect(testService).orNotFound(_))
      .unsafeRunSync()

    registry.getSampleValue("http_requests_total", Array("path", "status"), Array("unknown", "200")) shouldBe requests.toDouble
  }

  it should "count request that fail" in {
    val metrics = MetricsMiddleware(registry = registry)

    assertThrows[Exception](metrics.collect(failingTestService).orNotFound(testRequest).unsafeRunSync())
    registry.getSampleValue("http_requests_total", Array("path", "status"), Array("unknown", "500")) shouldBe 1.0
  }

  it should "measure a response time for a request" in {
    val metrics = MetricsMiddleware(registry = registry)

    metrics.collect(testService).orNotFound(testRequest).unsafeRunSync()

    registry
      .getSampleValue("http_requests_duration_seconds_sum", Array("path", "status"), Array("unknown", "200"))
      .toDouble should be > 0.0
  }

  it should "measure a response time if the service throws an exception" in {
    val metrics = MetricsMiddleware(registry = registry)

    assertThrows[Exception](metrics.collect(failingTestService).orNotFound(testRequest).unsafeRunSync())
    registry
      .getSampleValue("http_requests_duration_seconds_sum", Array("path", "status"), Array("unknown", "500"))
      .toDouble should be > 0.0
  }

  it should "add the path name if it is whitelisted" in {

    val req: Request[IO] = Request[IO](Method.GET, Uri.uri("/test"))
    val service: HttpService[IO] = HttpService[IO] {
      case GET -> Root / "test" => Ok()
    }

    val path: Path = Root / "test"
    val metrics    = MetricsMiddleware(paths = List(path))
    metrics.collect(service).orNotFound(req).unsafeRunSync()

    registry.getSampleValue("http_requests_total", Array("path", "status"), Array("/test", "200")) shouldBe 1.0
  }

  it should "add the path name for more complex requests" in {
    val req: Request[IO] = Request[IO](Method.GET, Uri.uri("/test/more/23?q=whatever"))
    val service: HttpService[IO] = HttpService[IO] {
      case GET -> Root / "test" / "more" / _ => Ok()
    }

    val path: Path = Root / "test" / "more"
    val metrics    = MetricsMiddleware(paths = List(path))
    metrics.collect(service).orNotFound(req).unsafeRunSync()

    registry.getSampleValue("http_requests_total", Array("path", "status"), Array("/test/more", "200")) shouldBe 1.0
  }
}
