package com.zerotrust.controller;

import com.zerotrust.dto.DocumentRequestDTO;
import com.zerotrust.dto.DocumentResponseDTO;
import com.zerotrust.service.DocumentService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<@NonNull List<DocumentResponseDTO>> getAll() {
        var documents = documentService.getAll();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<@NonNull DocumentResponseDTO> getById(@PathVariable Long id) {
        var document = documentService.getById(id);
        return ResponseEntity.ok(document);
    }

    @PostMapping
    public ResponseEntity<@NonNull DocumentResponseDTO> add(@RequestBody DocumentRequestDTO documentRequest) {
        var document = documentService.add(documentRequest);
        return ResponseEntity.ok(document);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<@NonNull DocumentResponseDTO> update(@PathVariable Long id, @RequestBody DocumentRequestDTO documentRequest) {
        var document = documentService.update(id, documentRequest);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
