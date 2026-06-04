package com.campushub.service;

import com.campushub.entity.enums.FileUsage;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface FileStorageService {
    Long uploadImage(MultipartFile file, Long uploaderId, FileUsage fileUsage);
    Map<String, Object> loadFile(Long fileId);
}
