package com.dashflow.api.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantNoteServiceTest {

  @Mock private TenantNoteRepository tenantNoteRepository;

  @InjectMocks private TenantNoteService tenantNoteService;

  @AfterEach
  void clearContext() {
    TenantContext.clear();
  }

  @Test
  void create_requiresTenantContext() {
    assertThatThrownBy(
            () -> tenantNoteService.create(new CreateTenantNoteRequest("t", "b")))
        .isInstanceOf(TenantContextRequiredException.class);

    verify(tenantNoteRepository, never()).save(any());
  }

  @Test
  void requireTenantId_isBoundWhenContextSet() {
    TenantContext.setTenantId("T001");
    assertThat(TenantContext.getTenantId()).isEqualTo("T001");
  }
}
