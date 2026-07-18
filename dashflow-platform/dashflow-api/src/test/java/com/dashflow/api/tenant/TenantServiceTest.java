package com.dashflow.api.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

  @Mock private TenantRepository tenantRepository;

  @InjectMocks private TenantService tenantService;

  @Test
  void create_rejectsBlankSlug() {
    CreateTenantRequest request = new CreateTenantRequest("Demo", "   ", TenantStatus.trial);

    assertThatThrownBy(() -> tenantService.create(request))
        .isInstanceOf(TenantValidationException.class)
        .hasMessageContaining("slug");

    verify(tenantRepository, never()).save(any());
  }

  @Test
  void create_persistsAndReturnsId() {
    when(tenantRepository.existsBySlug("demo")).thenReturn(false);
    when(tenantRepository.save(any(Tenant.class)))
        .thenAnswer(
            invocation -> {
              Tenant t = invocation.getArgument(0);
              return t;
            });

    TenantResponse response =
        tenantService.create(new CreateTenantRequest("Demo Tenant", "Demo", TenantStatus.active));

    ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
    verify(tenantRepository).save(captor.capture());
    Tenant saved = captor.getValue();

    assertThat(saved.getId()).isNotBlank();
    assertThat(saved.getSlug()).isEqualTo("demo");
    assertThat(saved.getName()).isEqualTo("Demo Tenant");
    assertThat(saved.getStatus()).isEqualTo(TenantStatus.active);
    assertThat(saved.getCreditBalance()).isEqualByComparingTo(TenantService.DEFAULT_CREDIT_BALANCE);
    assertThat(response.id()).isEqualTo(saved.getId());
    assertThat(response.slug()).isEqualTo("demo");
  }

  @Test
  void create_rejectsDuplicateSlug() {
    when(tenantRepository.existsBySlug("demo")).thenReturn(true);

    assertThatThrownBy(
            () ->
                tenantService.create(
                    new CreateTenantRequest("Other", "demo", TenantStatus.trial)))
        .isInstanceOf(TenantConflictException.class);

    verify(tenantRepository, never()).save(any());
  }
}
