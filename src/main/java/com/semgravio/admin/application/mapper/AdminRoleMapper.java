package com.semgravio.admin.application.mapper;

import org.mapstruct.Mapper;

import com.semgravio.admin.application.dto.response.RoleResponse;
import com.semgravio.admin.domain.aggregate.AdminRole;
import com.cartisan.web.mapper.DomainMapper;

/**
 * 角色 Mapper。
 *
 * @since 0.1.0
 */
@Mapper(componentModel = "spring")
public interface AdminRoleMapper extends DomainMapper<AdminRole, RoleResponse> {
}
