package meowcat.catlog.service.impl.miya_sleep;

import meowcat.catlog.config.VFSOptions;
import meowcat.catlog.model.miya_sleep.ExperimentRecord;
import meowcat.catlog.service.miya_sleep.MeowService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("miya-sleep-MeowService")
public class MeowServiceImpl implements MeowService {

    private static final String[] RESULTS_PATHS = {
            "sftp://omnisky:linux123@172.21.7.117/SLEEP-DATA/maomao/results",
//            "file://home/omnisky/wzy/miya-sleep/results"
            "sftp://omnisky:linux123@172.21.7.125/wzy/miya-sleep/results"
    };

    private static final String[] RESULTS_HOST_NAMES = {
            "117", "125"
    };
    public static final String ERR_STRING_CANNOT_GET_RESULTS_OF_DIR = "Cannot get experiment records list of result directory.";

    private final Iterable<Pair<String, FileObject>> resultsPathIterable;

    private final Logger logger = LogManager.getLogger(this);

    @Autowired
    private VFSOptions vfsOptions;

    public MeowServiceImpl() {
        this.resultsPathIterable = () -> new Iterator<>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < RESULTS_PATHS.length;
            }

            @Override
            public Pair<String, FileObject> next() {
                ImmutablePair<String, FileObject> ret;
                try {
                    ret = new ImmutablePair<>(RESULTS_HOST_NAMES[index],
                            VFS.getManager().resolveFile(RESULTS_PATHS[index],
                                    vfsOptions.getFileSystemOptions()));
                } catch (FileSystemException e) {
                    logger.warn("Cannot get result directory as FileObject.", e);
                    ret = null;
                }
                index++;
                return ret;
            }
        };
    }

    @Override
    public List<ExperimentRecord> getRecords() {
        Stream<ExperimentRecord> results = Stream.empty();
        for (var resultsDir : resultsPathIterable) {
            try {
                var hostName = resultsDir.getLeft();
                results = Stream.concat(results, Arrays.stream(resultsDir.getRight().getChildren())
                        .filter(fileObject -> {
                            try {
                                return fileObject.isFolder();
                            } catch (FileSystemException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .map(ExperimentRecord::new)
                        .peek(experimentRecord -> experimentRecord.setHostName(hostName)));
            } catch (FileSystemException | RuntimeException e) {
                logger.warn(ERR_STRING_CANNOT_GET_RESULTS_OF_DIR, e);
            }
        }
        return results.sorted(Comparator.comparing(ExperimentRecord::getFolderName))
                .collect(Collectors.toList());
    }

    @Override
    public ExperimentRecord getRecord(String folderName) {
        for (var resultsDir : resultsPathIterable) {
            if (resultsDir == null) {
                logger.warn(ERR_STRING_CANNOT_GET_RESULTS_OF_DIR);
                continue;
            }
            try (var dir = resultsDir.getRight().getChild(folderName)) {
                if (dir != null) return new ExperimentRecord(dir);
            } catch (FileSystemException e) {
                logger.info("Cannot get experiment record of given name from result directory");
            }
        }
        return null;
    }
}
