package com.cuizhy.rhc.global;

import com.cuizhy.rhc.dao.ConfigDao;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.*;

/**
 * Jenkins全局请求管理器
 */
@Component
@Data
@Slf4j
public class GlobalRequestManager implements ApplicationRunner {

    @Autowired
    private ConfigDao configDao;


    private HttpClient session;
    private Map<String,String> cookies = new HashMap<>();

    private String crumb;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        HttpClient.Builder builder = HttpClient.newBuilder();
        Map<String,Object> proxyConfig = configDao.getConfig("proxy");
        if (!CollectionUtils.isEmpty(proxyConfig) && proxyConfig.get("enabled") !=null){
            int enabled = Integer.parseInt(proxyConfig.get("enabled").toString());
            if (enabled == 1){
                String proxyHost = proxyConfig.get("host").toString();
                String proxyPort = proxyConfig.get("port").toString();
                InetSocketAddress proxyAddress = new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort));
                ProxySelector proxySelector = ProxySelector.of(proxyAddress);
                ProxySelector.setDefault(proxySelector);
                builder.proxy(proxySelector);

                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", proxyPort);

                log.info("network proxy set successful");
            }
        }

        this.session = builder
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
