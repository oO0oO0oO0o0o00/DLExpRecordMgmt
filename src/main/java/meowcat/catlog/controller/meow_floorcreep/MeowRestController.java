package meowcat.catlog.controller.meow_floorcreep;

import meowcat.catlog.service.meow_floorcreep.MeowService;
import org.apache.commons.vfs2.FileSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController("meow-floorcreep-rest-controller")
@RequestMapping(value = "/api/meow-floorcreep")
public class MeowRestController {

    @Autowired
    private MeowService meowService;

    @RequestMapping("history/{record}/{ith_fold}")
    public String getHistory(@PathVariable("record") String id,
                             @PathVariable("ith_fold") int ithFold) throws FileSystemException {
        return meowService.getRecord(id).getHistory(ithFold);
    }

    @RequestMapping("models-summary/{record}")
    public String getModelsSummary(@PathVariable("record") String id) throws FileSystemException {
        return meowService.getRecord(id).getModelsSummaryIndex();
    }

    @RequestMapping("models-summary/{record}/{model-id}")
    public void getModelSummaryImage(
            @PathVariable("record") String recordId, @PathVariable("model-id") String modelId,
            HttpServletResponse response) throws IOException {
        try (var img = meowService.getRecord(recordId).getModelSummaryImage(modelId)) {
            response.setContentType("image/png");
            img.getContent().getInputStream().transferTo(response.getOutputStream());
        }
    }

    @RequestMapping("delete-weights/{record}")
    public Map<String, Object> deleteWeights(
            @PathVariable("record") String recordId, HttpServletResponse response) throws IOException {
        var result = new HashMap<String, Object>();
        result.put("status", meowService.getRecord(recordId).deleteWeights());
        return result;
    }
}
