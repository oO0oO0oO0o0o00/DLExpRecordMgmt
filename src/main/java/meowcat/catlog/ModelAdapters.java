package meowcat.catlog;

import meowcat.catlog.model.ExperimentRecord;

public class ModelAdapters {

    public String meanAndStdToString(ExperimentRecord.MeanAndStd meanAndStd) {
        return String.format("%.2f±%.2f", meanAndStd.getMean(), meanAndStd.getStd());
    }
}
