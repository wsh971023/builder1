package com.cuizhy.rhc.global;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Jenkins全局请求管理器
 */
@Component
@Data
public class GlobalRequestManager {

    private HttpClient session;
    private Map<String,String> cookies = new HashMap<>();

    private String crumb;
    /**
     * bean初始化
     * */
    @PostConstruct
    private void init(){
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        this.session = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL) // 启用正常重定向
                .cookieHandler(cookieManager)               // 绑定 CookieManager
                .build();
    }

    /**
     * 构建 Cookie Header
     * @return Cookie Header
     */
    private String buildCookieHeader() {
        StringBuilder cookieHeader = new StringBuilder();
        this.getCookies().forEach((key, value) -> cookieHeader.append(key).append("=").append(value).append("; "));
        return cookieHeader.toString();
    }

    /**
     * 创建带 Headers 的 Request Builder
     * @param url 请求的 URL
     * @return
     */
    public HttpRequest.Builder createRequestBuilder(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept", "application/json")
                .header("Cookie", buildCookieHeader());
    }
}
