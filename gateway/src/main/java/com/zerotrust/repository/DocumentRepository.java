package com.zerotrust.repository;

import com.zerotrust.entity.DocumentEntity;
import com.zerotrust.entity.UserEntity;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<@NonNull DocumentEntity, @NonNull Long> {
    List<DocumentEntity> findAllByUser(UserEntity user);
    Optional<DocumentEntity> findByIdAndUser(Long id, UserEntity user);
}
