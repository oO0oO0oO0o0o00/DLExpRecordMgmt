package meowcat.catlog.controller;

import meowcat.catlog.service.MeowService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller("index-controller")
@RequestMapping(value = {"/projects", ""})
public class IndexController {

    private final MeowService meowService;

    public IndexController(MeowService meowService) {
        this.meowService = meowService;
    }

    @GetMapping("")
    public ModelAndView get() {
        return new ModelAndView("index").addObject("projects", meowService.getProjects());
    }

}
