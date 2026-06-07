package com.campushub.util;

import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;

/**
 * 图片上传校验：非空、文件名合法、MIME 与扩展名同为 JPG/PNG。
 *
 * <p>从文件存储实现中抽出，作为「合法图片」这一不变量的守卫，供头像、认证材料、
 * 取件凭证、完成凭证等多处复用。单张大小上限交由 {@code spring.servlet.multipart}
 * 在框架层拦截（超限抛 {@code MaxUploadSizeExceededException}，由全局异常处理器翻译），
 * 本类不再重复做大小校验。
 *
 * <p>与 {@link StudentIdMasker} 同置于 {@code util} 包：均为可复用的无状态技术 helper。
 * 本类标 {@code @Component} 以便构造注入，行为仍是纯函数。
 */
@Component
public class ImageValidator {

    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_PNG = "image/png";

    /**
     * 校验并解析图片基本信息。
     *
     * @return 规范化文件名与小写扩展名
     * @throws BusinessException 校验不通过（40001，errors 用 {@code file} 作 key）
     */
    public ImageInfo validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("图片文件不能为空");
        }
        String filename = cleanOriginalFilename(file);
        String extension = extensionOf(filename);
        String contentType = file.getContentType();
        boolean jpeg = MIME_JPEG.equals(contentType) && ("jpg".equals(extension) || "jpeg".equals(extension));
        boolean png = MIME_PNG.equals(contentType) && "png".equals(extension);
        if (!jpeg && !png) {
            throw invalid("仅支持 JPG 或 PNG 图片");
        }
        return new ImageInfo(filename, extension);
    }

    private String cleanOriginalFilename(MultipartFile file) {
        String filename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!StringUtils.hasText(filename) || filename.contains("..")) {
            throw invalid("文件名不合法");
        }
        return filename;
    }

    private String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            throw invalid("文件扩展名不合法");
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_PARAM, message, Map.of("file", message));
    }

    /** 图片校验结果：规范化文件名与小写扩展名。 */
    public record ImageInfo(String originalFilename, String extension) {
    }
}
