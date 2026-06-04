package com.campushub.service.impl;

import com.campushub.common.BusinessException;
import com.campushub.entity.StoredFile;
import com.campushub.entity.enums.FileUsage;
import com.campushub.service.FileStorageService;
import com.campushub.mapper.StoredFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final StoredFileMapper storedFileMapper;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public Long uploadImage(MultipartFile file, Long uploaderId, FileUsage fileUsage) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40001, "文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new BusinessException(40002, "仅支持jpg/png格式图片");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(40003, "文件大小不能超过5MB");
        }

        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName, contentType);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        String usageDir = fileUsage.name().toLowerCase();
        Path targetDir = Paths.get(uploadDir, usageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", targetDir, e);
            throw new BusinessException(50001, "创建上传目录失败");
        }

        Path targetPath = targetDir.resolve(storedName);
        try {
            file.transferTo(targetPath);
        } catch (IOException e) {
            log.error("保存文件失败: {}", targetPath, e);
            throw new BusinessException(50002, "保存文件失败");
        }

        StoredFile storedFile = new StoredFile();
        storedFile.setUploaderId(uploaderId);
        storedFile.setFileUsage(fileUsage);
        storedFile.setOriginalName(originalName);
        storedFile.setStoragePath(targetPath.toString());
        storedFile.setMimeType(contentType);
        storedFile.setSize(file.getSize());
        storedFileMapper.insert(storedFile);

        return storedFile.getId();
    }

    public Map<String, Object> loadFile(Long fileId) {
        StoredFile storedFile = storedFileMapper.selectById(fileId);
        if (storedFile == null) {
            throw new BusinessException(40401, "文件不存在");
        }

        Path path = Paths.get(storedFile.getStoragePath());
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BusinessException(50003, "读取文件失败");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bytes", bytes);
        result.put("mimeType", storedFile.getMimeType());
        result.put("originalName", storedFile.getOriginalName());
        return result;
    }

    private String getExtension(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                return ext;
            }
        }
        if ("image/jpeg".equals(contentType)) {
            return "jpg";
        }
        return "png";
    }
}
