package com.cuizhy.rhc.service;

import com.cuizhy.rhc.cache.CacheUtil;
import com.cuizhy.rhc.constants.Constants;
import com.cuizhy.rhc.dao.ConfigDao;
import com.cuizhy.rhc.global.GlobalRequestManager;
import com.cuizhy.rhc.model.Info;
import com.cuizhy.rhc.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Service
public class JenkinsService {

    @Autowired
    private GlobalRequestManager requestManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private CacheUtil cacheUtil;


    private boolean checkLoginStatus(String url,String username){
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest loginPageRequest = requestManager.createRequestBuilder(url + "/user/"+username+"/").GET().build();
        try {
            HttpResponse<String> response = client.send(loginPageRequest, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();
            if (responseCode != 200) {
                return false;
            }
            log.info("校验Jenkins登录状态: 已登录 ");
            return true;
        } catch (Exception e) {
            log.warn("校验Jenkins登录状态: 未登录 ",e);
        }
        return false;
    }

    /**
     * 登录 Jenkins
     * @param url
     * @param username
     * @param password
     * @throws Exception
     */
    public void loginToJenkins(String url, String username, String password){

        if (checkLoginStatus(url,username)){
            return;
        }

        try {
            cacheUtil.set(Constants.JENKINS_LOGIN_CACHE_KEY,Constants.JOB_STATUS_RUNNING);
            log.info("正在请求登录页面...");
            HttpRequest loginPageRequest = requestManager.createRequestBuilder(url + "/login").GET().build();
            HttpResponse<String> loginPageResponse = requestManager.getSession().send(loginPageRequest, HttpResponse.BodyHandlers.ofString());

            if (loginPageResponse.statusCode() != 200) {
                throw new RuntimeException("请求登录页面失败！响应内容: " + loginPageResponse.body());
            }

            // 准备登录数据
            String formData = "j_username=" + username + "&j_password=" + password + "&Submit=登录";

            HttpRequest loginRequest = requestManager.createRequestBuilder(url + "/j_spring_security_check")
                    .POST(BodyPublishers.ofString(formData))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            log.info("正在尝试登录...");
            HttpResponse<String> loginResponse = requestManager.getSession().send(loginRequest, HttpResponse.BodyHandlers.ofString());

            if (loginResponse.statusCode() != 200) {
                throw new RuntimeException("登录失败！响应内容: " + loginResponse.body());
            }

            // 保存 Cookies
            saveCookies(loginResponse.headers());
            log.info("登录成功...");
        }catch (Exception e){
            cacheUtil.set(Constants.JENKINS_LOGIN_CACHE_KEY,Constants.JOB_STATUS_FAIL);
            throw new RuntimeException("Jenkins 登录失败",e);
        }
    }

    // 保存 Cookies
    private void saveCookies(HttpHeaders headers) {
        headers.allValues("Set-Cookie").forEach(cookie -> {
            String[] parts = cookie.split(";", 2);
            if (parts.length > 0) {
                String[] keyValue = parts[0].split("=", 2);
                if (keyValue.length == 2) {
                    requestManager.getCookies().put(keyValue[0], keyValue[1]);
                }
            }
        });
    }

    // 获取 Crumb
    public String getJenkinsCrumb(String url) {
        try{
            log.info("正在获取 Jenkins-Crumb...");
            HttpRequest crumbRequest = requestManager.createRequestBuilder(url + "/crumbIssuer/api/json").GET().build();
            HttpResponse<String> crumbResponse = requestManager.getSession().send(crumbRequest, HttpResponse.BodyHandlers.ofString());

            if (crumbResponse.statusCode() != 200) {
                throw new RuntimeException("获取 Crumb 失败！响应内容: " + crumbResponse.body());
            }

            log.info("获取 Crumb 成功...");
            JsonNode jsonNode = objectMapper.readTree(crumbResponse.body());
            requestManager.setCrumb(jsonNode.get("crumb").asText());
            return jsonNode.get("crumb").asText();
        }catch (Exception e){
            cacheUtil.set(Constants.JENKINS_LOGIN_CACHE_KEY,Constants.JOB_STATUS_FAIL);
            throw new RuntimeException("Jenkins Crumb 获取失败",e);
        }
    }

    // 触发构建
    public void triggerBuild(String url,Info info){
        log.info("正在触发构建任务...");
        try {
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_RUNNING);
            cacheUtil.addInfoToJobList(info);
            String buildUrl = url + "/job/" + info.getJobName() + "/build";

            HttpRequest buildRequest = requestManager.createRequestBuilder(buildUrl)
                    .POST(BodyPublishers.noBody())
                    .header("Jenkins-Crumb", requestManager.getCrumb())
                    .build();

            HttpResponse<String> buildResponse = requestManager.getSession().send(buildRequest, HttpResponse.BodyHandlers.ofString());

            if (buildResponse.statusCode() != 201) {
                throw new RuntimeException("触发构建失败！响应内容: " + buildResponse.body());
            }

            log.info("开始构建...");
        } catch (Exception e) {
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_FAIL);
            cacheUtil.addInfoToJobList(info);
            throw new RuntimeException("Jenkins 触发构建失败",e);
        }
    }

    /**
     * 获取构建任务状态
     * @param jobName
     * @return
     * @throws Exception
     */
    public boolean getBuildStatus(String jobName) throws Exception {
        log.info("正在获取构建任务状态...");
        String jenkinsUrl = configDao.getValue("url", "jenkins");
        String job_info_url = configDao.getValue("job_info_url", "jenkins");
        String build_status_url = configDao.getValue("build_status_url", "jenkins");
        int buildNumber = getBuildNumber(jenkinsUrl, job_info_url, jobName, false);
        String statusUrl = String.format(build_status_url, buildNumber, jobName);

        HttpRequest statusRequest = HttpRequest.newBuilder()
                .uri(URI.create(jenkinsUrl + statusUrl.replace("{job_name}", jobName).replace("{build_number}", String.valueOf(buildNumber))))
                .GET()
                .header("Jenkins-Crumb", requestManager.getCrumb())
                .build();

        HttpResponse<String> statusResponse = requestManager.getSession().send(statusRequest, HttpResponse.BodyHandlers.ofString());
        if (statusResponse.statusCode() != 200) {
            log.info("获取构建任务状态失败！响应内容: " + statusResponse.body());
            return false;
        }
        // 解析 JSON 响应为 JsonNode
        JsonNode statusData = objectMapper.readTree(statusResponse.body());
        // 提取字段数据
        boolean building = statusData.get("building").asBoolean(); // 获取布尔值
        return !building;
    }

    public boolean checkBuildStatus(String jenkinsUrl, String buildStatusUrl, int buildNumber, Info info) throws Exception {
        log.info("正在监听构建任务状态...");

        String statusUrl = String.format(buildStatusUrl, buildNumber, info.getJobName());

        boolean success = false;

        while (true) {
            HttpRequest statusRequest = HttpRequest.newBuilder()
                    .uri(URI.create(jenkinsUrl + statusUrl.replace("{job_name}", info.getJobName()).replace("{build_number}", String.valueOf(buildNumber))))
                    .GET()
                    .header("Jenkins-Crumb", requestManager.getCrumb())
                    .build();

            HttpResponse<String> statusResponse = requestManager.getSession().send(statusRequest, HttpResponse.BodyHandlers.ofString());

            if (statusResponse.statusCode() != 200) {
                log.info("获取构建任务状态失败！响应内容: " + statusResponse.body());
                break;
            }
            // 解析 JSON 响应为 JsonNode
            JsonNode statusData = objectMapper.readTree(statusResponse.body());

            // 提取字段数据
            boolean building = statusData.get("building").asBoolean(); // 获取布尔值
            String result = statusData.has("result") ? statusData.get("result").asText() : null; // 检查并获取结果字段

            if (building) {
                log.info("构建任务 " + buildNumber + " 正在进行中... 等待中...");
                Thread.sleep(5000); // 每5秒检查一次
            } else {
                if ("SUCCESS".equals(result)) {
                    log.info("构建任务 " + buildNumber + " 已成功完成！");
                    success = true;
                    info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_SUCCESS);
                    cacheUtil.addInfoToJobList(info);
                } else if ("FAILURE".equals(result)) {
                    log.info("构建任务 " + buildNumber + " 执行失败！");
                    info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_FAIL);
                    cacheUtil.addInfoToJobList(info);
                } else if ("ABORTED".equals(result)){
                    log.info("构建任务 " + buildNumber + " 状态已放弃，结果: " + result);
                    info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_SUCCESS);
                    cacheUtil.addInfoToJobList(info);
                } else {
                    log.info("构建任务 " + buildNumber + " 状态未知，结果: " + result);
                    info.setStatus(Constants.JOB_PROGRESS_JENKINS_BUILD,Constants.JOB_STATUS_FAIL);
                    cacheUtil.addInfoToJobList(info);
                }
                break;
            }
        }

        return success;
    }

    public int getBuildNumber(String jenkinsUrl, String jobInfoUrl,String jobName, boolean buildRequired) throws Exception {
        if (buildRequired) {
            log.info("本次任务需等待Jenkins构建...");
            log.info("正在获取当前构建任务编号, 等待30秒...");
            Thread.sleep(10000); // 等待30秒，确保构建任务启动
        } else {
            log.info("本次任务无需等待Jenkins构建...");
        }

        // 获取任务信息
        HttpRequest jobInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create(jenkinsUrl + jobInfoUrl.replace("{job_name}", jobName)))
                .GET()
                .build();

        HttpResponse<String> jobInfoResponse = requestManager.getSession().send(jobInfoRequest, HttpResponse.BodyHandlers.ofString());

        if (jobInfoResponse.statusCode() != 200) {
            throw new RuntimeException("获取任务信息失败！响应内容: " + jobInfoResponse.body());
        }

        // 解析任务信息
        JsonNode jobInfo = objectMapper.readTree(jobInfoResponse.body());
        JsonNode lastBuildNode = jobInfo.path("lastBuild");
        int buildNumber = lastBuildNode.path("number").asInt();

        if (buildNumber == 0) {
            throw new RuntimeException("无法获取最新的构建编号！");
        }

        log.info("当前最新的构建任务编号：" + buildNumber);
        return buildNumber;
    }

    // 下载文件
    public void downloadFile(String url,String filePath, String saveDir,Info info) {
        log.info("准备下载文件: " + filePath + "...");

        try{
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_DOWNLOAD,Constants.JOB_STATUS_RUNNING);
            cacheUtil.addInfoToJobList(info);
            String fileUrl = url + "/job/"+info.getJobName() + "/ws/" + filePath;
            HttpRequest fileRequest = requestManager.createRequestBuilder(fileUrl)
                    .header("Jenkins-Crumb", requestManager.getCrumb())
                    .header("Accept-Encoding", "gzip, deflate")
                    .GET()
                    .build();

            HttpResponse<InputStream> fileResponse = requestManager.getSession().send(fileRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (fileResponse.statusCode() != 200) {
                throw new RuntimeException("下载文件失败！响应内容: " + new String(fileResponse.body().readAllBytes()));
            }

            // 获取文件总大小，用于计算进度
            long contentLength = fileResponse.headers().firstValueAsLong("Content-Length").orElse(-1L);
            log.info("文件大小: " + (contentLength >= 0 ? contentLength + " 字节" : "未知"));

            // 设置保存路径
            Path savePath = Paths.get(saveDir, filePath.substring(filePath.lastIndexOf("/") + 1));
            Files.createDirectories(savePath.getParent());

            // 使用 FileOutputStream 保存文件，并逐步读取 InputStream 计算进度
            try (InputStream inputStream = fileResponse.body();
                 FileOutputStream outputStream = new FileOutputStream(savePath.toFile())) {
                byte[] buffer = new byte[8192]; // 缓冲区大小
                long downloadedBytes = 0;
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;

                    // 更新进度
                    if (contentLength > 0) {
                        int progress = (int) ((downloadedBytes * 100) / contentLength);
                        System.out.printf("\rDownload process: %d%%", progress);
                    } else {
                        System.out.printf("\rDownloaded: %d Bytes", downloadedBytes);
                    }
                }
                System.out.println("\nDownload Success");
            }
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_DOWNLOAD,Constants.JOB_STATUS_SUCCESS);
            cacheUtil.addInfoToJobList(info);
            log.info("文件已成功下载到: " + savePath);
        }catch (Exception e){
            info.setStatus(Constants.JOB_PROGRESS_JENKINS_DOWNLOAD,Constants.JOB_STATUS_FAIL);
            cacheUtil.addInfoToJobList(info);
            throw new RuntimeException("下载文件失败！",e);
        }
    }


    public String getDownloadPath(Info info){
        String fileName = info.getFilepath().substring(info.getFilepath().lastIndexOf("/") + 1);
        return FileUtil.getRuntimeAbsolutePath() +File.separator + Constants.JENKINS_DOWNLOAD_DIR + File.separator+fileName;
    }
}
