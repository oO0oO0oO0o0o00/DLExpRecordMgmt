package meowcat.catlog.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@PropertySource("classpath:application-env.properties")
public class ClusterConfig {

    private final Logger logger = LogManager.getLogger(this);

    private static final Map<String, String> SFTP_PFW_PORTS = Map.of(
            "160", "10221",
            "125", "10222",
            "117", "10223"
    );

    private static final Map<String, Project> PROJECTS = Map.of(
            "meow-floorcreep", new Project(
                    "/wzy/meow-floorcreep/experiments",
                    new String[]{"160"}
            ), "miya-helper", new Project(
                    "/wzy/miya-helper/experiments",
                    new String[]{"160"}
            ), "charming-ball", new Project(
                    "/wzy/charming-ball/experiments",
                    new String[]{"125"}
            ), "meow-trpos", new Project(
                    "/wzy/meow-trpos/experiments",
                    new String[]{"125"}
            )
    );

    private static final String SFTP_DIRECT_URL_TEMPLATE = "sftp://omnisky:linux123@";
    private static final String DIRECT_IP_TEMPLATE = "172.21.7.";
    private static final String SFTP_PFW_URL_TEMPLATE = "sftp://omnisky:linux123@localhost:";
    //    private static final String LOCAL_URL_TEMPLATE = "file://home/omnisky";
    private static final String LOCAL_URL_TEMPLATE = "file://home/omnisky/";
    private static final String LOCAL_PC_URL_TEMPLATE = "file://Users/omnisky/";

    @Value("${meowcat.under_port_fw}")
    private boolean underPortFw;

    public List<String> getProjects() {
        return new ArrayList<>(PROJECTS.keySet());
    }

    public Stream<Map.Entry<String, String>> getUrlsForProject(String name) throws MeowException {
        var proj = PROJECTS.get(name);
        if (proj == null) throw new MeowException();
        return Arrays.stream(proj.availableOn)
                .map(key -> new AbstractMap.SimpleEntry<>(key, getDirectoryPath(key, proj.getPath())));
    }

    private @NotNull String getDirectoryPath(String key, String directory) {
//        String prefix;
//        if (underPortFw) prefix = SFTP_PFW_URL_TEMPLATE + SFTP_PFW_PORTS.get(key);
//        else {
//            var ip = DIRECT_IP_TEMPLATE + key;
//            if (isItMe(ip)) prefix = LOCAL_URL_TEMPLATE;
//            else prefix = SFTP_DIRECT_URL_TEMPLATE + ip;
//        }
//        return prefix + directory;
        return (underPortFw ? LOCAL_PC_URL_TEMPLATE : LOCAL_URL_TEMPLATE) + directory;
    }

    private boolean isItMe(String ip) {
        if (ip == null) return false;
        Enumeration<NetworkInterface> ifs = null;
        try {
            ifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logger.warn("cannot get network interfaces to determine if the given address is local", e);
            return false;
        }
        while (ifs.hasMoreElements()) {
            var hosts = ifs.nextElement().getInetAddresses();
            while (hosts.hasMoreElements())
                if (ip.equals(hosts.nextElement().getHostAddress())) return true;
        }
        return false;
    }

    private static class Project {
        private final String path;
        private final String[] availableOn;

        public Project(String path, String[] availableOn) {
            this.path = path;
            this.availableOn = availableOn;
        }

        public String getPath() {
            return path;
        }

        public String[] getAvailableOn() {
            return availableOn;
        }
    }


    public static class MeowException extends Exception {
    }
}
