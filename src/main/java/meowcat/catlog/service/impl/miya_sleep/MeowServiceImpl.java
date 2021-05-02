package meowcat.catlog.service.impl.miya_sleep;

import meowcat.catlog.config.VFSOptions;
import meowcat.catlog.model.miya_sleep.ExperimentRecord;
import meowcat.catlog.service.miya_sleep.MeowService;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service("miya-sleep-MeowService")
public class MeowServiceImpl implements MeowService {

    private static final String RESULTS_PATH = "sftp://omnisky:linux123@172.21.7.117/SLEEP-DATA/maomao/results";

    private final Logger logger = LogManager.getLogger(this);

    @Value("${meowcat.catlog.ssh.known_hosts_dir}")
    private String knownHostsDir;

    @Autowired
    private VFSOptions vfsOptions;

//    @Autowired
//    private SftpService sftpService;

    @Override
    public List<ExperimentRecord> getRecords() {
        var resultsDir = getResultsDir();
        if (resultsDir == null) return null;
        try {
            return Arrays.stream(resultsDir.getChildren())
                    .filter(fileObject -> {
                        try {
                            return fileObject.isFolder();
                        } catch (FileSystemException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(ExperimentRecord::new)
                    .sorted(Comparator.comparing(ExperimentRecord::getFolderName))
                    .collect(Collectors.toList());
        } catch (FileSystemException | RuntimeException e) {
            logger.warn("Cannot get experiment records list of result directory.", e);
            return null;
        }
    }

    private FileObject getResultsDir() {
        try {
            return VFS.getManager().resolveFile(RESULTS_PATH,
                    vfsOptions.getFileSystemOptions());
        } catch (FileSystemException e) {
            logger.warn("Cannot get result directory as FileObject.", e);
            return null;
        }
    }

    @Override
    public ExperimentRecord getRecord(String folderName) {
        FileObject resultsDir = getResultsDir();
        if (resultsDir == null) return null;
        try {
            return new ExperimentRecord(resultsDir.getChild(folderName));
        } catch (FileSystemException e) {
            logger.info("Cannot get experiment record of given name from result directory");
            return null;
        }
    }
}
