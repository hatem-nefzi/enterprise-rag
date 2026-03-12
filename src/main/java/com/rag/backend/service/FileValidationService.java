package com.rag.backend.service;


import com.rag.backend.exception.FileValidationException;
import com.rag.backend.config.FileUploadProperties;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

import java.io.IOException;
import java.nio.file.Paths;


@Service
@Slf4j
@RequiredArgsConstructor
public class FileValidationService {
    private final FileUploadProperties properties;
    private final Tika tika ; // now injected , not instanciated here 

    public void validateFile( MultipartFile file) {
        validateNoEmpty(file);
        validateSize(file);
        validateExtension(file);
        validateMimeType(file);
        
        log.info("File validation passed : {} ({} bytes)", file.getOriginalFilename(), file.getSize());
    }

    private void validateNoEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is empty");
        }
    
    }

    private void validateSize(MultipartFile file ) {
        long maxBytes = properties.getMaxFileSizeMb()*1024*1024;
        if (file.getSize() > maxBytes) {
            throw new FileValidationException("File size exceeds the maximum allowed of " + properties.getMaxFileSizeMb() + " MB");
        }
    }

    private void validateExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new FileValidationException("File must have a valid name");
        }

        String extension = getExtension(filename);
        if (!properties.getAllowedExtensions().contains(extension)) {
            throw new FileValidationException("Invalid file extention. Allowed: " + properties.getAllowedExtensions());
        }

        
    }

    private void validateMimeType(MultipartFile file) {
        try {
            String detectedType = tika.detect(file.getInputStream());
            if (!properties.getAllowedMimeTypes().contains(detectedType)) {
                throw new FileValidationException("Invalid MIME type detected : " + detectedType);
            }
        } catch (IOException e) {
            throw new FileValidationException("Could not read file for MIME validation");
        }
    }


    private String getExtension(String filename) {
        String cleanName = Paths.get(filename).getFileName().toString();
        int lastDot = cleanName.lastIndexOf('.');
        if (lastDot == -1) {
            throw new FileValidationException("File has no extension");

        }
        return cleanName.substring(lastDot+1).toLowerCase();

    }

    public String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed_file";
        }

        return Paths.get(filename).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }


    public String getDetectedMimeType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream());
        } catch (IOException e) {
            throw new FileValidationException("Could not detect MIME type");
        }
    }


    
}
