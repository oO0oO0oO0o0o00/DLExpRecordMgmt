package meowcat.catlog.controller.meow_bonn_metric;

import meowcat.catlog.service.meow_bonn_metric.MeowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/meow-bonn")
public class MeowRestController {

    @Autowired
    private MeowService meowService;

    @RequestMapping("detail")
    public String getDetail(
            @RequestParam("record") String id,
            @RequestParam("ith") String ithFoldString
    ) {
        int ithFold = Integer.parseInt(ithFoldString);
        return meowService.getDetail(id, ithFold);
    }

    @RequestMapping("delete-weights")
    public String deleteWeights(@RequestParam("id") String id) {
        return meowService.deleteWeights(id) ? "true" : "false";
    }
}
