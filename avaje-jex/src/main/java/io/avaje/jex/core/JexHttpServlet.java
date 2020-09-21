package io.avaje.jex.core;

import io.avaje.jex.Context;
import io.avaje.jex.JexConfig;
import io.avaje.jex.Routing;
import io.avaje.jex.http.NotFoundResponse;
import io.avaje.jex.spi.SpiRoutes;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

class JexHttpServlet extends HttpServlet {

  //private static final String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

  private final SpiRoutes routes;

  private final ServiceManager manager;

  private final HttpMethodMap methodMap = new HttpMethodMap();

  private final boolean prefer405;

  public JexHttpServlet(JexConfig config, SpiRoutes routes, ServiceManager manager) {
    this.routes = routes;
    this.manager = manager;
    this.prefer405 = config.isPrefer405();
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse res) {

      final Routing.Type routeType = method(req);
      final String uri = req.getRequestURI();
      SpiRoutes.Entry route = routes.match(routeType, uri);
      if (route == null) {
        Context ctx = new JexHttpContext(manager, req, res, Collections.emptyMap(), uri);
        try {
          processNoRoute(ctx, uri, routeType);
          routes.after(uri, ctx);
        } catch (Exception e) {
          handleException(ctx, e);
        }
      } else {
        final Map<String, String> pathParams = route.pathParams(uri);
        Context ctx = new JexHttpContext(manager, req, res, pathParams, route.matchPath());
        try {
          processRoute(ctx, uri, route);
          routes.after(uri, ctx);
        } catch (Exception e) {
          handleException(ctx, e);
        }
      }
  }

  private void handleException(Context ctx, Exception e) {
    manager.handleException(ctx, e);
  }

  private void processRoute(Context ctx, String uri, SpiRoutes.Entry route) {
    routes.before(uri, ctx);
    route.handle(ctx);
  }

  private void processNoRoute(Context ctx, String uri, Routing.Type routeType) {
    routes.before(uri, ctx);
    if (routeType == Routing.Type.HEAD && hasGetHandler(uri)) {
      processHead(ctx);
      return;
    }
    if (routeType == Routing.Type.GET || routeType == Routing.Type.HEAD) {
      // todo: check if handled by static resource
      // if (config.inner.resourceHandler?.handle(wrappedReq, JavalinResponseWrapper(rawRes, rwc)) == true) return@tryWithExceptionMapper
      // todo: check if handled by singlePageHandler
      //if (config.inner.singlePageHandler.handle(ctx)) return@tryWithExceptionMapper
    }
//    if (routeType == Routing.Type.OPTIONS && isCorsEnabled(config)) { // CORS is enabled, so we return 200 for OPTIONS
//      return@tryWithExceptionMapper
//    }
//    if (prefer405) {
//      //&& availableHandlerTypes.isNotEmpty()
//      //val availableHandlerTypes = MethodNotAllowedUtil.findAvailableHttpHandlerTypes(matcher, requestUri)
//      //throw MethodNotAllowedResponse(details = MethodNotAllowedUtil.getAvailableHandlerTypes(ctx, availableHandlerTypes))
//    }
    throw new NotFoundResponse("uri: " + uri);
  }

  private void processHead(Context ctx) {
    ctx.status(200);
  }

  private boolean hasGetHandler(String uri) {
    return routes.match(Routing.Type.GET, uri) != null;
  }

  private Routing.Type method(HttpServletRequest req) {
    return methodMap.get(req.getMethod());
  }
}