package com.ndzela.admin.recruitment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface DriverApplicationRepository extends JpaRepository<DriverApplication, UUID> {
    @Query("""
            select a from DriverApplication a
            where a.status in :statuses
              and (lower(a.firstName) like :q or lower(a.lastName) like :q or lower(a.phone) like :q or lower(coalesce(a.email, '')) like :q)
            order by a.createdAt desc""")
    Page<DriverApplication> search(@Param("statuses") Collection<ApplicationStatus> statuses, @Param("q") String q, Pageable pageable);

    long countByStatus(ApplicationStatus status);
}
