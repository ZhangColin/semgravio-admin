package com.semgravio.admin.application.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.semgravio.admin.application.dto.response.AdminUserResponse;
import com.semgravio.admin.domain.aggregate.AdminRole;
import com.semgravio.admin.domain.aggregate.AdminUser;
import com.cartisan.web.mapper.DomainMapper;

/**
 * 管理员 Mapper。
 *
 * @since 0.1.0
 */
@Mapper(componentModel = "spring")
public interface AdminUserMapper extends DomainMapper<AdminUser, AdminUserResponse> {

    @Mapping(target = "statusName", source = "status.name")
    @Mapping(target = "genderName", source = "gender.name")
    @Mapping(target = "roles", ignore = true)
    @Override
    AdminUserResponse convert(AdminUser adminUser);

    /**
     * 详情转换：携带已分配角色摘要（角色由应用层批量查询传入，无 N+1）。
     */
    @Mapping(target = "statusName", source = "adminUser.status.name")
    @Mapping(target = "genderName", source = "adminUser.gender.name")
    @Mapping(target = "roles", source = "roles")
    AdminUserResponse convertWithRoles(AdminUser adminUser, List<AdminRole> roles);
}
