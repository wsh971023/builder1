package com.cuizhy.rhc;

import lombok.extern.slf4j.Slf4j;
import org.komamitsu.spring.data.sqlite.EnableSqliteRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;

@Slf4j
@SpringBootApplication(proxyBeanMethods = false)
@EnableSqliteRepositories
public class RhcBuilderJavaApplication {

    public static void main(String[] args) {

        //SpringApplication.run(RhcBuilderJavaApplication.class, args);
        // 添加环境变量监听器
        SpringApplication app = new SpringApplication(RhcBuilderJavaApplication.class);

        String encoding = "UTF-8";
        String osName = System.getProperty("os.name").toLowerCase();
        log.info("OS Name: {}" , osName);

        String classpath = System.getProperty("java.class.path");
        log.info("Classpath: {}" , classpath);
        if (osName.toLowerCase().contains("window") && !classpath.contains("idea_rt.jar")){
            try {
                Process process = Runtime.getRuntime().exec("C:\\Windows\\System32\\chcp.com");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                reader.close();
                // 这里将结果作为一个字符串返回
                String output = result.toString();
                if (output.contains("65001")){
                    encoding = "UTF-8";
                }else if (output.contains("936")){
                    encoding = "GBK";
                }
            }catch (Exception ignore){}
        }
        log.info("Default Charset: {}" , encoding);
        System.setProperty("logEncoding", encoding);

        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
            URL jarUrl = RhcBuilderJavaApplication.class.getProtectionDomain().getCodeSource().getLocation();
            String jarPath = jarUrl.getPath();
            File jarFile = new File(jarPath);
            String jarDirPath = jarFile.getParentFile().getAbsolutePath();
            // 设置 Spring 环境中的 db.url
            event.getEnvironment().getSystemProperties().put("db.url", "jdbc:sqlite:" + jarDirPath+"/db/identifier.sqlite");
        });

        // 启动 Spring Boot 应用
        app.run(args);
    }

}
