package meowcat.catlog.controller.miya_sleep;

import meowcat.catlog.model.miya_sleep.ExperimentRecord;
import meowcat.catlog.service.miya_sleep.MeowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Objects;

@Controller("miya-sleep-controller")
@RequestMapping(value = "/miya-sleep")
public class MeowController {

    @Autowired
    private MeowService meowService;

    @RequestMapping
    public ModelAndView request(
            @RequestParam(value = "selected-record", required = false) String selectedRecordName) {
        ModelAndView mv = new ModelAndView("miya_sleep/index");
        List<ExperimentRecord> allRecords = meowService.getRecords();
        if (allRecords == null) {
            mv.addObject("failed", true);
            return mv;
        }
        mv.addObject("all_records", allRecords);
        if (selectedRecordName != null)
            mv.addObject("selected_record", allRecords.stream()
                    .filter(experimentRecord -> Objects.equals(selectedRecordName, experimentRecord.getFolderName()))
                    .findAny().orElse(allRecords.size() > 0 ? allRecords.get(0) : null));
        return mv;
    }
}
