package meowcat.catlog.controller;

import meowcat.catlog.model.ExperimentRecord;
import meowcat.catlog.service.MeowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.util.List;

@Controller
@RequestMapping(value = "/")
public class MeowController {

    @Autowired
    private MeowService meowService;

    @RequestMapping
    public ModelAndView request(
            @RequestParam(value = "selected-record", required = false) String selectedRecordName) {
        ModelAndView mv = new ModelAndView("index");
        List<String> records = meowService.getRecordsNames();
        if (records == null) mv.addObject("failed", true);
        else {
            mv.addObject("records", records);
            if (!records.contains(selectedRecordName))
                selectedRecordName = null;
            mv.addObject("selectedRecordName", selectedRecordName);
            if (selectedRecordName != null) {
                ExperimentRecord record = meowService.getRecord(selectedRecordName);
                mv.addObject("selectedRecord", record);
            } else {
                // TODO: Dashboard
            }
        }
        return mv;
    }
}
