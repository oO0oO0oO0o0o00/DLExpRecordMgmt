package meowcat.catlog.config;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.springframework.stereotype.Component;

@Component
public class VFSOptions {

    private final FileSystemOptions fileSystemOptions;

    public VFSOptions() {
        this.fileSystemOptions =  new FileSystemOptions();
        SftpFileSystemConfigBuilder sftpFileSystemConfigBuilder = SftpFileSystemConfigBuilder.getInstance();
        try {
            sftpFileSystemConfigBuilder.setIdentityProvider(fileSystemOptions, jSch -> {
            });
        } catch (FileSystemException e) {
            e.printStackTrace();
        }
    }

    public FileSystemOptions getFileSystemOptions() {
        return fileSystemOptions;
    }
}
