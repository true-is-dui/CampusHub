package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private String studentId;
    private String realName;
    private String nickname;
    private Long avatarFileId;
    private Long verificationFileId;
    private String college;
    private String contact;
    private AuthStatus authStatus;
    private UserRole role;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
