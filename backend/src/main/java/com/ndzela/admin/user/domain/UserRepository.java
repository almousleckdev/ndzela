package com.ndzela.admin.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRolesId(UUID roleId);

    @Query("select distinct u from User u join u.roles r join r.permissions p where p.name = :permission and u.status <> :excluded")
    List<User> findByPermissionExcludingStatus(@Param("permission") String permission, @Param("excluded") UserStatus excluded);

    default List<User> findActiveByPermission(String permission) {
        return findByPermissionExcludingStatus(permission, UserStatus.DISABLED);
    }
}
