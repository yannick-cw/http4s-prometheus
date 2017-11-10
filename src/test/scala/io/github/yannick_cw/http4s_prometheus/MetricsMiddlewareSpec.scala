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

  def testService: HttpService[IO] = HttpService[IO] {
    case GET -> Root => Ok()
  }

  val failingTestService: HttpService[IO] = HttpService[IO] {
    case GET -> Root => IO.raiseError[Response[IO]](new Exception("test exception"))
  }

  val testRequest: Request[IO] = Request[IO](Method.GET, Uri.uri("/"))

  it should "count the total http requests with service name and status" in {
    val registry = new CollectorRegistry

    val requests = 25
    val metrics  = MetricsMiddleware(registry = registry)
    List
      .fill(requests)(testRequest)
      .traverse(metrics.collect("countTest", testService).orNotFound(_))
      .unsafeRunSync()

    registry.getSampleValue("http_requests_total", Array("service", "status"), Array("countTest", "200")) shouldBe requests.toDouble
  }

  it should "count request that fail" in {
    val registry = new CollectorRegistry

    val metrics = MetricsMiddleware(registry = registry)

    assertThrows[Exception](metrics.collect("countTest", failingTestService).orNotFound(testRequest).unsafeRunSync())
    registry.getSampleValue("http_requests_total", Array("service", "status"), Array("countTest", "500")) shouldBe 1.0
  }

  it should "measure a response time for a request" in {
    val registry = new CollectorRegistry

    val metrics = MetricsMiddleware(registry = registry)

    metrics.collect("timingTest", testService).orNotFound(testRequest).unsafeRunSync()

    registry
      .getSampleValue("http_requests_duration_seconds_sum", Array("service", "status"), Array("timingTest", "200"))
      .toDouble should be > 0.0
  }

  it should "measure a response time if the service throws an exception" in {
    val registry = new CollectorRegistry

    val metrics = MetricsMiddleware(registry = registry)

    assertThrows[Exception](
      metrics.collect("timingThrowTest", failingTestService).orNotFound(testRequest).unsafeRunSync())
    registry
      .getSampleValue("http_requests_duration_seconds_sum",
                      Array("service", "status"),
                      Array("timingThrowTest", "500"))
      .toDouble should be > 0.0
  }

  it should "count request on multiple services" in {
    val registry = new CollectorRegistry

    val metrics = MetricsMiddleware(registry = registry)

    metrics.collect("service1", testService).orNotFound(testRequest).unsafeRunSync()
    metrics.collect("service2", testService).orNotFound(testRequest).unsafeRunSync()

    registry.getSampleValue("http_requests_total", Array("service", "status"), Array("service1", "200")) shouldBe 1.0
    registry.getSampleValue("http_requests_total", Array("service", "status"), Array("service2", "200")) shouldBe 1.0
  }
}
