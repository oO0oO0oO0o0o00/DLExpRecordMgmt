package meowcat.catlog.model.meow_floorcreep;

import com.fasterxml.jackson.databind.ObjectMapper;
import meowcat.catlog.util.IoUtil;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.util.FileObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ExperimentRecord implements Serializable {

    private final Logger logger = LogManager.getLogger(this);

    private final FileObject directory;

    private Map<String, Object> summary;

    private List<Map<String, Object>> scores;

    private List<String> metrics;

    private Map<String, Object> progress;

    private Integer heartBeatStatus;

    private String modelsSummaryIndex;
    private String hostName;

    public ExperimentRecord(FileObject directory) {
        this.directory = directory;
    }

    public Map<String, Object> getSummary() {
        if (summary == null) {
            try {
                summary = getJsonFileAsMap("summary.json");
            } catch (FileSystemException e) {
                logger.info("FileSystemException");
            }
            IoUtil.close(directory);
        }
        return summary;
    }

    public List<Map<String, Object>> getScores() {
        if (scores == null) {
            try {
                try (var resultsPath = directory.getChild("results.json")) {
                    if (resultsPath == null) return null;
                    var results = IoUtil.readJson(resultsPath);
                    assert results != null;
                    @SuppressWarnings("unchecked")
                    var rawScores = (List<Object>) results.get("test_scores");
                    //noinspection unchecked
                    scores = rawScores.stream().map(o -> (o instanceof Map) ?
                            (Map<String, Object>) o : null).collect(Collectors.toList());
                }
                if (metrics == null && scores != null && scores.size() > 0)
                    metrics = new ArrayList<>(scores.get(0).keySet());
            } catch (FileSystemException fse) {
                logger.info("FileSystemException");
            } catch (Exception e) {
                logger.warn("cannot get scores", e);
            }
        }
        return scores;
    }

    public List<String> getMetrics() {
        if (metrics == null) getScores();
        return metrics;
    }

    public String getHistory(int ithFold) {
        try {
            try (var resultsPath = directory.getChild("results.json")) {
                if (resultsPath == null) return null;
                var results = IoUtil.readJson(resultsPath);
                assert results != null;
                @SuppressWarnings("unchecked")
                var histories = (List<Object>) results.get("training_history");
                return new ObjectMapper().writeValueAsString(histories.get(ithFold));
            }
        } catch (Exception e) {
            logger.warn("cannot get history of fold " + ithFold, e);
            return null;
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
            try (var dir = directory.getChild("models")) {
                String result;
                try (var file = dir.getChild("models.json")) {
                    result = IoUtil.readText(file);
                }
                modelsSummaryIndex = result;
            }
            IoUtil.close(directory);
        }
        return modelsSummaryIndex;
    }

    public FileObject getModelSummaryImage(String id) throws FileSystemException {
        FileObject result;
        try (var dir = directory.getChild("models")) {
            result = dir.getChild(id + ".png");
        }
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

    public String getLogFile() throws IOException {
        String result;
        try (var file = directory.getChild("log.txt")) {
            result = FileObjectUtils.getContentAsString(file, StandardCharsets.UTF_8);
        }
        IoUtil.close(directory);
        return result;
    }

    public boolean hasWeights() {
        try (var dir = directory.getChild("checkpoints")) {
            return dir != null && dir.isFolder();
        } catch (FileSystemException e) {
            logger.warn("cannot determine hasWeights");
            return false;
        }
    }

    public boolean delete() {
        try {
            directory.deleteAll();
        } catch (FileSystemException e) {
            logger.warn("cannot delete");
            return false;
        }
        return true;
    }

    public boolean deleteWeights() {
        try (var dir = directory.getChild("checkpoints")) {
            dir.deleteAll();
        } catch (FileSystemException e) {
            logger.warn("cannot get weights folder to delete");
            return false;
        }
        return true;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostName() {
        return hostName;
    }

    private Map<String, Object> getJsonFileAsMap(String s) throws FileSystemException {
        try (var file = directory.getChild(s)) {
            return IoUtil.readJson(file);
        }
    }
}
