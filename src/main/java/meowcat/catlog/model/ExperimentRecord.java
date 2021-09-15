package meowcat.catlog.model;

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

    public static final String FOLDER_NAME_CUST_PAGES = "custom-pages";
    public static final String FILENAME_CUST_PAGES_MANIFEST_JSON = "manifest.json";
    private final Logger logger = LogManager.getLogger(this);

    private final FileObject directory;

    private Map<String, Object> summary;

    private List<Map<String, Object>> scores;

    private List<String> metrics;

    private Map<String, Object> progress;

    private Integer heartBeatStatus;

    private String modelsSummaryIndex;
    private String hostName;
    private String project;

    public ExperimentRecord(String project, String hostName, FileObject directory) {
        this.project = project;
        this.hostName = hostName;
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

    public List<CustomPage> getCustomPagesNames() throws FileSystemException {
        var pagesDir = directory.getChild(FOLDER_NAME_CUST_PAGES);
        if (pagesDir == null) return null;
        List<CustomPage> ret = new ArrayList<>();
        for (var pageDir : pagesDir.getChildren()) {
            Map<String, Object> manifest;
            try (var maniPath = pageDir.getChild(FILENAME_CUST_PAGES_MANIFEST_JSON)) {
                if (!maniPath.isFile()) continue;
                manifest = IoUtil.readJson(maniPath);
            }
            if (manifest == null) continue;
            ret.add(new CustomPage(pageDir.getName().getBaseName(), manifest));
        }
        return ret;
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

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public CustomPage getCustomPage(String pageId, int page) throws FileSystemException {
        var dir = directory.getChild(FOLDER_NAME_CUST_PAGES).getChild(pageId);
        Map<String, Object> manifest;
        try (var maniFile = dir.getChild(FILENAME_CUST_PAGES_MANIFEST_JSON)) {
            manifest = IoUtil.readJson(maniFile);
        }
        assert manifest != null;
        var customPage = new CustomPage(dir.getName().getBaseName(), manifest);
        customPage.setIthFold(page);
        return customPage;
    }


    public FileObject getAttachment(String customPage, String fold, String name) throws FileSystemException {
        FileObject result;
        var dir = directory.getChild("custom-pages").getChild(customPage);
        if (fold != null) dir = dir.getChild(fold);
        return dir.getChild("attachments").getChild(name);
    }

    public class CustomPage {
        private final String id;
        private final String name;
        private final boolean perFold;
        private int ithFold;

        private CustomPage(String id, String name, boolean perFold) {
            this.id = id;
            this.name = name;
            this.perFold = perFold;
        }

        private CustomPage(String id, Map<String, Object> manifest) {
            this.id = id;
            this.name = (String) manifest.get("title");
            this.perFold = (boolean) manifest.get("per_fold");
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isPerFold() {
            return perFold;
        }

        public Map<String, Object> getRoot() {
            try {
                var folder = directory.getChild(FOLDER_NAME_CUST_PAGES).getChild(id);
                if (perFold) folder = folder.getChild(Integer.toString(ithFold));
                try (var doc = folder.getChild("document.json")) {
                    return IoUtil.readJson(doc);
                }
            } catch (FileSystemException e) {
                return null;
            }
        }

        public int getIthFold() {
            return ithFold;
        }

        public void setIthFold(int ithFold) {
            this.ithFold = ithFold;
        }
    }
}
