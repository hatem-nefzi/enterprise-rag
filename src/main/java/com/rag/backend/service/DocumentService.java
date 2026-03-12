package com.rag.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.rag.backend.config.StorageProperties;
import com.rag.backend.exception.DocumentNotFoundException;
import com.rag.backend.exception.DocumentUploadException;
import com.rag.backend.exception.FileStorageException;
import com.rag.backend.exception.MessagePublishingException;
import com.rag.backend.model.Document;
import com.rag.backend.model.User;
import com.rag.backend.model.enums.DocumentStatus;
import com.rag.backend.model.message.DocumentProcessingMessage;
import com.rag.backend.repository.DocumentRepository;
import com.rag.backend.dto.document.DocumentUploadResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final FileValidationService fileValidationService;
    private final DocumentPublisher documentPublisher;
    private final StorageProperties storageProperties;

    public DocumentUploadResponse uploadDocument(MultipartFile file, User user){

        // validate and sanitize - throws FileValidationException if invalid 
        fileValidationService.validateFile(file);
        String sanitizedFilename = fileValidationService.sanitizeFilename(file.getOriginalFilename());

        // save file to disk - outside transation, pure I/O operation
        String storagePath = storeFile(file, sanitizedFilename);
        log.info("File stored at : {}", storagePath);

        // save DB record + publish message - wrapped in transation 
        return saveAndPublish(file, user, sanitizedFilename, storagePath);

        
    }

    @Transactional
    protected DocumentUploadResponse saveAndPublish(
        MultipartFile file,
        User user,
        String sanitizedFilename,
        String storagePath
    ) {
        //create DB record 
        Document document =  Document.builder()
            .user(user)
            .filename(sanitizedFilename)
            .storagePath(storagePath)
            .fileSize(file.getSize())
            .mimeType(detectMimeType(file))
            .status(DocumentStatus.PENDING)
            .uploadedAt(LocalDateTime.now())
            .build();
        
            document = documentRepository.save(document);
            log.info("Document record created : id= {} ", document.getId());

            // publish to RabbitMQ 
            try {
                DocumentProcessingMessage message = DocumentProcessingMessage.builder()
                    .documentId(document.getId())
                    .filename(sanitizedFilename)
                    .storagePath(storagePath)
                    .mimeType(document.getMimeType())
                    .fileSizeBytes(document.getFileSize())
                    .uploadedBy(user.getId())
                    .uploadedAt(document.getUploadedAt())
                    .retryCount(0)
                    .build();

                documentPublisher.publishForProcessing(message);

                // update status to PROCESSING Only after successful publish
                document.setStatus(DocumentStatus.PROCESSING);
                documentRepository.save(document);

            } catch (MessagePublishingException e) {
                // publishing failed - mark as FAILED so user/admin knows 
                // transaction will still commit this FAILED Status 
                document.setStatus(DocumentStatus.FAILED);
                documentRepository.save(document);
                log.error("Failed to publish document id = {} , marked as FAILED " , document.getId());
                throw new DocumentUploadException("Document saved but failed to queue for processing ", e );
                
            }

            log.info("Document uploaded and queued: id = {}, file={} ", document.getId(), sanitizedFilename);
            return DocumentUploadResponse.success(document.getId(), sanitizedFilename);
    }
    
    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    public List<Document> getUserDocuments(Long userId) {
        return documentRepository.findByUserId(userId);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = getDocument(documentId);

        // only delete file if document was actually stored
        if (document.getStoragePath() != null) {
            deleteFile(document.getStoragePath());
        }

        documentRepository.delete(document);
        log.info("Document deleted: id={}", documentId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String storeFile(MultipartFile file, String sanitizedFilename) {
        try {
            Path uploadPath = Paths.get(storageProperties.getUploadDir());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // UUID prefix prevents filename collisions
            String uniqueFilename = UUID.randomUUID() + "_" + sanitizedFilename;
            Path filePath = uploadPath.resolve(uniqueFilename);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString();

        } catch (IOException e) {
            throw new FileStorageException(
                "Failed to store file: " + sanitizedFilename, e
            );
        }
    }

    private void deleteFile(String storagePath) {
        try {
            Files.deleteIfExists(Paths.get(storagePath));
            log.info("File deleted: {}", storagePath);
        } catch (IOException e) {
            // log but don't throw — DB record deletion should still proceed
            log.warn("Could not delete file at {}: {}", storagePath, e.getMessage());
        }
    }

    private String detectMimeType(MultipartFile file) {
        try {
            // re-use Tika detection — don't trust file.getContentType() from browser
            return fileValidationService.getDetectedMimeType(file);
        } catch (Exception e) {
            log.warn("Could not detect MIME type, falling back to content type header");
            return file.getContentType();
        }
    }
}
    
    

