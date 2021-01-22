package meowcat.catlog.service;

import meowcat.catlog.model.ExperimentRecord;

import java.util.List;

public interface MeowService {

    List<ExperimentRecord> getRecordsBrief();

    ExperimentRecord getRecord(String id);

    String getDetail(String id, int ithFold);
}
