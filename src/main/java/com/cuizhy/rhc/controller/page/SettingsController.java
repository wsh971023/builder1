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
}
