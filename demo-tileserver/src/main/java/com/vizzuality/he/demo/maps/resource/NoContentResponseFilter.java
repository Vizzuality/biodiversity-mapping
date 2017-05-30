package com.vizzuality.he.demo.maps.resource;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import com.sun.tools.internal.ws.wsdl.document.http.HTTPConstants;

/**
 * Sets a 204 for any response that is a NULL object but indicating a 200.
 */
public class NoContentResponseFilter implements ContainerResponseFilter {

  @Override
  public void filter(
    ContainerRequestContext requestContext, ContainerResponseContext responseContext
  ) throws IOException {
    if (responseContext.getStatus() == 200 &&
        responseContext.getEntity() != null &&
        responseContext.getEntity() instanceof byte[] &&
        ((byte[])responseContext.getEntity()).length == 0) {
      responseContext.setStatus(204);
    }
  }
}
