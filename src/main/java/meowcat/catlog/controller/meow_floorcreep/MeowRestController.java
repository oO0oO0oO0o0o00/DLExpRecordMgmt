package meowcat.catlog.controller.meow_floorcreep;

import com.fasterxml.jackson.core.JsonProcessingException;
import meowcat.catlog.service.meow_floorcreep.MeowService;
import org.apache.commons.vfs2.FileSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController("meow-floorcreep-rest-controller")
@RequestMapping(value = "/api/meow-floorcreep")
public class MeowRestController {

    private final MeowService meowService;

    public MeowRestController(MeowService meowService) {
        this.meowService = meowService;
    }

    @RequestMapping("history")
    public String getHistory(@RequestParam("record-id") String id,
                             @RequestParam("ith-fold") int ithFold) throws FileSystemException, JsonProcessingException {
        return meowService.getRecord(id).getHistory(ithFold);
    }

    @RequestMapping("models-summary")
    public String getModelsSummary(@RequestParam("record-id") String id) throws FileSystemException {
        return meowService.getRecord(id).getModelsSummaryIndex();
    }

    @GetMapping("models-summary/{record-id}/{model-id}")
    public void getModelSummaryImage(
            @PathVariable("record-id") String recordId, @PathVariable("model-id") String modelId,
            HttpServletResponse response) throws IOException {
        try (var img = meowService.getRecord(recordId).getModelSummaryImage(modelId)) {
            response.setContentType("image/png");
            img.getContent().getInputStream().transferTo(response.getOutputStream());
        }
    }

    @RequestMapping("delete")
    public Map<String, Object> delete(
            @RequestParam("record-id") String recordId, HttpServletResponse response) throws IOException {
        var result = new HashMap<String, Object>();
        result.put("status", meowService.getRecord(recordId).delete());
        return result;
    }

    @RequestMapping("delete-weights")
    public Map<String, Object> deleteWeights(
            @RequestParam("record-id") String recordId, HttpServletResponse response) throws IOException {
        var result = new HashMap<String, Object>();
        result.put("status", meowService.getRecord(recordId).deleteWeights());
        return result;
    }
}
