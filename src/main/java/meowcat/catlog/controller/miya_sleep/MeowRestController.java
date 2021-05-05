package meowcat.catlog.controller.miya_sleep;

import meowcat.catlog.service.miya_sleep.MeowService;
import org.apache.commons.vfs2.FileSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.HttpResource;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController("miya-sleep-rest-controller")
@RequestMapping(value = "/api/miya-sleep")
public class MeowRestController {

    @Autowired
    private MeowService meowService;

    @RequestMapping("history/{record}")
    public String getDetail(@PathVariable("record") String id) throws FileSystemException {
        return meowService.getRecord(id).getHistory();
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

    @RequestMapping("prediction/{record}")
    public void getPredictionImage(
            @PathVariable("record") String recordId, HttpServletResponse response) throws IOException {
        try (var img = meowService.getRecord(recordId).getPredictionImage()) {
            response.setContentType("image/svg+xml");
            img.getContent().getInputStream().transferTo(response.getOutputStream());
        }
    }
}
