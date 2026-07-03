package com.zerotrust.repository;

import com.zerotrust.entity.UserEntity;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<@NonNull UserEntity, @NonNull Long> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);
}
