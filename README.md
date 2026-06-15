# errorgap-java

JVM notifier for [Errorgap](https://errorgap.com). Two artifacts:

- `io.errorgap:errorgap-core` — base SDK with no framework deps
- `io.errorgap:errorgap-spring-boot-starter` — Spring Boot 3.x auto-config

Requires Java 17+.

## Install (Maven)

```xml
<dependency>
  <groupId>io.errorgap</groupId>
  <artifactId>errorgap-spring-boot-starter</artifactId>
  <version>0.1.0</version>
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
```

The starter registers `Configuration`, `Client`, and a Spring MVC
`HandlerExceptionResolver` that reports unhandled controller exceptions.
Beans are only registered when `errorgap.project-slug` is set.

## Configure (plain Java)

```java
import io.errorgap.Configuration;
import io.errorgap.Errorgap;

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
