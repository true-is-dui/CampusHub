package com.campushub.service.impl;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.ErrorReason;
import com.campushub.config.FileStorageProperties;
import com.campushub.entity.StoredFile;
import com.campushub.entity.enums.FileBusinessType;
import com.campushub.entity.enums.FileUsage;
import com.campushub.mapper.StoredFileMapper;
import com.campushub.service.FileStorageService;
import com.campushub.service.dto.StoredFileContent;
import com.campushub.util.ImageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * 本地文件存储实现。
 *
 * <p>实现顺序为：图片校验（{@link ImageValidator}）-> 保存磁盘文件 -> 写入 stored_files
 * 元数据。如果元数据插入失败，会尝试删除刚保存的磁盘文件，降低孤立文件风险。
 *
 * <p>本类只负责存储编排与路径/哈希等技术细节；「是否合法图片」的判断委托给
 * {@link ImageValidator}，保持职责单一。
 */
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final StoredFileMapper storedFileMapper;
    private final FileStorageProperties properties;
    private final ImageValidator imageValidator;

    @Override
    public Long uploadImage(MultipartFile file, Long uploaderId, FileUsage fileUsage,
                            FileBusinessType businessType, Long businessId) {
        ImageValidator.ImageInfo image = imageValidator.validate(file);
        String relativePath = buildRelativePath(fileUsage, image.extension());
        Path target = resolveStoragePath(relativePath);

        byte[] bytes = readBytes(file);
        writeFile(target, bytes);

        StoredFile storedFile = new StoredFile();
        storedFile.setUploaderId(uploaderId);
        storedFile.setFileUsage(fileUsage);
        storedFile.setOriginalFilename(image.originalFilename());
        storedFile.setStoragePath(relativePath);
        storedFile.setMimeType(file.getContentType());
        storedFile.setFileSize(file.getSize());
        storedFile.setSha256(sha256(bytes));
        storedFile.setBusinessType(businessType == null ? null : businessType.name());
        storedFile.setBusinessId(businessId);

        try {
            int inserted = storedFileMapper.insert(storedFile);
            if (inserted != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件元数据保存失败");
            }
            return storedFile.getId();
        } catch (RuntimeException ex) {
            deleteQuietly(target);
            throw ex;
        }
    }

    @Override
    public StoredFileContent loadFile(Long fileId) {
        StoredFile storedFile = storedFileMapper.selectById(fileId);
        if (storedFile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorReason.RESOURCE_NOT_FOUND,
                    "文件不存在");
        }
        Path path = resolveStoragePath(storedFile.getStoragePath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorReason.RESOURCE_NOT_FOUND,
                    "文件不存在");
        }
        return new StoredFileContent(new FileSystemResource(path), storedFile.getMimeType(),
                storedFile.getFileSize());
    }

    @Override
    public void updateBusinessTrace(Long fileId, FileBusinessType businessType, Long businessId) {
        StoredFile update = new StoredFile();
        update.setId(fileId);
        update.setBusinessType(businessType == null ? null : businessType.name());
        update.setBusinessId(businessId);
        storedFileMapper.updateById(update);
    }

    private String buildRelativePath(FileUsage fileUsage, String extension) {
        LocalDate now = LocalDate.now();
        return directoryOf(fileUsage) + "/" + now.getYear() + "/" + "%02d".formatted(now.getMonthValue())
                + "/" + UUID.randomUUID() + "." + extension;
    }

    private String directoryOf(FileUsage fileUsage) {
        return switch (fileUsage) {
            case AVATAR -> "avatar";
            case VERIFICATION_MATERIAL -> "verification";
            case PICKUP_CREDENTIAL -> "pickup-credential";
            case COMPLETION_PROOF -> "completion-proof";
        };
    }

    private Path resolveStoragePath(String relativePath) {
        Path root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "文件路径不合法",
                    Map.of("file", "文件路径不合法"));
        }
        return target;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取上传文件失败");
        }
    }

    private void writeFile(Path target, byte[] bytes) {
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存上传文件失败");
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "计算文件哈希失败");
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 元数据落库失败后的清理兜底，删除失败不覆盖原异常。
        }
    }
}
