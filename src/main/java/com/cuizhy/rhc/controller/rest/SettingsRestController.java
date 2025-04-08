package com.cuizhy.rhc.controller.rest;

import com.cuizhy.rhc.constants.Constants;
import com.cuizhy.rhc.dao.ConfigDao;
import com.cuizhy.rhc.vo.JenkinsSettingsSavePVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/settings")
public class SettingsRestController {

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private ObjectMapper objectMapper;

    @RequestMapping("save")
    public void save(@RequestBody JenkinsSettingsSavePVO jenkinsSettingsSavePVO) {
        // 将对象转换为 Map
        Map<String, Object> fieldMap = objectMapper.convertValue(jenkinsSettingsSavePVO, new TypeReference<>() {});

        fieldMap.forEach((key, value) ->
                configDao.setValue(
                        key,
                        Constants.CONFIG_TYPE_JENKINS,
                        value != null ? value.toString() : ""
                )
        );
    }
}
