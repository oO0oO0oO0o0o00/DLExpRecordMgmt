package meowcat.catlog.service.meow_bonn_metric;

import meowcat.catlog.model.meow_bonn_metric.ExperimentRecord;

import java.util.List;

public interface MeowService {

    List<ExperimentRecord> getRecordsBrief();

    ExperimentRecord getRecord(String id);

    String getDetail(String id, int ithFold);

    boolean deleteWeights(String id);
}
