package com.dashflow.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ServiceTypeServiceTest {

  @Mock private ServiceTypeRepository serviceTypeRepository;

  @InjectMocks private ServiceTypeService serviceTypeService;

  @Test
  void findAll_containsAuth() {
    ReflectionTestUtils.setField(serviceTypeService, "objectMapper", new ObjectMapper());

    ServiceType auth = new ServiceType();
    auth.setId(ServiceTypeService.AUTH_TYPE_ID);
    auth.setType(ServiceKind.auth);
    auth.setDisplayName("Authentication");

    ServiceDefault stub = new ServiceDefault();
    stub.setId("sd-auth-stub");
    stub.setVendor(ServiceTypeService.STUB_AUTH_VENDOR);
    stub.setDefaultConfig("{\"issuer\":\"https://auth.example.local/stub\"}");
    stub.setServiceType(auth);
    auth.setDefaults(List.of(stub));

    when(serviceTypeRepository.findAllWithDefaults()).thenReturn(List.of(auth));

    List<ServiceTypeResponse> catalog = serviceTypeService.listCatalog();

    assertThat(catalog).hasSize(1);
    assertThat(catalog.getFirst().type()).isEqualTo(ServiceKind.auth);
    assertThat(catalog.getFirst().id()).isEqualTo("st-auth");
    assertThat(catalog.getFirst().defaults())
        .anySatisfy(d -> assertThat(d.vendor()).isEqualTo("StubAuth"));
    assertThat(serviceTypeService.hasAuthStubVendor()).isTrue();
  }
}
