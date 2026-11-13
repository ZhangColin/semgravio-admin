package com.semgravio.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * CORS 放行名单配置驱动集成测试（#121）。
 *
 * <p>历史：WebConfig 曾硬编码仅 dev 两个源，生产域名 https://admin.semgravio.com 不在名单，
 * 浏览器登录 POST 携带 Origin 即被 CorsFilter 以 403 "Invalid CORS request" 拒绝——
 * 2026-11-05 首发上线的唯一阻断项。根因与公网差分证据见 GitHub issue #121。</p>
 *
 * <p>断言点（HTTP filter-chain seam）：经 RANDOM_PORT 真实链路打
 * {@code POST /api/admin/auth/login}，断言带放行名单内 Origin 的请求穿过 CorsFilter
 * （到达认证逻辑、以 401 凭证错误回包）且响应回 {@code Access-Control-Allow-Origin}；
 * 名单外 Origin 仍被拒。放行名单经 {@code admin.cors.allowed-origins} 注入——
 * 生产由服务器 .env.production 的 {@code CORS_ALLOWED_ORIGINS} 覆盖。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "admin.cors.allowed-origins=https://admin.semgravio.com, https://ops.semgravio.com")
class CorsAllowedOriginsIntegrationTest {

    private final TestRestTemplate restTemplate;
    private final int port;

    @Autowired
    CorsAllowedOriginsIntegrationTest(
            TestRestTemplate restTemplate, @Value("${local.server.port}") int port) {
        this.restTemplate = restTemplate;
        this.port = port;
    }

    @Test
    @DisplayName("放行名单内的 Origin 穿过 CorsFilter（401 凭证错误而非 403，且回 ACAO 头）")
    void given_originInAllowedList_when_loginPostCarriesIt_then_passesCorsFilter() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setOrigin("https://admin.semgravio.com");
        HttpEntity<String> request = new HttpEntity<>("{\"username\":\"x\",\"password\":\"x\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/admin/auth/login"), HttpMethod.POST, request, String.class);

        // #121 symptom：修复前此处 403 Invalid CORS request；修复后请求到达认证逻辑，坏凭证得 401
        assertThat(response.getStatusCode().value())
                .as("名单内 Origin 不应被 CORS 拒：%s", response.getBody()).isEqualTo(401);
        assertThat(response.getHeaders().getAccessControlAllowOrigin())
                .as("应回显 Access-Control-Allow-Origin").isEqualTo("https://admin.semgravio.com");
    }

    @Test
    @DisplayName("逗号分隔多源名单逐个生效（逗号后带空格不失配）")
    void given_commaSeparatedListWithSpace_when_loginPostCarriesSecondOrigin_then_passesCorsFilter() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setOrigin("https://ops.semgravio.com");
        HttpEntity<String> request = new HttpEntity<>("{\"username\":\"x\",\"password\":\"x\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/admin/auth/login"), HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode().value())
                .as("名单内第二源不应被 CORS 拒：%s", response.getBody()).isEqualTo(401);
    }

    @Test
    @DisplayName("名单外 Origin 仍被 CorsFilter 拒（403，不放行任意来源）")
    void given_originNotInAllowedList_when_loginPostCarriesIt_then_rejectedByCors() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setOrigin("https://evil.example.com");
        HttpEntity<String> request = new HttpEntity<>("{\"username\":\"x\",\"password\":\"x\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/admin/auth/login"), HttpMethod.POST, request, String.class);

        // 名单是显式白名单而非全放行——allowCredentials(true) 下尤其不能松
        assertThat(response.getStatusCode().value())
                .as("名单外 Origin 应被 CORS 拒").isEqualTo(403);
        assertThat(response.getBody()).asString().contains("Invalid CORS request");
    }

    @Test
    @DisplayName("名单内 Origin 的预检 OPTIONS 放行（浏览器实际先打的请求，#121 公网证据之二）")
    void given_originInAllowedList_when_preflightOptions_then_allowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://admin.semgravio.com");
        headers.set("Access-Control-Request-Method", "POST");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/admin/auth/login"), HttpMethod.OPTIONS, request, String.class);

        // 修复前预检同样 403 Invalid CORS request；修复后应 2xx 并回 CORS 头
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("预检应放行：%s", response.getBody()).isTrue();
        assertThat(response.getHeaders().getAccessControlAllowOrigin())
                .as("预检响应应回显 Access-Control-Allow-Origin").isEqualTo("https://admin.semgravio.com");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
