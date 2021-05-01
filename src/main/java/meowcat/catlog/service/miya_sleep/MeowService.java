package meowcat.catlog.service.miya_sleep;

import meowcat.catlog.model.miya_sleep.ExperimentRecord;

import java.util.List;

public interface MeowService {

    List<ExperimentRecord> getRecords();

    ExperimentRecord getRecord(String folderName);
}
