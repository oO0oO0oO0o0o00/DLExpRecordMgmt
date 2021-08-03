package meowcat.catlog.config;

import meowcat.catlog.util.IoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;

@Component
@PropertySource("classpath:application-env.properties")
public class ClusterConfig {

    private final Logger logger = LogManager.getLogger(this);

    private static final Map<String, String> SFTP_PFW_PORTS = Map.of(
            "160", "10221",
            "125", "10222",
            "117", "10223"
    );

    private static final String SFTP_DIRECT_URL_TEMPLATE = "sftp://omnisky:linux123@";
    private static final String DIRECT_IP_TEMPLATE = "172.21.7.";
    private static final String SFTP_PFW_URL_TEMPLATE = "sftp://omnisky:linux123@localhost:";
    private static final String LOCAL_URL_TEMPLATE = "file://home/omnisky";

    @Value("${meowcat.under_port_fw}")
    private boolean underPortFw;

    public String getDirectoryPath(String key, String directory) {
        String prefix;
        if (underPortFw) prefix = SFTP_PFW_URL_TEMPLATE + SFTP_PFW_PORTS.get(key);
        else {
            var ip = DIRECT_IP_TEMPLATE + key;
            if (isItMe(ip)) prefix = LOCAL_URL_TEMPLATE;
            else prefix = SFTP_DIRECT_URL_TEMPLATE + ip;
        }
        return prefix + directory;
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
}
