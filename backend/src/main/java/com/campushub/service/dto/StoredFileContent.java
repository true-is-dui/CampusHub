package com.campushub.service.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;

/**
 * 已保存文件的读取结果。
 *
 * <p>用于业务 Controller 返回图片二进制内容；文件模块只提供内容和 MIME 信息，
 * 是否允许当前用户读取仍由业务模块在调用前判断。图片均为内联展示，无需下载文件名，
 * 故不携带 originalFilename。
 *
 * <p>放在 {@code service.dto} 子包：它是服务层对外输出的数据载体（非框架对象、
 * 也非对外 JSON 契约），与 {@code service} 根包下的服务接口区分开。
 */
@Getter
@RequiredArgsConstructor
public class StoredFileContent {

    private final Resource resource;
    private final String mimeType;
    private final long fileSize;
}
