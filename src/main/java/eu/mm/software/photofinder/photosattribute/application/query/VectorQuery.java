package eu.mm.software.photofinder.photosattribute.application.query;

import java.util.List;

public interface VectorQuery {

    List<String> search(String prompt,
                        String provider,
                        String model,
                        String path,
                        Integer limit);
}
