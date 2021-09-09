package meowcat.catlog.controller;

import meowcat.catlog.config.ClusterConfig;
import meowcat.catlog.model.ExperimentRecord;
import meowcat.catlog.service.MeowService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Controller("meow-controller")
@RequestMapping(value = "/projects/{project}")
public class MeowController {

    private final MeowService meowService;

    private final Logger logger = LogManager.getLogger(this);

    public MeowController(MeowService meowService) {
        this.meowService = meowService;
    }

    @GetMapping({"{selected}", ""})
    public ModelAndView getProjectIndex(
            @PathVariable String project,
            @PathVariable(value = "selected", required = false) String selectedRecordName) {
        ModelAndView mv = new ModelAndView("project/index");
        List<ExperimentRecord> allRecords = null;
        try {
            allRecords = meowService.getRecords(project);
        } catch (ClusterConfig.MeowException me) {
            return new ModelAndView("forward:/projects");
        } catch (Exception e) {
            logger.warn("cannot get records", e);
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
            } catch (Exception e) {
                logger.warn("cannot find selected record", e);
            }
        return mv;
    }

    @GetMapping("config")
    public ModelAndView getConfigViewer(
            @PathVariable String project,
            @RequestParam("record-id") String recordId) throws IOException, ClusterConfig.MeowException {
        ModelAndView mv = new ModelAndView("project/code_viewer");
        mv.addObject("code", meowService.getRecord(project, recordId).getConfigFile());
        return mv;
    }

    @GetMapping("log")
    public ModelAndView getLogViewer(
            @PathVariable String project,
            @RequestParam("record-id") String recordId) throws IOException, ClusterConfig.MeowException {
        ModelAndView mv = new ModelAndView("project/log_viewer");
        mv.addObject("code", meowService.getRecord(project, recordId).getLogFile());
        return mv;
    }
}
