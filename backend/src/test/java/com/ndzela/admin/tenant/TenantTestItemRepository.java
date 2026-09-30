package com.ndzela.admin.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantTestItemRepository extends JpaRepository<TenantTestItem, UUID> {
    List<TenantTestItem> findByName(String name);

    boolean existsByName(String name);
}
