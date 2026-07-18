package com.dashpipe.api.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * W1-US01 stub auth: reads {@code X-Tenant-Id} into {@link TenantContext}.
 *
 * <p>Replace with JWT claim extraction when IdP is wired. Intended for {@code local}/{@code test}
 * development only until security is hardened.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TenantContextFilter extends OncePerRequestFilter {

  public static final String TENANT_ID_HEADER = "X-Tenant-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String tenantId = request.getHeader(TENANT_ID_HEADER);
    if (tenantId != null && !tenantId.isBlank()) {
      TenantContext.setTenantId(tenantId.trim());
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }
}
