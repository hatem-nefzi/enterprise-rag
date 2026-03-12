package com.rag.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import com.rag.backend.config.StorageProperties;
import com.rag.backend.dto.document.DocumentUploadResponse;
import com.rag.backend.exception.DocumentNotFoundException;
import com.rag.backend.exception.DocumentUploadException;
import com.rag.backend.exception.FileStorageException;
import com.rag.backend.exception.FileValidationException;
import com.rag.backend.exception.MessagePublishingException;
import com.rag.backend.model.Document;
import com.rag.backend.model.User;
import com.rag.backend.model.enums.DocumentStatus;
import com.rag.backend.model.message.DocumentProcessingMessage;
import com.rag.backend.repository.DocumentRepository;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private FileValidationService fileValidationService;
    @Mock private DocumentPublisher documentPublisher;
    @Mock private StorageProperties storageProperties;
    @Mock private MultipartFile file;

    @InjectMocks
    private DocumentService documentService;

    private User testUser;
    private Document savedDocument;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        savedDocument = Document.builder()
            .id(42L)
            .filename("report.pdf")
            .fileSize(12545L)
            .storagePath("/tmp/rag-uploads/uuid_report.pdf")
            .status(DocumentStatus.PENDING)
            .uploadedAt(LocalDateTime.now())
            .build();

        // default happy path
        when(storageProperties.getUploadDir()).thenReturn("/tmp/rag-uploads");
        when(file.getOriginalFilename()).thenReturn("report.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(fileValidationService.sanitizeFilename(any())).thenReturn("report.pdf");
        when(fileValidationService.getDetectedMimeType(any())).thenReturn("application/pdf");
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
    }

    // -------------------------------------------------------------------------
    // uploadDocument
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnSuccessResponseOnValidUpload() throws Exception {
        mockFileStorage();

        DocumentUploadResponse response = documentService.uploadDocument(file, testUser);

        assertNotNull(response);
        assertEquals(42L, response.getDocumentId());
        assertEquals("report.pdf", response.getFilename());
        assertEquals(DocumentStatus.PROCESSING, response.getStatus());
    }

    @Test
    void shouldCallValidationBeforeAnythingElse() throws Exception {
        // if validation throws, nothing else should be called
        doThrow(new FileValidationException("Invalid file"))
            .when(fileValidationService).validateFile(any());

        assertThrows(FileValidationException.class,
            () -> documentService.uploadDocument(file, testUser));

        // file should never be stored if validation fails
        verify(documentRepository, never()).save(any());
        verify(documentPublisher, never()).publishForProcessing(any());
    }

    @Test
    void shouldSaveDocumentWithPendingStatusFirst() throws Exception {
        mockFileStorage();

        documentService.uploadDocument(file, testUser);

        // first save should be PENDING
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, atLeastOnce()).save(captor.capture());

        Document firstSave = captor.getAllValues().get(0);
        assertEquals(DocumentStatus.PENDING, firstSave.getStatus());
    }

    @Test
    void shouldUpdateStatusToProcessingAfterSuccessfulPublish() throws Exception {
        mockFileStorage();

        documentService.uploadDocument(file, testUser);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, atLeastOnce()).save(captor.capture());

        // second save should be PROCESSING
        Document secondSave = captor.getAllValues().get(1);
        assertEquals(DocumentStatus.PROCESSING, secondSave.getStatus());
    }

    @Test
    void shouldMarkDocumentAsFailedWhenPublishingFails() throws Exception {
        mockFileStorage();
        doThrow(new MessagePublishingException("RabbitMQ down", new RuntimeException()))
            .when(documentPublisher).publishForProcessing(any());

        assertThrows(DocumentUploadException.class,
            () -> documentService.uploadDocument(file, testUser));

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, atLeastOnce()).save(captor.capture());

        Document failedSave = captor.getAllValues().get(1);
        assertEquals(DocumentStatus.FAILED, failedSave.getStatus());
    }

    @Test
    void shouldThrowFileStorageExceptionWhenFileCannotBeSaved() throws Exception {
        when(file.getInputStream()).thenThrow(new IOException("Disk full"));

        assertThrows(FileStorageException.class,
            () -> documentService.uploadDocument(file, testUser));

        // if file storage fails, DB should never be touched
        verify(documentRepository, never()).save(any());
    }

    @Test
    void shouldUseTikaDetectedMimeTypeNotBrowserProvided() throws Exception {
        mockFileStorage();
        // browser says PDF but Tika detects it differently
        when(file.getContentType()).thenReturn("application/pdf");
        when(fileValidationService.getDetectedMimeType(any())).thenReturn("text/plain");

        documentService.uploadDocument(file, testUser);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, atLeastOnce()).save(captor.capture());

        // should use Tika's detected type, not browser's
        assertEquals("text/plain", captor.getAllValues().get(0).getMimeType());
    }

    @Test
    void shouldPublishMessageWithCorrectDocumentId() throws Exception {
        mockFileStorage();

        documentService.uploadDocument(file, testUser);

        ArgumentCaptor<DocumentProcessingMessage> captor =
            ArgumentCaptor.forClass(DocumentProcessingMessage.class);
        verify(documentPublisher).publishForProcessing(captor.capture());

        assertEquals(42L, captor.getValue().getDocumentId());
        assertEquals("report.pdf", captor.getValue().getFilename());
        assertEquals(1L, captor.getValue().getUploadedBy());
        assertEquals(0, captor.getValue().getRetryCount());
    }

    // -------------------------------------------------------------------------
    // getDocument
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnDocumentWhenFound() {
        when(documentRepository.findById(42L)).thenReturn(Optional.of(savedDocument));

        Document result = documentService.getDocument(42L);

        assertEquals(42L, result.getId());
    }

    @Test
    void shouldThrowDocumentNotFoundExceptionWhenMissing() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class,
            () -> documentService.getDocument(99L));
    }

    // -------------------------------------------------------------------------
    // deleteDocument
    // -------------------------------------------------------------------------

    @Test
    void shouldDeleteDocumentAndFile() throws Exception {
        when(documentRepository.findById(42L)).thenReturn(Optional.of(savedDocument));

        // create a real temp file so deleteIfExists works
        Path tempFile = Files.createTempFile("test", ".pdf");
        savedDocument.setStoragePath(tempFile.toString());

        documentService.deleteDocument(42L);

        verify(documentRepository).delete(savedDocument);
        assertFalse(Files.exists(tempFile));
    }

    @Test
    void shouldStillDeleteDBRecordEvenIfFileDeletionFails() {
        savedDocument.setStoragePath("/nonexistent/path/file.pdf");
        when(documentRepository.findById(42L)).thenReturn(Optional.of(savedDocument));

        // should not throw — file deletion failure is logged, not rethrown
        assertDoesNotThrow(() -> documentService.deleteDocument(42L));
        verify(documentRepository).delete(savedDocument);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private void mockFileStorage() throws Exception {
        when(file.getInputStream())
            .thenReturn(new ByteArrayInputStream("pdf content".getBytes()));
    }
}