package eu.mm.software.photofinder.photosattribute.application.query;

import eu.mm.software.photofinder.photosattribute.domain.AiProvider;

public interface AIProcessor {

    ResponseAiDto describePhoto(byte[] imageData, AiProvider provider);
}