package com.zerotrust.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "document")
public class DocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="app_user_id", nullable=false)
    private UserEntity user;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Date lastModified;
}
