package com.cuizhy.rhc.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class IndexController {

    @RequestMapping("")
    public String index(Model model) {
        model.addAttribute("info","666");
        return "index";
    }
}
