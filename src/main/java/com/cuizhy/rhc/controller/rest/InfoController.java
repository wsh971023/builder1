package com.cuizhy.rhc.controller.rest;

import com.cuizhy.rhc.dao.InfoDao;
import com.cuizhy.rhc.model.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/info")
public class InfoController {

    @Autowired
    private InfoDao infoDao;

    @RequestMapping("/get")
    public List<Info> getInfo(){
        return infoDao.getInfo();
    }

    @RequestMapping("/get/{name}/{env}")
    public Info getInfo(@PathVariable String name, @PathVariable String env){
        return infoDao.getInfo(name, env);
    }

    @RequestMapping("/get/{env}")
    public List<Info> getInfo(@PathVariable String env){
        return infoDao.getInfo(env);
    }
}
