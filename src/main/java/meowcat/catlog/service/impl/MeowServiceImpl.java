package meowcat.catlog.service.impl;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import meowcat.catlog.model.ExperimentRecord;
import meowcat.catlog.service.MeowService;
import meowcat.catlog.util.IoUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service("MeowService")
public class MeowServiceImpl implements MeowService {
    private static final String RESULTS_PATH = "/home/omnisky/wzy/bonn_metric_exp/results";
    private static final String RESULTS_PATH_TEST = "D:\\Temp\\bonnexptestresults";
    private static final String MANIFEST_FILENAME = "manifest.json";

    @Override
    public List<String> getRecordsNames() {
        File dir = getDir();
        String[] list = dir.list((self, filename) -> new File(new File(self, filename), MANIFEST_FILENAME).isFile());
        if (list == null) return null;
        Arrays.sort(list);
        return Arrays.asList(list);
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
}
