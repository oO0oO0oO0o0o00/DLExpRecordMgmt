package meowcat.catlog.model.meow_floorcreep;

import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import meowcat.catlog.util.IoUtil;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.util.FileObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public Map<String, Object> getSummary() throws FileSystemException {
        if (summary == null) {
            summary = getJsonFileAsMap("summary.json");
            IoUtil.close(directory);
        }
        return summary;
    }

    public List<Map<String, Object>> getScores() throws FileSystemException {
        if (scores == null) {
            try (var resultsPath = directory.getChild("results")) {
                try (var cachePath = resultsPath.getChild("scores.cache.json")) {
                    if (cachePath != null) {
                        scores = IoUtil.readJsonArray(cachePath);
                        assert scores != null;
                    }
                }
            }
        }
        if (scores == null) {
            var numFold = getNumFold();
            scores = new ArrayList<>(numFold);
            for (var i = 0; i < numFold; i++) scores.add(null);
            for (var item : getExistingFolds().entrySet()) {
                try (var file = item.getValue().getChild("scores.json")) {
                    var json = IoUtil.readJsonArray(file);
                    assert json != null;
                    scores.set(item.getKey(), json.get(0));
                }
            }

            if ((boolean) getProgress().get("completed")) {
                try (var cachePath = directory.resolveFile("results/scores.cache.json")) {
                    cachePath.createFile();
                    try (var content = cachePath.getContent()) {
                        try (var os = content.getOutputStream()) {
                            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                                    .writeValue(os, scores);
                        } catch (IOException e) {
                            logger.error(e);
                        }
                    }
                }
            }
            IoUtil.close(directory);
        }
        if (metrics == null && scores.size() > 0) metrics = new ArrayList<>(scores.get(0).keySet());
        return scores;
    }

    public List<String> getMetrics() throws FileSystemException {
        if (metrics == null) getScores();
        return metrics;
    }

    public String getHistory(int ithFold) throws FileSystemException {
        try (var resultsPath = directory.getChild("results")) {
            try (var ithFoldPath = resultsPath.getChild(String.format("%02d", ithFold))) {
                try (var file = ithFoldPath.getChild("history.json")) {
                    var ret = IoUtil.readText(file);
                    IoUtil.close(directory);
                    return ret;
                }
            }
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
                modelsSummaryIndex = getTextFileContent(dir, "models.json");
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

    private Map<Integer, FileObject> getExistingFolds() throws FileSystemException {
        int numFold = getNumFold();
        Map<Integer, FileObject> existingFolds = new HashMap<>(numFold);
        try (var resultsPath = directory.getChild("results")) {
            for (var fold : resultsPath.getChildren()) {
                try {
                    int ithFold = Integer.parseInt(fold.getName().getBaseName());
                    if (ithFold >= 0 && ithFold < numFold)
                        existingFolds.put(ithFold, fold);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return existingFolds;
    }

    private int getNumFold() throws FileSystemException {
        return (int) getSummary().get("num_fold");
    }

    private static String getTextFileContent(FileObject directory, String s) throws FileSystemException {
        try (var file = directory.getChild(s)) {
            return IoUtil.readText(file);
        }
    }

    private Map<String, Object> getJsonFileAsMap(String s) throws FileSystemException {
        try (var file = directory.getChild(s)) {
            return IoUtil.readJson(file);
        }
    }
}
