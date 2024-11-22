package io.avaje.jex;

import java.io.IOException;

import com.sun.net.httpserver.Filter.Chain;

/**
 * A filter used to pre- and post-process incoming requests. Pre-processing occurs before the
 * application's exchange handler is invoked, and post-processing occurs after the exchange handler
 * returns. Filters are organized in chains, and are associated with {@link Context} instances.
 *
 * <p>Each {@code HttpFilter} in the chain, invokes the next filter within its own {@link
 * #filter(Context, Chain)} implementation. The final {@code HttpFilter} in the chain invokes the
 * applications exchange handler.
 */
@FunctionalInterface
public interface HttpFilter {

  /**
   * Asks this filter to pre/post-process the given request. The filter can:
   *
   * <ul>
   *   <li>Examine or modify the request headers.
   *   <li>Set attribute objects in the context, which other filters or the handler can access.
   *   <li>Decide to either:
   *       <ol>
   *         <li>Invoke the next filter in the chain, by calling {@link FilterChain#proceed}.
   *         <li>Terminate the chain of invocation, by <b>not</b> calling {@link
   *             FilterChain#filter}.
   *       </ol>
   *   <li>If option 1. above is taken, then when filter() returns all subsequent filters in the
   *       Chain have been called, and the response headers can be examined or modified.
   *   <li>If option 2. above is taken, then this Filter must use the Context to send back an
   *       appropriate response.
   * </ul>
   *
   * @param ctx the {@code Context} of the current request
   * @param chain the {@code FilterChain} which allows the next filter to be invoked
   */
  void filter(Context ctx, FilterChain chain) throws IOException;
}
