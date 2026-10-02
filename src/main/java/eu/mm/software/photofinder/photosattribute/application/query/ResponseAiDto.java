package eu.mm.software.photofinder.photosattribute.application.query;

import eu.mm.software.photofinder.photosattribute.domain.Status;

public record ResponseAiDto(String response, Long totalToken, String model, Status status) {
}