package meowcat.catlog.service;

import meowcat.catlog.config.ClusterConfig;
import meowcat.catlog.model.ExperimentRecord;

import java.util.List;

public interface MeowService {

    List<String> getProjects();

    List<ExperimentRecord> getRecords(String project) throws ClusterConfig.MeowException;

    ExperimentRecord getRecord(String project, String folderName) throws ClusterConfig.MeowException;
}
