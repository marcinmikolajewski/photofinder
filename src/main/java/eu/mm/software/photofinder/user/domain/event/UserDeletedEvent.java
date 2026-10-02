package eu.mm.software.photofinder.user.domain.event;

import eu.mm.software.photofinder.photosattribute.domain.event.DomainEvent;

public record UserDeletedEvent(
        String userId
) implements DomainEvent {}