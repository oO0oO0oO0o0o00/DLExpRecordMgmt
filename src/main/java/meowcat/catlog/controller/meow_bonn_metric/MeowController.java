package meowcat.catlog.controller.meow_bonn_metric;

import meowcat.catlog.model.ExperimentRecord;
import meowcat.catlog.service.MeowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
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
        List<ExperimentRecord> allRecords = meowService.getRecordsBrief();
        if (allRecords == null) mv.addObject("failed", true);
        else {
            mv.addObject("allRecords", allRecords);
            String requested = selectedRecordName;
            if (selectedRecordName == null || allRecords.stream().noneMatch(experimentRecord -> requested.equals(experimentRecord.getId())))
                selectedRecordName = null;
            ExperimentRecord selectedRecord;
            if (selectedRecordName != null) {
                selectedRecord = meowService.getRecord(selectedRecordName);
            } else {
                selectedRecord = null;
                var records = new ArrayList<ExperimentRecord>();
                for (var recordName : allRecords) records.add(0, meowService.getRecord(recordName.getId()));
                mv.addObject("featuredRecords", records);
            }
            mv.addObject("selectedRecord", selectedRecord);
        }
        return mv;
    }
}
