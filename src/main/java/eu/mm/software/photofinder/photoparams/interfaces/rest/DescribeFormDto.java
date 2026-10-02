package eu.mm.software.photofinder.photoparams.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.mm.software.photofinder.photoparams.domain.DescribeAttributes;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DescribeFormDto implements DescribeAttributes {

    private String path;
    private Set<String> extensions = new HashSet<>();
    private String provider;

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Set<String> getExtensions() {
        return extensions;
    }

    @Override
    public String getProvider() {
        return provider;
    }
}