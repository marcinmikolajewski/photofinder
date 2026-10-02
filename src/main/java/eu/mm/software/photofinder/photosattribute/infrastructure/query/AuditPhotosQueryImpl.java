package eu.mm.software.photofinder.photosattribute.infrastructure.query;

import eu.mm.software.photofinder.photosattribute.application.query.AuditPhotoDto;
import eu.mm.software.photofinder.photosattribute.application.query.AuditPhotosQuery;
import eu.mm.software.photofinder.photosattribute.application.query.AuditSummaryDto;
import eu.mm.software.photofinder.photosattribute.domain.AuditPhotos;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttribute;
import eu.mm.software.photofinder.photosattribute.domain.AiProvider;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.AuditPhotoSpringDataRepository;
import eu.mm.software.photofinder.photosattribute.infrastructure.mongo.PhotoAttributeSpringDataRepository;
import eu.mm.software.photofinder.photosattribute.application.query.AuditPhotoStatsDto;
import eu.mm.software.photofinder.user.application.query.UserQuery;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class AuditPhotosQueryImpl implements AuditPhotosQuery {

    private final AuditPhotoSpringDataRepository auditPhotoSpringDataRepository;
    private final PhotoAttributeSpringDataRepository photoAttributeSpringDataRepository;
    private final UserQuery userQuery;
    private final MongoTemplate mongoTemplate;

    @Override
    public Set<AuditPhotoDto> findByLoggedUser() {
        return auditPhotoSpringDataRepository.findAllByUserId(userQuery.findLoggedUserId()).stream()
                .map(this::toDto)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<AuditPhotoDto> findByUserEmailAndProvider(AiProvider provider) {
        return auditPhotoSpringDataRepository.findAllByUserIdAndProvider(
                        userQuery.findLoggedUserId(), provider.name()).stream()
                .map(this::toDto)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, AuditPhotoStatsDto> getStats() {

        Map<String, AuditPhotoStatsDto> map = new LinkedHashMap<>();

        Set<AuditPhotos> allAuditPhotos = auditPhotoSpringDataRepository.findAllByUserId(userQuery.findLoggedUserId());
        map.put("ALL", createStatsDto(allAuditPhotos));

        Arrays.stream(AiProvider.values()).forEach(it ->
                map.put(it.name(), createStatsDto(allAuditPhotos.stream()
                        .filter(ap -> it.name().equals(ap.getProvider()))
                        .collect(Collectors.toSet()))));

        return map.entrySet().stream()
                .sorted(Comparator.comparing(it -> it.getValue().numberPhotos()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    @Override
    public Set<String> findIdPhotosWithEmptyDescription() {

        Set<String> emptyDescriptionsIds = photoAttributeSpringDataRepository.findAllByUserId(
                        userQuery.findLoggedUserId(), Pageable.unpaged()).stream()
                .filter(it -> StringUtils.isEmpty(it.getPhotoDescription()))
                .map(PhotoAttribute::getId)
                .collect(Collectors.toSet());

        return auditPhotoSpringDataRepository.findAllByUserId(userQuery.findLoggedUserId()).stream()
                .filter(it -> emptyDescriptionsIds.contains(it.getPhotoAttributeId()))
                .map(AuditPhotos::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<AuditPhotoDto> findByUserId(String userId) {
        return auditPhotoSpringDataRepository.findAllByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditSummaryDto> summarizeByProvider(String userId) {
        MatchOperation match = Aggregation.match(
                Criteria.where("userId").is(userQuery.findLoggedUserId()));

        GroupOperation group = Aggregation.group("provider")
                .count().as("totalPhotos")
                .sum("totalTokens").as("totalTokens")
                .avg("processingTimeMs").as("avgProcessingMs");

        SortOperation sort = Aggregation.sort(
                org.springframework.data.domain.Sort.by("totalPhotos").descending());

        Aggregation aggregation = Aggregation.newAggregation(match, group, sort);

        return mongoTemplate.aggregate(
                        aggregation, AuditPhotos.COLLECTION, AuditSummaryDto.class)
                .getMappedResults();
    }

    private AuditPhotoStatsDto createStatsDto(Set<AuditPhotos> auditPhotos) {
        long allTokens = auditPhotos.stream()
                .mapToLong(it -> it.getTotalTokens() != null ? it.getTotalTokens() : 0L)
                .sum();

        long totalMs = auditPhotos.stream()
                .mapToLong(it -> it.getProcessingTimeMs() != null ? it.getProcessingTimeMs() : 0L)
                .sum();

        Duration duration = Duration.of(totalMs, ChronoUnit.MILLIS);
        int totalPhotos = auditPhotos.size();
        Duration avgDuration = totalPhotos != 0 ? duration.dividedBy(totalPhotos) : Duration.ZERO;

        return new AuditPhotoStatsDto(totalPhotos,
                allTokens,
                duration,
                totalPhotos != 0 ? (double) allTokens / totalPhotos : 0,
                avgDuration);
    }

    private AuditPhotoDto toDto(AuditPhotos audit) {
        AuditPhotoDto dto = new AuditPhotoDto();
        dto.setId(audit.getId());
        dto.setPhotoAttributeId(audit.getPhotoAttributeId());
        dto.setProvider(audit.getProvider());
        dto.setModel(audit.getModel());
        dto.setTotalTokens(audit.getTotalTokens());
        dto.setProcessingTimeMs(audit.getProcessingTimeMs());
        dto.setStatus(audit.getStatus());
        dto.setWorkerNode(audit.getWorkerNode());
        dto.setOccurredAt(audit.getOccurredAt());
        return dto;
    }
}
