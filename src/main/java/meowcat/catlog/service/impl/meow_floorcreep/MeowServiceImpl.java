package meowcat.catlog.service.impl.meow_floorcreep;

import meowcat.catlog.config.ClusterConfig;
import meowcat.catlog.config.VFSOptions;
import meowcat.catlog.model.meow_floorcreep.ExperimentRecord;
import meowcat.catlog.service.meow_floorcreep.MeowService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("meow-floorcreep-MeowService")
public class MeowServiceImpl implements MeowService {

    private static final String RESULTS_PATH = "/wzy/meow-floorcreep/experiments";

    private static final String[] RESULTS_HOST_NAMES = {
            /*"117", */"160"/*, "125"*/
    };
    public static final String ERR_STRING_CANNOT_GET_RESULTS_OF_DIR = "Cannot get experiment records list of result directory.";

    private final Iterable<Pair<String, FileObject>> resultsPathIterable;

    private final String[] RESULTS_URLS;

    private final Logger logger = LogManager.getLogger(this);

    @Autowired
    private VFSOptions vfsOptions;

    @Autowired
    public MeowServiceImpl(ClusterConfig clusterConfig) {

        RESULTS_URLS = Arrays.stream(RESULTS_HOST_NAMES)
                .map(name -> clusterConfig.getDirectoryPath(name, RESULTS_PATH)).toArray(String[]::new);

        this.resultsPathIterable = () -> new Iterator<>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < RESULTS_HOST_NAMES.length;
            }

            @Override
            public Pair<String, FileObject> next() {
                ImmutablePair<String, FileObject> ret;
                try {
                    ret = new ImmutablePair<>(RESULTS_HOST_NAMES[index],
                            VFS.getManager().resolveFile(RESULTS_URLS[index],
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
