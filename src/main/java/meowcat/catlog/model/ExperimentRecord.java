package meowcat.catlog.model;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import meowcat.catlog.util.MathUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class ExperimentRecord implements Serializable {

    private String id;

    private String name;

    private int folds;

    private int repeats;

    private List<String> metrics;

    private List<Map<String, MeanAndStd>> listOfFinalScores;

    private Map<String, MeanAndStd> averagedFinalScores;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getFolds() {
        return folds;
    }

    public void setFolds(int folds) {
        this.folds = folds;
    }

    public int getRepeats() {
        return repeats;
    }

    public ExperimentRecord(String id) {
        this.id = id;
    }

    public void setRepeats(int repeats) {
        this.repeats = repeats;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    private void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    public List<Map<String, MeanAndStd>> getListOfFinalScores() {
        return listOfFinalScores;
    }

    private void setListOfFinalScores(List<Map<String, MeanAndStd>> listOfFinalScores) {
        this.listOfFinalScores = listOfFinalScores;
        List<String> metrics = new ArrayList<>();
        for (Map<String, MeanAndStd> finalScores : listOfFinalScores)
            if (finalScores != null) {
                metrics.addAll(finalScores.keySet());
                break;
            }
        setMetrics(metrics);
        Map<String, MeanAndStd> averagedFinalScores = new Hashtable<>();
        for (String metric : metrics) {
//            float[] scores = new float[folds];
            var scores = new ArrayList<Float>();
            for (int i = 0; i < folds; i++) {
                var map = listOfFinalScores.get(i);
                if (map != null)
                    scores.add(map.get(metric).mean);
            }
            float mean = MathUtil.mean(scores);
            float std = MathUtil.std(scores, mean);
            averagedFinalScores.put(metric, new MeanAndStd(mean, std));
        }
        setAveragedFinalScores(averagedFinalScores);
    }

    public void setListOfFinalScoresFromJson(List<JsonObject> listOfFinalScoresInJson) {
        List<Map<String, MeanAndStd>> listOfFinalScores = new ArrayList<>();
        for (JsonObject finalScoresInJson : listOfFinalScoresInJson) {
            if (finalScoresInJson == null) {
                listOfFinalScores.add(null);
                continue;
            }
            Map<String, MeanAndStd> finalScores = new Hashtable<>();
            for (JsonObject.Member metricInJson : finalScoresInJson) {
                JsonObject metric = metricInJson.getValue().asObject();
                finalScores.put(metricInJson.getName(),
                        new MeanAndStd(metric.get("mean").asFloat(), metric.get("std").asFloat()));
            }
            listOfFinalScores.add(finalScores);
        }
        setListOfFinalScores(listOfFinalScores);
    }

    public Map<String, MeanAndStd> getAveragedFinalScores() {
        return averagedFinalScores;
    }

    private void setAveragedFinalScores(Map<String, MeanAndStd> averagedFinalScores) {
        this.averagedFinalScores = averagedFinalScores;
    }

    @NotNull
    public static ExperimentRecord fromJson(String id, JsonValue json) {
        ExperimentRecord record = new ExperimentRecord(id);
        JsonObject jsonObject = json.asObject();
        JsonValue name = jsonObject.get("name");
        if (name != null && !name.isNull()) record.name = name.asString();
        record.folds = jsonObject.get("k-fold").asInt();
        record.repeats = jsonObject.get("repeats").asInt();
        return record;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static class MeanAndStd implements Serializable {
        private float mean;
        private float std;

        public float getMean() {
            return mean;
        }

        public void setMean(float mean) {
            this.mean = mean;
        }

        public float getStd() {
            return std;
        }

        public void setStd(float std) {
            this.std = std;
        }

        public MeanAndStd(float mean, float std) {
            this.mean = mean;
            this.std = std;
        }
    }
}
