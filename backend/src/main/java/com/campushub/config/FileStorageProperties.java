package com.campushub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置，绑定 application.yaml 中的 file.storage.*。
 *
 * <p>root 可以是相对路径或绝对路径；相对路径按后端运行目录解析。
 * 仓库默认使用 uploads，并已加入 .gitignore，避免提交本地上传文件。
 *
 * <p>用 {@code @Getter @Setter} 而非 {@code @Data}：配置绑定只需 getter/setter，
 * 不需要 {@code @ToString}/{@code @EqualsAndHashCode}。注册见 {@link PropertiesConfig}。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /** 文件存储根目录，配置缺省值为 uploads。 */
    private String root = "uploads";
}
