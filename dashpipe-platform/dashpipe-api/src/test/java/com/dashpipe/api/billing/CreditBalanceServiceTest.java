package com.dashpipe.api.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dashpipe.api.tenant.Tenant;
import com.dashpipe.api.tenant.TenantRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** W5-US04: credit deduct on aggregate path. */
@ExtendWith(MockitoExtension.class)
class CreditBalanceServiceTest {

  @Mock private TenantRepository tenantRepository;

  private CreditBalanceService service;

  @BeforeEach
  void setUp() {
    service = new CreditBalanceService(tenantRepository);
  }

  @Test
  void deduct_subtractsFromBalance() {
    Tenant tenant = new Tenant();
    tenant.setId("T001");
    tenant.setCreditBalance(new BigDecimal("10.0000"));
    when(tenantRepository.findById("T001")).thenReturn(Optional.of(tenant));
    when(tenantRepository.save(tenant)).thenReturn(tenant);

    BigDecimal after = service.deduct("T001", new BigDecimal("2.5000"));

    assertThat(after).isEqualByComparingTo("7.5000");
    ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
    verify(tenantRepository).save(captor.capture());
    assertThat(captor.getValue().getCreditBalance()).isEqualByComparingTo("7.5000");
  }

  @Test
  void deduct_zeroAmount_isNoOp() {
    Tenant tenant = new Tenant();
    tenant.setId("T001");
    tenant.setCreditBalance(new BigDecimal("10.0000"));
    when(tenantRepository.findById("T001")).thenReturn(Optional.of(tenant));

    assertThat(service.deduct("T001", BigDecimal.ZERO)).isEqualByComparingTo("10.0000");
  }
}
