package io.avaje.jex.jetty;

import io.avaje.jex.ServerConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class JettyServerConfig implements ServerConfig {

  private boolean sessions = true;
  private boolean security = true;
  /**
   * Set true to use Loom virtual threads for ThreadPool.
   * This requires JDK 17 with Loom included.
   */
  private boolean virtualThreads;
  /**
   * Set maxThreads when using default QueuedThreadPool. Defaults to 200.
   */
  private int maxThreads;
  private SessionHandler sessionHandler;
  private ServletContextHandler contextHandler;
  private Server server;
  private final List<Consumer<JettyServerConfig>> configureCallback = new ArrayList<>();

  public boolean sessions() {
    return sessions;
  }

  public JettyServerConfig sessions(boolean sessions) {
    this.sessions = sessions;
    return this;
  }

  public boolean security() {
    return security;
  }

  public JettyServerConfig security(boolean security) {
    this.security = security;
    return this;
  }

  public boolean virtualThreads() {
    return virtualThreads;
  }

  /**
   * Set to true to use Loom virtual threads for the Jetty worker pool.
   */
  public JettyServerConfig virtualThreads(boolean virtualThreads) {
    this.virtualThreads = virtualThreads;
    return this;
  }

  public int maxThreads() {
    return maxThreads;
  }

  public JettyServerConfig maxThreads(int maxThreads) {
    this.maxThreads = maxThreads;
    return this;
  }

  public SessionHandler sessionHandler() {
    return sessionHandler;
  }

  /**
   * Set the SessionHandler to use. When not set one is created automatically.
   */
  public JettyServerConfig sessionHandler(SessionHandler sessionHandler) {
    this.sessionHandler = sessionHandler;
    return this;
  }

  public ServletContextHandler contextHandler() {
    return contextHandler;
  }

  /**
   * Set the ServletContextHandler to use. When not set the one is created automatically.
   */
  public JettyServerConfig contextHandler(ServletContextHandler contextHandler) {
    this.contextHandler = contextHandler;
    return this;
  }

  public Server server() {
    return server;
  }

  /**
   * Set the Jetty Server to use. When not set one is created automatically.
   */
  public JettyServerConfig server(Server server) {
    this.server = server;
    return this;
  }

  /**
   * Register a callback that is executed after the server and contextHandler have been
   * created but before the server has started.
   * <p>
   * When we use this to register filters to the ServletContextHandler or perform other
   * changes prior to the server starting.
   */
  public JettyServerConfig register(Consumer<JettyServerConfig> callback) {
    configureCallback.add(callback);
    return this;
  }

  /**
   * Run configuration callbacks prior to starting the server.
   */
  void postConfigure() {
    for (Consumer<JettyServerConfig> callback : configureCallback) {
      callback.accept(this);
    }
  }
}
