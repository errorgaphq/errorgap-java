# errorgap-java

JVM notifier for [Errorgap](https://errorgap.com). Two artifacts:

- `com.errorgap:errorgap-core` — base SDK with no framework deps
- `com.errorgap:errorgap-spring-boot-starter` — Spring Boot 3.x and 4.x auto-config

Requires Java 17+.

## Install (Maven)

```xml
<dependency>
  <groupId>com.errorgap</groupId>
  <artifactId>errorgap-spring-boot-starter</artifactId>
  <version>0.2.0</version>
</dependency>
```

For non-Spring apps, depend on `errorgap-core` directly.

## Configure (Spring Boot)

`application.properties`:

```properties
errorgap.endpoint=https://errorgap.example.com
errorgap.project-slug=your-project
errorgap.api-key=${ERRORGAP_API_KEY}
errorgap.environment=production
errorgap.release=@project.version@
errorgap.apm-enabled=true
errorgap.apm-sample-rate=1.0
errorgap.root-directory=${user.dir}
```

The starter registers `Configuration`, `Client`, and a Spring MVC
`HandlerExceptionResolver` that reports unhandled controller exceptions with
request parameters, user/session context, backtraces, and inline application
source excerpts. Sensitive parameter names such as `password`, `token`, and
`authorization` are redacted before delivery.
Beans are only registered when `errorgap.project-slug` is set.

## APM (Spring Boot)

With `errorgap.apm-enabled=true`, the starter automatically reports:

- Spring MVC request duration, status, method, and normalized route names
- JDBC query spans with normalized SQL and application call-site metadata
- Environment and release metadata on every transaction

Use `errorgap.apm-sample-rate` from `0.0` to `1.0` to control transaction
sampling. Errors are still reported when a transaction is not sampled.

Wrap background jobs with the injected `ErrorgapApm` bean:

```java
import com.errorgap.spring.ErrorgapApm;

errorgapApm.trackJob(
    ReceiptJob.class.getName(),
    "default",
    () -> receiptJob.run()
);
```

The wrapper reports job duration, status, queue, captured JDBC spans, and the
exception when the job fails. It rethrows the original runtime exception.

## Source excerpts

Set `errorgap.root-directory` to the application root (normally `${user.dir}`).
The SDK resolves application frames from standard Maven source directories and
includes a bounded source excerpt in each notice. For dependency/vendor frames,
it also reads a sibling Maven `-sources.jar` when one is installed; for example,
run `mvn dependency:sources` in environments where vendor excerpts are wanted.

## Configure (plain Java)

```java
import com.errorgap.Configuration;
import com.errorgap.Errorgap;

Configuration cfg = new Configuration()
    .setEndpoint("https://errorgap.example.com")
    .setProjectSlug("your-project")
    .setApiKey("flk_...");
Errorgap.init(cfg);
```

`Errorgap.init` defaults to installing
`Thread.setDefaultUncaughtExceptionHandler`. Pass `false` as the second
argument to skip.

## Manual notification

```java
try {
    risky();
} catch (Exception exc) {
    Errorgap.notify(exc, new NoticeOptions()
        .context(Map.of("component", "billing")));
    throw exc;
}
```

`notify` returns a `Client.Result` (`status`, `body`, `error`, `queued`).
The SDK never throws.

## Graceful shutdown

```java
Errorgap.flush(Duration.ofSeconds(5));   // wait for queued notices
Errorgap.shutdown(Duration.ofSeconds(5)); // flush + stop worker thread
```

## Development

```sh
./mvnw test
```

(or `mvn test` if you have Maven installed)

## License

MIT.
