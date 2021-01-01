package meowcat.catlog.service;

import meowcat.catlog.model.ExperimentRecord;

import java.util.List;

public interface MeowService {

    List<String> getRecordsNames();

    ExperimentRecord getRecord(String id);

    String getDetail(String id, int ithFold);
}
