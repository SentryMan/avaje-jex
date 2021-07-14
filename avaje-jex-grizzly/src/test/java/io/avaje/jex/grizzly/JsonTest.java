package io.avaje.jex.grizzly;

import io.avaje.jex.Jex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

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
        .get("/", ctx -> ctx.json(HelloDto.rob())) //.header("x2-foo","asd")
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
    assertThat(headers.firstValue("content-type").get()).isEqualTo("application/json");
  }

  @Test
  void stream_viaIterator() {
    final Stream<HelloDto> beanStream = pair.request()
      .path("iterate")
      .GET()
      .stream(HelloDto.class);

    // assert AutoCloseable iterator on the server-side was closed
    assertThat(ITERATOR.isClosed()).isTrue();
    // expect client gets the expected stream of beans
    assertCollectedStream(beanStream);
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
