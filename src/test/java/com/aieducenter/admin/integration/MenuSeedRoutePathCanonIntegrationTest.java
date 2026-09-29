package com.aieducenter.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * 菜单种子 routePath ⊆ elegant-router 正典推导（issue #94 防复发——fail fast 校验）。
 *
 * <p><b>为什么种子 routePath 必须等于推导值</b>：admin-web 动态菜单模式下路由按后端
 * {@code /menus/my} 下发的 routePath 注册（{@code transformBackendMenuToMenuRoute} 直拷），
 * 而前端静态正典表（{@code src/router/elegant/transform.ts}）按 @elegant-router/core 的
 * {@code transformRouterNameToPath = '/' + name.replaceAll("_", "/")} 从路由名推导 path
 * （每个下划线一段）。两者不一致时：侧边栏按路由 name 跳转照常渲染（日常无感），正典
 * 路径深链 / F5 / 书签落 not-found catch-all → 守卫查路由名存在 → 导 403 而非 404
 * （admin-web #70 三层根因）。手写种子字段漂移已有两例前科（V18 icon 未收录集、
 * V15/V18/V19/V20 routePath 扁平 kebab——本票 V21 修正五条），推导校验收口一处即绝后患；
 * 前端明确不加自愈逻辑——契约错误应尽早暴露，而非静默自愈掩盖。</p>
 *
 * <p><b>为什么跑在独立 Flyway schema 上</b>：常规测试库 {@code flyway.enabled=false} +
 * {@code ddl-auto=create}（空表、无种子），种子行在常规环境不可达；本类照
 * {@code RoleAssignmentFlywaySchemaIntegrationTest} 先例在独立 schema
 * {@code menu_seed_canon_flyway} 全量跑 Flyway 链 + {@code ddl-auto=none} 复刻生产
 * schema，直查 {@code sys_admin_menus} 断言种子终态。与「不写种子快照测试」（V12
 * 教训）不冲突：不比对行快照，断言的是推导不变式——任何未来种子迁移违反即红，
 * 单行违规逐条列名（id / route_name / 实际值 / 正典值）。</p>
 *
 * <p><b>口径</b>：校验覆盖全部框架页行（{@code href IS NULL}——外链行不注册框架路由，
 * issue #94 明示逃生门，手写 path 豁免）；比票面「routeName 属 admin-web 路由集时」更严：
 * 后端测试无法枚举前端路由集，而本应用种子全部为 elegant 框架页——真出现自定义页时
 * 本测试变红、由人拍板（fail fast 即设计意图）。</p>
 *
 * @since 0.1.0
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:25432/aieducenter_test?currentSchema=menu_seed_canon_flyway",
        "spring.flyway.enabled=true",
        "spring.flyway.schemas=menu_seed_canon_flyway",
        "spring.flyway.default-schema=menu_seed_canon_flyway",
        "spring.jpa.hibernate.ddl-auto=none"
})
class MenuSeedRoutePathCanonIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @AfterAll
    void dropSchema() throws Exception {
        // 清理：删除本测试专用的独立 schema，不污染共享测试库
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS menu_seed_canon_flyway CASCADE");
        }
    }

    @Test
    @DisplayName("全量 Flyway 链种子行：框架页 routePath == '/' + routeName.replaceAll('_','/')（href 外链行豁免；route_name 必填）")
    void given_fullFlywayMenuSeeds_when_queryAllRows_then_frameworkPageRoutePathMatchesElegantRouterDerivation() throws Exception {
        List<String> violations = new ArrayList<>();
        int rowCount = 0;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, route_name, route_path, href FROM sys_admin_menus ORDER BY id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rowCount++;
                long id = resultSet.getLong("id");
                String routeName = resultSet.getString("route_name");
                String routePath = resultSet.getString("route_path");
                String href = resultSet.getString("href");

                if (routeName == null) {
                    violations.add("id=" + id + "：route_name 为 null——框架页种子必须带路由名（推导的输入）");
                    continue;
                }
                if (href != null) {
                    // 外链行不注册框架路由，手写 path 豁免（issue #94 逃生门）
                    continue;
                }
                String derived = "/" + routeName.replace("_", "/");
                if (!derived.equals(routePath)) {
                    violations.add("id=" + id + " " + routeName + "：routePath=" + routePath
                            + " ≠ 正典推导 " + derived);
                }
            }
        }

        assertThat(rowCount)
                .as("全量 Flyway 链应种出菜单行（空集将令推导断言哑绿）")
                .isPositive();
        assertThat(violations)
                .as("种子 routePath 必须等于 elegant-router 正典推导 '/' + routeName.replaceAll('_','/')"
                        + "——正典路径深链/F5 落 403 的根因（admin-web #70 / issue #94）")
                .isEmpty();
    }
}
