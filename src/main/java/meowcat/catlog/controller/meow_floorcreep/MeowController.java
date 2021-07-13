package meowcat.catlog.controller.meow_floorcreep;

import meowcat.catlog.model.meow_floorcreep.ExperimentRecord;
import meowcat.catlog.service.meow_floorcreep.MeowService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Controller("meow-floorcreep-controller")
@RequestMapping(value = "/meow-floorcreep")
public class MeowController {

    @Autowired
    private MeowService meowService;

    private final Logger logger = LogManager.getLogger(this);

    @RequestMapping
    public ModelAndView request(
            @RequestParam(value = "selected-record", required = false) String selectedRecordName) {
        ModelAndView mv = new ModelAndView("meow_floorcreep/index");
        List<ExperimentRecord> allRecords = null;
        try {
            allRecords=meowService.getRecords();
        }catch (Exception e){
            logger.warn("cannot get records",e);
        }
        if (allRecords == null) {
            mv.addObject("failed", true);
            return mv;
        }
        mv.addObject("all_records", allRecords);
        if (selectedRecordName != null)
            try {
                mv.addObject("selected_record", allRecords.stream()
                        .filter(experimentRecord -> Objects.equals(selectedRecordName, experimentRecord.getFolderName()))
                        .findAny().orElse(allRecords.size() > 0 ? allRecords.get(0) : null));
            }catch (Exception e){
                logger.warn("cannot find selected record",e);
            }
        return mv;
    }

    @GetMapping("config/{record-id}")
    public ModelAndView getConfigViewer(
            @PathVariable("record-id") String recordId) throws IOException {
        ModelAndView mv = new ModelAndView("meow_floorcreep/code_viewer");
        mv.addObject("code", meowService.getRecord(recordId).getConfigFile());
        return mv;
    }

    @GetMapping("log/{record-id}")
    public ModelAndView getLogViewer(
            @PathVariable("record-id") String recordId) throws IOException {
        ModelAndView mv = new ModelAndView("meow_floorcreep/log_viewer");
        mv.addObject("code", meowService.getRecord(recordId).getLogFile());
        return mv;
    }
}
