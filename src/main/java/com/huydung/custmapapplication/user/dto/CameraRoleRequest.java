package com.huydung.custmapapplication.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Yêu cầu cấp (enabled = true) hoặc thu hồi (false) quyền Camera của một tài khoản. */
@Getter
@Setter
@NoArgsConstructor
public class CameraRoleRequest {
    private Boolean enabled;
}
