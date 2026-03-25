package com.group5.ems.service.external;

import com.group5.ems.dto.vietqr.VietQrBankDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class VietQrApiClient {

    private final RestClient restClient;


    public VietQrApiClient(
            RestClient.Builder restClientBuilder
            ) {
        this.restClient = restClientBuilder.baseUrl("https://api.vietqr.io").build();
    }

    @Cacheable("banks")
    public List<VietQrBankDTO> getSupportedBanks() {
        VietQrBanksRes response = restClient.get()
                .uri("/v2/banks")
                .retrieve()
                .body(VietQrBanksRes.class);
                
        if (response != null && response.data() != null) {
            return response.data();
        }
        return List.of();
    }

    // Helper record to parse the banks list response
    private record VietQrBanksRes(String code, String desc, List<VietQrBankDTO> data) {}
}
