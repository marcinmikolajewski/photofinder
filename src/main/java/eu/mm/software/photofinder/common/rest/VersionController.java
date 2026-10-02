package eu.mm.software.photofinder.common.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class VersionController {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @GetMapping("/rest/api/v1/version")
    public Map<String, String> getVersionInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("version", buildProperties != null ? buildProperties.getVersion() : "unknown");
        info.put("buildDate", LocalDateTime.now().toString());
        return info;
    }
}
