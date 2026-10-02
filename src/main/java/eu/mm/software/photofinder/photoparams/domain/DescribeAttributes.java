package eu.mm.software.photofinder.photoparams.domain;

import java.util.Set;

public interface DescribeAttributes {

    String getPath();

    Set<String> getExtensions();

    String getProvider();

}
