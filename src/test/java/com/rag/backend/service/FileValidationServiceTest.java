package com.rag.backend.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import com.rag.backend.config.FileUploadProperties;
import com.rag.backend.exception.FileValidationException;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  
public class FileValidationServiceTest {

    @Mock
    private FileUploadProperties properties;

    @Mock
    private Tika tika;

    @InjectMocks
    private FileValidationService validationService;

    private MultipartFile mockFile;

    @BeforeEach
    void setUp() throws IOException {
        mockFile = mock(MultipartFile.class);

        // default happy path setup
        when(properties.getMaxFileSizeMb()).thenReturn(5L);
        when(properties.getAllowedExtensions()).thenReturn(List.of("pdf", "txt", "docx"));
        when(properties.getAllowedMimeTypes()).thenReturn(List.of("application/pdf", "text/plain"));

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(tika.detect(any(InputStream.class))).thenReturn("application/pdf");
    }

    // ---- validateFile ----

    @Test
    void shouldPassValidationForValidFile() {
        assertDoesNotThrow(() -> validationService.validateFile(mockFile));
    }

    @Test
    void shouldThrowWhenFileIsNull() {
        assertThrows(FileValidationException.class,
            () -> validationService.validateFile(null));
    }

    @Test
    void shouldThrowWhenFileIsEmpty() {
        when(mockFile.isEmpty()).thenReturn(true);
        assertThrows(FileValidationException.class,
            () -> validationService.validateFile(mockFile));
    }

    @Test
    void shouldThrowWhenFileSizeExceedsLimit() {
        when(mockFile.getSize()).thenReturn(10L * 1024 * 1024); // 10MB
        assertThrows(FileValidationException.class,
            () -> validationService.validateFile(mockFile));
    }

    @Test
    void shouldThrowWhenExtensionNotAllowed() {
        when(mockFile.getOriginalFilename()).thenReturn("malware.exe");
        assertThrows(FileValidationException.class,
            () -> validationService.validateFile(mockFile));
    }

    @Test
    void shouldThrowWhenFileHasNoExtension() {
        when(mockFile.getOriginalFilename()).thenReturn("noextension");
        assertThrows(FileValidationException.class,
            () -> validationService.validateFile(mockFile));
    }

    @Test
    void shouldThrowWhenMimeTypeNotAllowed() throws IOException {
        when(tika.detect(any(InputStream.class))).thenReturn("application/x-msdownload");
        assertThrows(FileValidationException.class,
            () -> validationService.validateFile(mockFile));
    }

    @Test
    void shouldThrowWhenInputStreamFails() throws IOException {
        when(mockFile.getInputStream()).thenThrow(new IOException("Stream error"));
        assertThrows(FileValidationException.class,
            () -> validationService.validateFile(mockFile));
    }

    // ---- Extension spoofing (key security test) ----

    @Test
    void shouldThrowWhenExtensionSpoofed() throws IOException {
        // file is named .pdf but Tika detects it as executable
        when(mockFile.getOriginalFilename()).thenReturn("fake.pdf");
        when(tika.detect(any(InputStream.class))).thenReturn("application/x-msdownload");

        assertThrows(FileValidationException.class,
            () -> validationService.validateFile(mockFile));
    }

    // ---- sanitizeFilename ----

    @Test
    void shouldSanitizeFilenameWithSpecialChars() {
        String result = validationService.sanitizeFilename("../../etc/passwd");
        assertFalse(result.contains("/"));
        assertFalse(result.contains(".."));
    }

    @Test
    void shouldReturnDefaultNameWhenFilenameIsNull() {
        assertEquals("unnamed_file", validationService.sanitizeFilename(null));
    }

    @Test
    void shouldPreserveValidFilename() {
        assertEquals("my-file_v2.pdf", validationService.sanitizeFilename("my-file_v2.pdf"));
    }
}