package eu.mm.software.photofinder.photosattribute.domain.event;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}