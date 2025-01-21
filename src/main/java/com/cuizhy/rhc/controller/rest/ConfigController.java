package com.cuizhy.rhc.controller.rest;

import com.cuizhy.rhc.dao.ConfigDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigDao dao;

    @RequestMapping("/get/{type}/{key}")
    public String getValue(@PathVariable String key, @PathVariable String type) {
        return dao.getValue(key, type);
    }

    @RequestMapping("/get/{type}")
    public Map<String,Object> getConfig(@PathVariable String type) {
        return dao.getConfig(type);
    }
}
