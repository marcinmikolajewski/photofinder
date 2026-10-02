package eu.mm.software.photofinder.photosattribute.infrastructure.rabbit;

import java.util.Map;

public record DocumentMessage(
        String id,
        String text,
        Map<String, Object> metadata
) {}