package com.zerotrust.service;

import com.zerotrust.dto.DocumentRequestDTO;
import com.zerotrust.dto.DocumentResponseDTO;
import com.zerotrust.entity.DocumentEntity;
import com.zerotrust.entity.UserEntity;
import com.zerotrust.exception.EntityNotFoundException;
import com.zerotrust.repository.DocumentRepository;
import com.zerotrust.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceOwnershipTest {

    private static final String USERNAME = "mihai_user";

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private DocumentService documentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getByIdReturnsOnlyOwnedDocument() {
        var user = currentUser();
        var document = ownedDocument(user, "private");
        var dto = mappedDto(document);

        authenticateAsCurrentUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(documentRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(document));
        when(modelMapper.map(document, DocumentResponseDTO.class)).thenReturn(dto);

        var result = documentService.getById(1L);

        assertSame(dto, result);
        verify(documentRepository).findByIdAndUser(1L, user);
    }

    @Test
    void getByIdRejectsForeignDocument() {
        var user = currentUser();

        authenticateAsCurrentUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(documentRepository.findByIdAndUser(1L, user)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> documentService.getById(1L));
        verify(modelMapper, never()).map(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateRejectsForeignDocument() {
        var user = currentUser();
        var request = new DocumentRequestDTO();
        request.setContent("updated");

        authenticateAsCurrentUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(documentRepository.findByIdAndUser(1L, user)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> documentService.update(1L, request));
        verify(documentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteRejectsForeignDocument() {
        var user = currentUser();

        authenticateAsCurrentUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(documentRepository.findByIdAndUser(1L, user)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> documentService.delete(1L));
        verify(documentRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateChangesOwnedDocument() {
        var user = currentUser();
        var document = ownedDocument(user, "before");
        var request = new DocumentRequestDTO();
        request.setContent("after");
        var dto = mappedDto(document);

        authenticateAsCurrentUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(documentRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(document));
        when(documentRepository.save(document)).thenReturn(document);
        when(modelMapper.map(document, DocumentResponseDTO.class)).thenReturn(dto);

        var result = documentService.update(1L, request);

        assertSame(dto, result);
        assertEquals("after", document.getContent());
        verify(documentRepository).save(document);
    }

    @Test
    void deleteRemovesOwnedDocument() {
        var user = currentUser();
        var document = ownedDocument(user, "private");

        authenticateAsCurrentUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(documentRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(document));

        documentService.delete(1L);

        verify(documentRepository).delete(document);
    }

    private void authenticateAsCurrentUser() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(USERNAME, "password", List.of()));
        SecurityContextHolder.setContext(context);
    }

    private UserEntity currentUser() {
        var user = new UserEntity();
        user.setId(1L);
        user.setUsername(USERNAME);
        user.setPasswordHash("password-hash");
        return user;
    }

    private DocumentEntity ownedDocument(UserEntity user, String content) {
        var document = new DocumentEntity();
        document.setId(1L);
        document.setUser(user);
        document.setContent(content);
        document.setLastModified(new Date());
        return document;
    }

    private DocumentResponseDTO mappedDto(DocumentEntity document) {
        var dto = new DocumentResponseDTO();
        dto.setId(document.getId());
        dto.setContent(document.getContent());
        dto.setLastModified(document.getLastModified());
        return dto;
    }
}
