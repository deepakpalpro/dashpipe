package com.dashflow.api.billing;

import com.dashflow.api.tenant.Tenant;
import com.dashflow.api.tenant.TenantNotFoundException;
import com.dashflow.api.tenant.TenantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adjusts {@code tenants.credit_balance} (architecture §6.2 prepaid credits). */
@Service
public class CreditBalanceService {

  private static final Logger log = LoggerFactory.getLogger(CreditBalanceService.class);

  private final TenantRepository tenantRepository;

  public CreditBalanceService(TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  /**
   * Subtract {@code amount} from the tenant credit balance. Zero/null amount is a no-op. Balance may
   * go negative so aggregate re-runs stay consistent; runs are blocked when balance ≤ 0.
   */
  @Transactional
  public BigDecimal deduct(String tenantId, BigDecimal amount) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("tenantId required");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
      return currentBalance(tenantId);
    }

    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
    BigDecimal before = tenant.getCreditBalance() == null ? BigDecimal.ZERO : tenant.getCreditBalance();
    BigDecimal after = before.subtract(amount).setScale(4, RoundingMode.HALF_UP);
    tenant.setCreditBalance(after);
    tenantRepository.save(tenant);
    log.info(
        "credit deduct tenantId={} amount={} before={} after={}",
        tenantId,
        amount,
        before,
        after);
    return after;
  }

  @Transactional(readOnly = true)
  public BigDecimal currentBalance(String tenantId) {
    return tenantRepository
        .findById(tenantId)
        .map(t -> t.getCreditBalance() == null ? BigDecimal.ZERO : t.getCreditBalance())
        .orElseThrow(() -> new TenantNotFoundException(tenantId));
  }

  @Transactional
  public void setBalance(String tenantId, BigDecimal balance) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
    tenant.setCreditBalance(
        balance == null ? BigDecimal.ZERO : balance.setScale(4, RoundingMode.HALF_UP));
    tenantRepository.save(tenant);
  }

  @Transactional
  public void setQuotaConfig(String tenantId, String quotaConfigJson) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
    tenant.setQuotaConfig(quotaConfigJson);
    tenantRepository.save(tenant);
  }
}
