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

    /** The drivers that must not be looked up again: refused by staff, or linked with their idTag known (a linked driver without one waits for the first charge). */
    @org.springframework.data.jpa.repository.Query("select distinct t.driverId from IdTag t where t.status = com.ndzela.admin.charging.domain.IdTagStatus.UNLINKED or (t.status = com.ndzela.admin.charging.domain.IdTagStatus.LINKED and t.idTag is not null)")
    List<UUID> decidedDriverIds();

    /** Links Zella has not been told about, and unlinks it still holds. */
    @org.springframework.data.jpa.repository.Query("select t from IdTag t where (t.status = com.ndzela.admin.charging.domain.IdTagStatus.LINKED and t.zellaNotified = false) or (t.status <> com.ndzela.admin.charging.domain.IdTagStatus.LINKED and t.zellaNotified = true)")
    List<IdTag> pendingZellaSync();
}
