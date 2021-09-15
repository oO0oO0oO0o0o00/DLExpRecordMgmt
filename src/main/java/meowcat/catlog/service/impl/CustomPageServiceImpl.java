package meowcat.catlog.service.impl;

import meowcat.catlog.service.CustomPageService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("CustomPageService")
public class CustomPageServiceImpl implements CustomPageService {

    @Override
    public @NotNull Map<String, Object> fillUrls(
            @NotNull Map<String, Object> element, String project, String recordId, String pageId, int ithFold) {
        var path = String.format(
                "api/custom/images/%s/%s/%s/%s/", project, recordId, pageId, ithFold < 0 ? "main" : ithFold);
        return fillUrlsRecur(element, path);
    }

    private @NotNull Map<String, Object> fillUrlsRecur(@NotNull Map<String, Object> element, String path) {
        Map<String, Object> newElement = new HashMap<>();
        var type = ((String) element.get("type"));
        newElement.put("type", type);
        switch (type) {
            case "flow": {
                List<Map<String, Object>> newList = new ArrayList<>();
                for (var child : ((List<?>) element.get("data"))) {
                    @SuppressWarnings("unchecked") var childMap = (Map<String, Object>) child;
                    newList.add(fillUrlsRecur(childMap, path));
                }
                newElement.put("data", newList);
                break;
            }
            case "image": {
                @SuppressWarnings("unchecked")
                var oldData = ((Map<String, Object>) element.get("data"));
                var src = ((String) oldData.get("src"));
                newElement.put("data", Map.of("src", path + src, "description", oldData.get("description")));
                break;
            }
            default:
                newElement.put("data", element.get("data"));
        }
        return newElement;
    }
}
