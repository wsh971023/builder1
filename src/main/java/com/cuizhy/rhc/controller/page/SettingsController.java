package com.cuizhy.rhc.controller.page;

import com.cuizhy.rhc.constants.Constants;
import com.cuizhy.rhc.dao.ConfigDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    @Autowired
    private ConfigDao configDao;

    @RequestMapping("jenkins")
    public String jenkins(Model model){
        Map<String,Object> config = configDao.getConfig(Constants.CONFIG_TYPE_JENKINS);
        model.addAttribute("config", config);
        return "settings/jenkins";
    }

    @RequestMapping("git")
    public String git(Model model){
        Map<String,Object> config = configDao.getConfig(Constants.CONFIG_TYPE_GIT);
        model.addAttribute("config", config);
        return "settings/git";
    }

    @RequestMapping("proxy")
    public String proxy(Model model){
        Map<String,Object> config = configDao.getConfig(Constants.CONFIG_TYPE_PROXY);
        model.addAttribute("config", config);

        //获取当前系统代理配置
        if (System.getProperty("http.proxyHost") != null && System.getProperty("http.proxyPort") != null){
            model.addAttribute("currentProxy", System.getProperty("http.proxyHost") + ":" + System.getProperty("http.proxyPort"));
        }else {
            model.addAttribute("currentProxy", "null");
        }
        return "settings/proxy";
    }


}
