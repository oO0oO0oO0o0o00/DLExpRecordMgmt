package meowcat.catlog.controller;

import meowcat.catlog.config.ClusterConfig;
import meowcat.catlog.service.MeowService;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController("meow-rest-controller")
@RequestMapping(value = "/api")
public class MeowRestController {

    private final Logger logger = LogManager.getLogger(this);

    private final MeowService meowService;

    public MeowRestController(MeowService meowService) {
        this.meowService = meowService;
    }

    @RequestMapping("history")
    public String getHistory(
            @RequestParam("project") String project,
            @RequestParam("record-id") String id,
            @RequestParam("ith-fold") int ithFold) throws ClusterConfig.MeowException {
        return meowService.getRecord(project, id).getHistory(ithFold - 1);
    }

    @RequestMapping("models-summary")
    public String getModelsSummary(
            @RequestParam("project") String project,
            @RequestParam("record-id") String id) throws FileSystemException, ClusterConfig.MeowException {
        return meowService.getRecord(project, id).getModelsSummaryIndex();
    }

    @GetMapping("models-summary/{project}/{record-id}/{model-id}")
    public void getModelSummaryImage(
            @PathVariable("project") String project,
            @PathVariable("record-id") String recordId, @PathVariable("model-id") String modelId,
            HttpServletResponse response) throws IOException {
        try (var img = meowService.getRecord(project, recordId).getModelSummaryImage(modelId)) {
            response.setContentType("image/png");
            img.getContent().getInputStream().transferTo(response.getOutputStream());
        } catch (ClusterConfig.MeowException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("delete")
    public Map<String, Object> delete(
            @RequestParam("project") String project,
            @RequestParam("record-id") String recordId) throws IOException, ClusterConfig.MeowException {
        var result = new HashMap<String, Object>();
        result.put("status", meowService.getRecord(project, recordId).delete());
        return result;
    }

    @RequestMapping("delete-weights")
    public Map<String, Object> deleteWeights(
            @RequestParam("project") String project,
            @RequestParam("record-id") String recordId) throws IOException, ClusterConfig.MeowException {
        var result = new HashMap<String, Object>();
        result.put("status", meowService.getRecord(project, recordId).deleteWeights());
        return result;
    }

    @GetMapping("custom/images/{project}/{record-id}/{custom-page}/{ith-fold}/{name}")
    public void getAttachmentImage(
            @PathVariable("project") String project,
            @PathVariable("record-id") String recordId,
            @PathVariable("custom-page") String customPage,
            @PathVariable("ith-fold") String ithFold,
            @PathVariable("name") String name,
            HttpServletResponse response) throws IOException {
        if ("main".equals(ithFold)) ithFold = null;
        try (var img = meowService.getRecord(project, recordId)
                .getAttachment(customPage, ithFold, name)) {
            response.setContentType("image/png");
            img.getContent().getInputStream().transferTo(response.getOutputStream());
        } catch (ClusterConfig.MeowException e) {
            logger.warn(e);
        }
    }
}
