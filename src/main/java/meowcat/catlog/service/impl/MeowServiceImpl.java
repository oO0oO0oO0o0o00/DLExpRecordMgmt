package meowcat.catlog.service.impl;

import meowcat.catlog.config.ClusterConfig;
import meowcat.catlog.config.VFSOptions;
import meowcat.catlog.model.ExperimentRecord;
import meowcat.catlog.service.MeowService;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("meow-MeowService")
public class MeowServiceImpl implements MeowService {

    //    private static final String RESULTS_PATH = "/wzy/meow-floorcreep/experiments";
    private static final String RESULTS_PATH = "/wzy/miya-helper/experiments";

    private static final String[] RESULTS_HOST_NAMES = {
            /*"117", */"160"/*, "125"*/
    };
    public static final String ERR_STRING_CANNOT_GET_RESULTS_OF_DIR = "Cannot get experiment records list of result directory.";

    private final Logger logger = LogManager.getLogger(this);

    @Autowired
    private ClusterConfig clusterConfig;

    @Autowired
    private VFSOptions vfsOptions;

    @Override
    public List<ExperimentRecord> getRecords(String project) throws ClusterConfig.MeowException {
        AtomicBoolean hasError = new AtomicBoolean(false);
        List<ExperimentRecord> list = getResultsPathsOf(project).map(kv -> {
                    try {
                        if (!kv.getValue().isFolder()) return null;
                        return Arrays.stream(kv.getValue().getChildren())
                                .map(v -> new AbstractMap.SimpleEntry<>(kv.getKey(), v));
                    } catch (FileSystemException | NullPointerException e) {
                        hasError.set(true);
                        return null;
                    }
                }).filter(Objects::nonNull).flatMap(Function.identity())
                .map(kv -> new ExperimentRecord(project, kv.getKey(), kv.getValue()))
                .sorted(Comparator.comparing(ExperimentRecord::getFolderName))
                .collect(Collectors.toList());
        if (hasError.get()) list.add(null);
        return list;
    }

    @Override
    public ExperimentRecord getRecord(String project, String folderName) throws ClusterConfig.MeowException {
        return getResultsPathsOf(project).map(kv -> {
                    try {
                        return new AbstractMap.SimpleEntry<>(kv.getKey(), kv.getValue().getChild(folderName));
                    } catch (FileSystemException | NullPointerException e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .map(kv -> new ExperimentRecord(project, kv.getKey(), kv.getValue()))
                .findAny().orElse(null);
    }

    private Stream<Map.Entry<String, FileObject>> getResultsPathsOf(String project) throws ClusterConfig.MeowException {
        return clusterConfig.getUrlsForProject(project)
                .map(kv -> {
                    try {
                        return new AbstractMap.SimpleEntry<>(
                                kv.getKey(), VFS.getManager().resolveFile(kv.getValue(),
                                vfsOptions.getFileSystemOptions()));
                    } catch (FileSystemException e) {
                        return null;
                    }
                });
    }
}
