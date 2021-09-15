package meowcat.catlog.service;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface CustomPageService {

    @NotNull Map<String, Object> fillUrls(
            @NotNull Map<String, Object> element, String project, String recordId, String pageId, int ithFold);

}
