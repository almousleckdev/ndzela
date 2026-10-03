package com.ndzela.admin.recruitment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {
    @Query("""
            select m from ContactMessage m
            where m.status in :statuses
              and (lower(m.name) like :q or lower(m.subject) like :q or lower(coalesce(m.email, '')) like :q or lower(coalesce(m.phone, '')) like :q)
            order by m.createdAt desc""")
    Page<ContactMessage> search(@Param("statuses") Collection<MessageStatus> statuses, @Param("q") String q, Pageable pageable);

    long countByStatus(MessageStatus status);
}
