package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.entity.enums.FileUsage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stored_files")
public class StoredFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uploaderId;
    private FileUsage fileUsage;
    private String originalName;
    private String storagePath;
    private String mimeType;
    private Long size;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
