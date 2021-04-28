package meowcat.catlog.service.impl.meow_bonn_metric;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import meowcat.catlog.model.meow_bonn_metric.ExperimentRecord;
import meowcat.catlog.service.meow_bonn_metric.MeowService;
import meowcat.catlog.util.IoUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.FileUtils;

@Service("MeowService")
public class MeowServiceImpl implements MeowService {
    private static final String RESULTS_PATH = "/home/omnisky/wzy/bonn_metric_exp/results";
    private static final String RESULTS_PATH_TEST = "D:\\Temp\\bonnexptestresults";
    private static final String MANIFEST_FILENAME = "manifest.json";

    @Override
    public List<ExperimentRecord> getRecordsBrief() {
        File dir = getDir();
        String[] list = dir.list();
        if (list == null) return null;
        Arrays.sort(list);
        var results = new ArrayList<ExperimentRecord>();
        for (var filename : list) {
            File recordDir = new File(dir, filename);
            File manifest = new File(recordDir, MANIFEST_FILENAME);
            if (manifest.isFile())
                results.add(ExperimentRecord.fromJson(filename, Objects.requireNonNull(IoUtil.readJson(manifest))));
        }
        results.sort(Comparator.comparing(ExperimentRecord::getId));
        return results;
    }

    @NotNull
    private File getDir() {
        File dir = new File(RESULTS_PATH);
        if (!dir.exists()) dir = new File(RESULTS_PATH_TEST);
        return dir;
    }

    @Override
    public ExperimentRecord getRecord(String id) {
        File dir = new File(getDir(), id);
        File manifest = new File(dir, MANIFEST_FILENAME);
        ExperimentRecord record = ExperimentRecord.fromJson(id, Objects.requireNonNull(IoUtil.readJson(manifest)));
        if (dir.listFiles((file) -> file.isDirectory() && file.getName().startsWith("weights-")).length > 0)
            record.setHasSavedWeights(true);
        List<JsonObject> jsonObjects = new ArrayList<>(record.getFolds());
        for (int i = 0; i < record.getFolds(); i++) {
            File file = new File(dir, i + "_metrics.json");
            JsonValue jsonValue;
            if (file.isFile() && (jsonValue = IoUtil.readJson(file)) != null)
                jsonObjects.add(jsonValue.asObject());
            else
                jsonObjects.add(null);
        }
        record.setListOfFinalScoresFromJson(jsonObjects);
        return record;
    }

    @Override
    public String getDetail(String id, int ithFold) {
        return Objects.requireNonNull(IoUtil.readJson(new File(new File(getDir(), id), ithFold + "_results.json"))).toString();
    }

    @Override
    public boolean deleteWeights(String id) {
        File dir = new File(getDir(), id);
        for (var file : dir.listFiles((file) -> file.isDirectory() && file.getName().startsWith("weights-"))) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
