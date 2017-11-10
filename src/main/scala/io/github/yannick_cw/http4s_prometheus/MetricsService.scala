package io.github.yannick_cw.http4s_prometheus

import java.io.{StringWriter, Writer}
import java.util

import cats.effect.Effect
import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import org.http4s.{Header, HttpService}
import org.http4s.dsl.Http4sDsl

object MetricsService {

  private def toText(metrics: util.Enumeration[MetricFamilySamples]): String = {
    val writer: Writer = new StringWriter()
    TextFormat.write004(writer, metrics)
    writer.toString
  }

  def apply[F[_]: Effect](registry: CollectorRegistry = CollectorRegistry.defaultRegistry): HttpService[F] = {
    val S = new Http4sDsl[F]() {}
    import S._

    HttpService[F] {
      case GET -> Root =>
        Ok()
          .withBody(toText(registry.metricFamilySamples()))
          .putHeaders(Header("Content-Type", TextFormat.CONTENT_TYPE_004))
    }
  }
}
