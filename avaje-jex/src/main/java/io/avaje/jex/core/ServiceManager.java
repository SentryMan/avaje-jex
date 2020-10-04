package io.avaje.jex.core;

import io.avaje.jex.Context;
import io.avaje.jex.ErrorHandling;
import io.avaje.jex.spi.JsonService;
import io.avaje.jex.spi.SpiContext;

import java.util.Map;

public class ServiceManager {

  private final JsonService jsonService;

  private final ExceptionManager exceptionHandler;

  private final TemplateManager templateManager;

  public ServiceManager(JsonService jsonService, ErrorHandling errorHandling, TemplateManager templateManager) {
    this.jsonService = jsonService;
    this.exceptionHandler = new ExceptionManager(errorHandling);
    this.templateManager = templateManager;
  }

  public <T> T jsonRead(Class<T> clazz, SpiContext ctx) {
    return jsonService.jsonRead(clazz, ctx);
  }

  public void jsonWrite(Object bean, SpiContext ctx) {
    jsonService.jsonWrite(bean, ctx);
  }

  public void handleException(Context ctx, Exception e) {
    exceptionHandler.handle(ctx, e);
  }

  public void render(Context ctx, String name, Map<String, Object> model) {
    templateManager.render(ctx, name, model);
  }
}
