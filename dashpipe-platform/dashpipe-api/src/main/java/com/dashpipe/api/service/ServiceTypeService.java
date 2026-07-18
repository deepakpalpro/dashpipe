package com.dashpipe.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceTypeService {

  public static final String AUTH_TYPE_ID = "st-auth";
  public static final String STUB_AUTH_VENDOR = "StubAuth";
  public static final String OAUTH_VENDOR = "OAuth";
  public static final String OIDC_VENDOR = "OIDC";
  public static final String KEYCLOAK_VENDOR = "Keycloak";
  public static final String AAD_VENDOR = "AAD";
  public static final String AWS_COGNITO_VENDOR = "AWSCognito";
  public static final String AZURE_MI_VENDOR = "AzureMI";
  public static final String CERT_BASED_VENDOR = "CertBased";
  public static final String JWT_VENDOR = "JWT";

  private final ServiceTypeRepository serviceTypeRepository;
  private final ObjectMapper objectMapper;

  public ServiceTypeService(ServiceTypeRepository serviceTypeRepository, ObjectMapper objectMapper) {
    this.serviceTypeRepository = serviceTypeRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<ServiceTypeResponse> listCatalog() {
    return serviceTypeRepository.findAllWithDefaults().stream()
        .map(st -> ServiceTypeResponse.from(st, objectMapper))
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean hasAuthStubVendor() {
    return serviceTypeRepository.findAllWithDefaults().stream()
        .filter(st -> st.getType() == ServiceKind.auth)
        .flatMap(st -> st.getDefaults().stream())
        .anyMatch(d -> STUB_AUTH_VENDOR.equals(d.getVendor()));
  }
}