package meowcat.catlog.service.meow_floorcreep;

import meowcat.catlog.model.meow_floorcreep.ExperimentRecord;

import java.util.List;

public interface MeowService {

    List<meowcat.catlog.model.meow_floorcreep.ExperimentRecord> getRecords();

    ExperimentRecord getRecord(String folderName);
}
