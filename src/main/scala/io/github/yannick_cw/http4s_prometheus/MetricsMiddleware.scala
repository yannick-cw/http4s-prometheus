package io.github.yannick_cw.http4s_prometheus

import java.util.concurrent.TimeUnit

import cats.data.{Kleisli, OptionT}
import cats.effect.Effect
import io.prometheus.client._
import org.http4s.HttpService
import cats.implicits._
import org.http4s.dsl.impl.Path

import scala.concurrent.duration.{Duration, FiniteDuration}

class MetricsMiddleware(bucketsInSeconds: List[Double], registry: CollectorRegistry, paths: List[Path]) {

  private val httpRequestsTotal = Counter
    .build()
    .name(s"http_requests_total")
    .help(s"Total http requests received")
    .labelNames("path", "status")
    .register(registry)

  private val responseTime = Histogram
    .build()
    .name("http_requests_duration_seconds")
    .help("Histogram of the response time of http requests in Seconds")
    .labelNames("path", "status")
    .buckets(bucketsInSeconds: _*)
    .register(registry)

  private def collectMetrics[F[_]: Effect](startTime: FiniteDuration, code: String, serviceName: String): Unit = {
    val finishTime = Duration.fromNanos(System.nanoTime())
    httpRequestsTotal.labels(serviceName, code).inc()
    responseTime.labels(serviceName, code).observe((finishTime - startTime).toUnit(TimeUnit.SECONDS))
  }

  def collect[F[_]: Effect](service: HttpService[F]): HttpService[F] =
    Kleisli { req =>
      val path =
        paths.find(allowedPath => Path(req.uri.path).startsWith(allowedPath)).map(_.toString).getOrElse("unknown")
      val startTime = Duration.fromNanos(System.nanoTime())
      OptionT(
        service(req)
          .map { response =>
            collectMetrics(startTime, response.status.code.toString, path)
            response
          }
          .value
          .adaptError {
            case e =>
              collectMetrics(startTime, "500", path)
              e
          })
    }
}
object MetricsMiddleware {
  def apply(bucketsInSeconds: List[Double] = List(0.01, 0.025, 0.05, 0.075, 0.1, 0.125, 0.15, 0.175, 0.2, 0.225, 0.25,
              0.275, 0.3, 0.325, 0.35, 0.4, 0.45, 0.5, 0.6, 0.7, 1, 2, 3, 5, 10),
            registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
            paths: List[Path] = List.empty): MetricsMiddleware =
    new MetricsMiddleware(bucketsInSeconds, registry, paths)
}
