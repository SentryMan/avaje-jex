package io.avaje.jex.jdk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.*;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import io.avaje.applog.AppLog;
import io.avaje.jex.AppLifecycle;
import io.avaje.jex.Jex;
import io.avaje.jex.routes.SpiRoutes;
import io.avaje.jex.spi.SpiServiceManager;

public class JdkServerStart {

  private static final System.Logger log = AppLog.getLogger("io.avaje.jex");

  public Jex.Server start(Jex jex, SpiRoutes routes, SpiServiceManager serviceManager) {
    final ServiceManager manager = new ServiceManager(serviceManager, "http", "");

    try {
      final HttpServer server;

      var port = new InetSocketAddress(jex.config().port());
      final var sslContext = jex.config().sslContext();

      if (sslContext != null) {
        var httpsServer = HttpsServer.create(port, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        server = httpsServer;
      } else {
        server = HttpServer.create(port, 0);
      }

      var handler = new BaseHandler(routes);
      var context = server.createContext("/", handler);
      context.getFilters().add(new RoutingFilter(routes, manager));
      context.getFilters().addAll(routes.filters());
      server.setExecutor(Executors.newThreadPerTaskExecutor(jex.config().threadFactory()));
      server.start();

      jex.lifecycle().status(AppLifecycle.Status.STARTED);
      String jexVersion = Jex.class.getPackage().getImplementationVersion();
      log.log(Level.INFO, "started server on port {0,number,#} version {1}", port, jexVersion);
      return new JdkJexServer(server, jex.lifecycle(), handler);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}