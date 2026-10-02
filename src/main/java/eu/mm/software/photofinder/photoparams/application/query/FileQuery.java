package eu.mm.software.photofinder.photoparams.application.query;

import java.util.Set;


public interface FileQuery {

    Set<String> listAllFilesFromPath(String path, Set<String> extension);
    Set<String> findAllByLoggedUser();
}
