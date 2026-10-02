package eu.mm.software.photofinder.photosattribute.infrastructure;

import eu.mm.software.photofinder.photosattribute.domain.PhotoParamsDto;

public interface PhotosSender {

    void sendMessage(String queue, PhotoParamsDto photoParamsDto, int priority);
}
