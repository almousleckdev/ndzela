package com.ndzela.admin.charging.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdTagRepository extends JpaRepository<IdTag, UUID> {
    Optional<IdTag> findByCustomerId(String customerId);

    Optional<IdTag> findByIdTag(String idTag);

    List<IdTag> findAllByIdTagInAndStatus(Collection<String> idTags, IdTagStatus status);

    List<IdTag> findAllByStatusOrderByUpdatedAtDesc(IdTagStatus status);

    List<IdTag> findAllByDriverId(UUID driverId);

    List<IdTag> findAllByStatusAndZellaNotifiedAndHistoryRead(IdTagStatus status, boolean zellaNotified, boolean historyRead);

    boolean existsByDriverIdAndStatusAndIdNot(UUID driverId, IdTagStatus status, UUID id);

    /** The drivers that must not be looked up again: refused by staff. Linked drivers are looked up too, because they may sign in to Zella with Google or Apple and get a second account. */
    @org.springframework.data.jpa.repository.Query("select distinct t.driverId from IdTag t where t.status = com.ndzela.admin.charging.domain.IdTagStatus.UNLINKED")
    List<UUID> decidedDriverIds();

    /** Links Zella has not been told about, and unlinks it still holds. */
    @org.springframework.data.jpa.repository.Query("select t from IdTag t where (t.status = com.ndzela.admin.charging.domain.IdTagStatus.LINKED and t.zellaNotified = false) or (t.status <> com.ndzela.admin.charging.domain.IdTagStatus.LINKED and t.zellaNotified = true)")
    List<IdTag> pendingZellaSync();

    /** Every idTag a linked customer charged with, from the payment feed: Zella may have replaced the one on the link since. */
    @org.springframework.data.jpa.repository.Query("select distinct p.idTag as idTag, t.driverId as driverId from SessionPayment p, IdTag t where t.customerId = p.customerId and t.status = com.ndzela.admin.charging.domain.IdTagStatus.LINKED and p.idTag is not null")
    List<IdTagOwner> ownersFromPayments();

    interface IdTagOwner {
        String getIdTag();

        UUID getDriverId();
    }
}
