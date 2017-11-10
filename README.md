## http4s-prometheus exporter

Provides instumentation to wrap http4s `HttpService[F]` with a prometheus metric collecting middleware.

### Usage

Wrap services you want to monitor and provide a metrics service endpoint for prometheus:
```scala
  import io.github.yannick_cw.http4s_prometheus.{MetricsMiddleware, MetricsService}

  private val M = MetricsMiddleware()
  val httpService  = HttpService[IO] { case GET -> Root / "hi" => Ok() }
  val httpService2 = HttpService[IO] { case GET -> Root / "Ups" => NotFound() }

  val routing = Router(
    "one"         -> M.collect("service 1 name", httpService),
    "two"         -> M.collect("service 2 name", httpService2),
    "/metrics" -> MetricsService[IO]()
  )
```
Via `/metrics` prometheus can now access the http request counter
```
# HELP http_requests_total Total http requests received
# TYPE http_requests_total counter
http_requests_total{service="service 1 name",status="200",} 10.0
```
and a request duration histogram in seconds
```
# HELP http_requests_duration_seconds Histogram of the response time of http requests in Seconds
# TYPE http_requests_duration_seconds histogram
http_requests_duration_seconds_bucket{service="service 2 name",status="404",le="0.01",} 9.0
```

### Additional settings

If you want you can supply the buckets for the Histogram and a custom `CollectorRegistry`
```scala
MetricsMiddleware(List(0.01, 0.025), CollectorRegistry.defaultRegistry)
```
