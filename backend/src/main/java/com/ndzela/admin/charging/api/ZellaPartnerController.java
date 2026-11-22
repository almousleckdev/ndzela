package com.ndzela.admin.charging.api;

import com.ndzela.admin.charging.application.ZellaDriverProfile;
import com.ndzela.admin.charging.application.ZellaDriverProfiles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// The Zella app reads who a linked customer is for NDZela (server to server, X-Api-Key). The route is public for the JWT filters: the key is checked here, and the answer is the one country the key belongs to.
@RestController
@RequiredArgsConstructor
public class ZellaPartnerController {
    private final ZellaPartnerAccess access;
    private final ZellaDriverProfiles profiles;

    @GetMapping("/api/v1/partners/zella/customers/{customerId}/driver")
    public ResponseEntity<ZellaDriverProfile> driver(@PathVariable String customerId, @RequestHeader(value = "X-Api-Key", required = false) String key) {
        return access.call("driver", key, () -> profiles.ofCustomer(customerId))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
