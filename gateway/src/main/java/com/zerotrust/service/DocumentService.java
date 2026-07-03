package com.zerotrust.service;

import com.zerotrust.dto.DocumentRequestDTO;
import com.zerotrust.dto.DocumentResponseDTO;
import com.zerotrust.entity.DocumentEntity;
import com.zerotrust.entity.UserEntity;
import com.zerotrust.exception.EntityNotFoundException;
import com.zerotrust.repository.DocumentRepository;
import com.zerotrust.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public List<DocumentResponseDTO> getAll() {
        var user = getCurrentUser();

        return documentRepository.findAllByUser(user).stream()
                .map(document -> modelMapper.map(document, DocumentResponseDTO.class))
                .toList();
    }

    public DocumentResponseDTO getById(Long id) {
        var user = getCurrentUser();
        var document = documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Document", id));

        return modelMapper.map(document, DocumentResponseDTO.class);
    }

    public DocumentResponseDTO add(DocumentRequestDTO documentRequest) {
        var user = getCurrentUser();
        var document = DocumentEntity.builder()
                .user(user)
                .content(documentRequest.getContent())
                .lastModified(Date.from(Instant.now()))
                .build();

        documentRepository.save(document);
        return modelMapper.map(document, DocumentResponseDTO.class);
    }

    public DocumentResponseDTO update(Long id, DocumentRequestDTO documentRequest) {
        var user = getCurrentUser();
        var document = documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Document", id));

        document.setContent(documentRequest.getContent());
        document.setLastModified(Date.from(Instant.now()));

        documentRepository.save(document);
        return modelMapper.map(document, DocumentResponseDTO.class);
    }

    public void delete(Long id) {
        var user = getCurrentUser();
        var document = documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Document", id));

        documentRepository.delete(document);
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("No authenticated user in security context");
        }

        var username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User", username));
    }
}
