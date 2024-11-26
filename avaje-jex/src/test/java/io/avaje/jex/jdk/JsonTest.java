package io.avaje.jex.jdk;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.avaje.jex.Jex;

class JsonTest {

  static List<HelloDto> HELLO_BEANS = asList(HelloDto.rob(), HelloDto.fi());

  static AutoCloseIterator<HelloDto> ITERATOR = createBeanIterator();

  private static AutoCloseIterator<HelloDto> createBeanIterator() {
    return new AutoCloseIterator<>(HELLO_BEANS.iterator());
  }

  static TestPair pair = init();

  static TestPair init() {
    Jex app = Jex.create()
      .routing(routing -> routing
        .get("/", ctx -> ctx.json(HelloDto.rob()).status(200))
        .get("/iterate", ctx -> ctx.jsonStream(ITERATOR))
        .get("/stream", ctx -> ctx.jsonStream(HELLO_BEANS.stream()))
        .post("/", ctx -> ctx.text("bean[" + ctx.bodyAsClass(HelloDto.class) + "]")));

    return TestPair.create(app);
  }

  @AfterAll
  static void end() {
    pair.shutdown();
  }

  @Test
  void get() {

    var bean = pair.request()
      .GET()
      .bean(HelloDto.class);

    assertThat(bean.id).isEqualTo(42);
    assertThat(bean.name).isEqualTo("rob");

    final HttpResponse<String> hres = pair.request()
      .GET().asString();

    final HttpHeaders headers = hres.headers();
    assertThat(headers.firstValue("Content-Type").get()).isEqualTo("application/json");
  }

  @Test
  void stream_viaIterator() {
    final Stream<HelloDto> beanStream = pair.request()
      .path("iterate")
      .GET()
      .stream(HelloDto.class);

    // expect client gets the expected stream of beans
    assertCollectedStream(beanStream);
    // assert AutoCloseable iterator on the server-side was closed
    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
    assertThat(ITERATOR.isClosed()).isTrue();
  }

  @Test
  void stream() {
    final Stream<HelloDto> beanStream = pair.request()
      .path("stream")
      .GET()
      .stream(HelloDto.class);

    assertCollectedStream(beanStream);
  }

  private void assertCollectedStream(Stream<HelloDto> beanStream) {
    final List<HelloDto> collectedBeans = beanStream.collect(toList());
    assertThat(collectedBeans).hasSize(2);

    final HelloDto first = collectedBeans.get(0);
    assertThat(first.id).isEqualTo(42);
    assertThat(first.name).isEqualTo("rob");

    final HelloDto second = collectedBeans.get(1);
    assertThat(second.id).isEqualTo(45);
    assertThat(second.name).isEqualTo("fi");
  }

  @Test
  void post() {
    HelloDto dto = new HelloDto();
    dto.id = 42;
    dto.name = "rob was here";

    var res = pair.request()
      .body(dto)
      .POST().asString();

    assertThat(res.body()).isEqualTo("bean[id:42 name:rob was here]");
    assertThat(res.statusCode()).isEqualTo(200);

    dto.id = 99;
    dto.name = "fi";

    res = pair.request()
      .body(dto)
      .POST().asString();

    assertThat(res.body()).isEqualTo("bean[id:99 name:fi]");
    assertThat(res.statusCode()).isEqualTo(200);
  }

}
