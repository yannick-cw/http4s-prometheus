package io.github.yannick_cw.http4s_prometheus

import java.util.concurrent.TimeUnit

import cats.data.{Kleisli, OptionT}
import cats.effect.Effect
import io.prometheus.client._
import org.http4s.HttpService
import cats.implicits._

import scala.concurrent.duration.{Duration, FiniteDuration}

class MetricsMiddleware(serviceName: String, buckets: List[Double], registry: CollectorRegistry) {

  private val httpRequestsTotal = Counter
    .build()
    .name(s"http_requests_total")
    .help(s"Total http requests received")
    .labelNames("service", "status")
    .register(registry)

  private val responseTime =
    Histogram
      .build()
      .name("http_requests_duration_seconds")
      .help("Histogram of the response time of http requests in Seconds")
      .labelNames("service", "status")
      .buckets(buckets: _*)
      .register(registry)

  private def collectMetrics[F[_]: Effect](startTime: FiniteDuration, code: String): Unit = {
    val finishTime = Duration.fromNanos(System.nanoTime())
    httpRequestsTotal.labels(serviceName, code).inc()
    responseTime.labels(serviceName, code).observe((finishTime - startTime).toUnit(TimeUnit.SECONDS))
  }

  def collectFor[F[_]: Effect](service: HttpService[F]): HttpService[F] =
    Kleisli { req =>
      val startTime = Duration.fromNanos(System.nanoTime())
      OptionT(
        service(req)
          .map { response =>
            collectMetrics(startTime, response.status.code.toString)
            response
          }
          .value
          .adaptError {
            case e =>
              collectMetrics(startTime, "500")
              e
          })
    }
}
object MetricsMiddleware {
  def apply(serviceName: String,
            buckets: List[Double] = List(0.01, 0.025, 0.05, 0.075, 0.1, 0.125, 0.15, 0.175, 0.2, 0.225, 0.25, 0.275,
              0.3, 0.325, 0.35, 0.4, 0.45, 0.5, 0.6, 0.7, 1, 2, 3, 5, 10),
            registry: CollectorRegistry = CollectorRegistry.defaultRegistry): MetricsMiddleware =
    new MetricsMiddleware(serviceName, buckets, registry)
}
