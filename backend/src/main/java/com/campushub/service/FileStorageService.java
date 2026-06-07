package com.campushub.service;

import com.campushub.entity.enums.FileBusinessType;
import com.campushub.entity.enums.FileUsage;
import com.campushub.service.dto.StoredFileContent;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务。
 *
 * <p>仅负责图片格式/大小校验、本地保存、元数据记录和按 fileId 读取文件。
 * 不负责判断头像、认证材料、取件凭证或完成凭证的业务访问权限。
 */
public interface FileStorageService {

    /**
     * 上传图片并写入 stored_files 元数据。
     *
     * @param file         multipart 图片文件
     * @param uploaderId   上传者用户 ID
     * @param fileUsage    文件用途
     * @param businessType 上传溯源业务类型，可为空
     * @param businessId   上传溯源业务 ID，可为空
     * @return 已落库文件的 ID（业务方只需以 fileId 关联，不暴露存储路径等内部元数据）
     */
    Long uploadImage(MultipartFile file, Long uploaderId, FileUsage fileUsage,
                     FileBusinessType businessType, Long businessId);

    /**
     * 按 fileId 读取文件内容和 MIME 信息。
     *
     * <p><b>受信调用</b>：本方法不做任何业务访问权限判断，对任意存在的 fileId 都直接返回内容。
     * 调用方<b>必须</b>在调用前完成权限校验（如 {@code loadAvatar} 经用户存在性、
     * {@code loadReviewImage} 经管理员身份）。禁止把前端传入的 fileId 不加校验直接传入，
     * 以免越权读取他人认证材料、取件凭证等受限文件。
     */
    StoredFileContent loadFile(Long fileId);

    /** 回填上传溯源业务对象 ID，不改变文件真实业务归属。 */
    void updateBusinessTrace(Long fileId, FileBusinessType businessType, Long businessId);
}
