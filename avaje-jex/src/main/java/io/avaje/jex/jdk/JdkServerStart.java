package io.avaje.jex.jdk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import io.avaje.applog.AppLog;
import io.avaje.jex.AppLifecycle;
import io.avaje.jex.Jex;
import io.avaje.jex.core.SpiServiceManager;
import io.avaje.jex.routes.SpiRoutes;

public final class JdkServerStart {

  private static final System.Logger log = AppLog.getLogger("io.avaje.jex");

  public Jex.Server start(Jex jex, SpiRoutes routes, SpiServiceManager serviceManager) {
    try {
      var port = new InetSocketAddress(jex.config().port());
      final var sslContext = jex.config().sslContext();

      final HttpServer server;
      final String scheme;
      if (sslContext != null) {
        var httpsServer = HttpsServer.create(port, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        server = httpsServer;
        scheme = "https";
      } else {
        scheme = "http";
        server = HttpServer.create(port, 0);
      }

      final var manager = new CtxServiceManager(serviceManager, scheme, "");

      var handler = new BaseHandler(routes);
      var context = server.createContext("/", handler);
      context.getFilters().add(new RoutingFilter(routes, manager, jex.config().compression()));
      context.getFilters().addAll(routes.filters());
      server.setExecutor(jex.config().executor());
      server.start();

      jex.lifecycle().status(AppLifecycle.Status.STARTED);
      log.log(
          Level.INFO,
          "started com.sun.net.httpserver.HttpServer on port %s://%s".formatted(scheme, port));
      return new JdkJexServer(server, jex.lifecycle(), handler);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
