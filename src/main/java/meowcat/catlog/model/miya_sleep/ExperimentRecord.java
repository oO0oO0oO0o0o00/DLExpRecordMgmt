package meowcat.catlog.model.miya_sleep;

import meowcat.catlog.util.IoUtil;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.util.FileObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

public class ExperimentRecord implements Serializable {

    private final FileObject directory;

    private Map<String, Object> summary;

    private Map<String, Object> scores;

    private String history;

    private Map<String, Object> progress;

    private Integer heartBeatStatus;

    private String modelsSummaryIndex;

    public ExperimentRecord(FileObject directory) {
        this.directory = directory;
    }

    public Map<String, Object> getSummary() throws FileSystemException {
        if (summary == null) {
            summary = getJsonFileAsMap("summary.json");
            IoUtil.close(directory);
        }
        return summary;
    }

    public Map<String, Object> getScores() throws FileSystemException {
        if (scores == null) {
            scores = getJsonFileAsMap("scores.json");
            IoUtil.close(directory);
        }
        return scores;
    }

    public String getHistory() throws FileSystemException {
        // all right i know these things shall be in service layer but whatever
        if (history == null) {
            history = getTextFileContent(directory, "history.json");
            IoUtil.close(directory);
        }
        return history;
    }

    private static String getTextFileContent(FileObject directory, String s) throws FileSystemException {
        try (var file = directory.getChild(s)) {
            return IoUtil.readText(file);
        }
    }

    @NotNull
    private Map<String, Object> getJsonFileAsMap(String s) throws FileSystemException {
        try (var file = directory.getChild(s)) {
            return Objects.requireNonNull(IoUtil.readJson(file));
        }
    }

    public String getFolderName() {
        return directory.getName().getBaseName();
    }

    public Map<String, Object> getProgress() throws FileSystemException {
        if (progress == null) {
            progress = getJsonFileAsMap("progress.json");
            IoUtil.close(directory);
        }
        return progress;
    }

    public int getHeartBeatStatus() throws FileSystemException {
        if (heartBeatStatus == null) {
            var progress = getProgress();
            var heartBeatTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(((Integer) progress.get("time"))), ZoneId.systemDefault());
            var diff = ChronoUnit.SECONDS.between(heartBeatTime, LocalDateTime.now()) / 60.0;
            if (diff > 15) heartBeatStatus = -1;
            else if (diff < -5) heartBeatStatus = 1;
            else heartBeatStatus = 0;
        }
        return heartBeatStatus;
    }

    public String getModelsSummaryIndex() throws FileSystemException {
        if (modelsSummaryIndex == null) {
            try (var dir = directory.getChild("summary")) {
                modelsSummaryIndex = getTextFileContent(dir, "models.json");
            }
            IoUtil.close(directory);
        }
        return modelsSummaryIndex;
    }

    public FileObject getModelSummaryImage(String id) throws FileSystemException {
        FileObject result;
        try (var dir = directory.getChild("summary")) {
            result = dir.getChild(id + ".png");
        }
        IoUtil.close(directory);
        return result;
    }

    public FileObject getPredictionImage() throws FileSystemException {
        FileObject result = directory.getChild("visualize.svg");
        IoUtil.close(directory);
        return result;
    }

    public String getConfigFile() throws IOException {
        String result;
        try (var file = directory.getChild("config.py")) {
            result = FileObjectUtils.getContentAsString(file, StandardCharsets.UTF_8);
        }
        IoUtil.close(directory);
        return result;
    }
}
