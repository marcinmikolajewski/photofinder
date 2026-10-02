package eu.mm.software.photofinder.worker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@Getter
public class WorkerNodeInfo {

    private final String nodeLabel;

    public WorkerNodeInfo(@Value("${worker.node.name:}") String configuredName) {
        String name = configuredName.isBlank() ? detectHostname() : configuredName;
        String cpu = detectCpuInfo();
        this.nodeLabel = name + " | " + cpu;
        log.info("Worker node identified as: {}", nodeLabel);
    }

    private String detectHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }

    private String detectCpuInfo() {
        try {
            Path cpuInfo = Path.of("/proc/cpuinfo");
            if (Files.exists(cpuInfo)) {
                return Files.lines(cpuInfo)
                        .filter(l -> l.startsWith("model name"))
                        .findFirst()
                        .map(l -> l.contains(":") ? l.split(":", 2)[1].strip() : l)
                        .orElse(fallbackCpuInfo());
            }
        } catch (IOException ignored) {}
        return fallbackCpuInfo();
    }

    private String fallbackCpuInfo() {
        return System.getProperty("os.arch") + " x" + Runtime.getRuntime().availableProcessors() + " cores";
    }
}
