package meowcat.catlog.controller;

import meowcat.catlog.service.MeowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/")
public class MeowRestController {

    @Autowired
    private MeowService meowService;

    @RequestMapping("detail")
    public String getDetail(
            @RequestParam("record") String recordName,
            @RequestParam("ith") String ithFoldString
    ) {
        int ithFold = Integer.parseInt(ithFoldString);
        return meowService.getDetail(recordName, ithFold);
    }

}
