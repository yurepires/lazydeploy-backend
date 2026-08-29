package com.yurepires.lazydeploy.repository;

import com.yurepires.lazydeploy.entity.NotificationDeliveryAttemptEntity;
import com.yurepires.lazydeploy.entity.ServerSubscriptionEntity;
import com.yurepires.lazydeploy.mapper.NotificationHistoryMapper;
import com.yurepires.lazydeploy.model.notification.NotificationHistoryEntry;
import com.yurepires.lazydeploy.model.notification.NotificationHistoryFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaNotificationHistoryRepository implements NotificationHistoryRepository {

    private final EntityManager entityManager;
    private final NotificationHistoryMapper historyMapper;

    public JpaNotificationHistoryRepository(
            EntityManager entityManager,
            NotificationHistoryMapper historyMapper
    ) {
        this.entityManager = entityManager;
        this.historyMapper = historyMapper;
    }

    @Override
    public Page<NotificationHistoryEntry> findHistory(
            UUID userId,
            NotificationHistoryFilter filter,
            Pageable pageable
    ) {
        NotificationHistoryFilter effectiveFilter = emptyFilterWhenNull(filter);
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<NotificationDeliveryAttemptEntity> query = criteriaBuilder
                .createQuery(NotificationDeliveryAttemptEntity.class);
        Root<NotificationDeliveryAttemptEntity> attempt = query.from(NotificationDeliveryAttemptEntity.class);
        query.select(attempt);

        query.where(buildPredicates(criteriaBuilder, query, attempt, userId, effectiveFilter));
        applyOrder(criteriaBuilder, query, attempt, pageable.getSort());

        TypedQuery<NotificationDeliveryAttemptEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<NotificationHistoryEntry> content = typedQuery.getResultList()
                .stream()
                .map(historyMapper::toDomain)
                .toList();

        long total = count(userId, effectiveFilter, criteriaBuilder);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Optional<NotificationHistoryEntry> findByIdAndUserId(UUID notificationId, UUID userId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<NotificationDeliveryAttemptEntity> query = criteriaBuilder
                .createQuery(NotificationDeliveryAttemptEntity.class);
        Root<NotificationDeliveryAttemptEntity> attempt = query.from(NotificationDeliveryAttemptEntity.class);
        query.select(attempt);
        query.where(
                criteriaBuilder.and(
                        criteriaBuilder.equal(attempt.get("id"), notificationId),
                        ownerPredicate(criteriaBuilder, query, attempt, userId)
                )
        );

        return entityManager.createQuery(query)
                .getResultStream()
                .findFirst()
                .map(historyMapper::toDomain);
    }

    private long count(
            UUID userId,
            NotificationHistoryFilter filter,
            CriteriaBuilder criteriaBuilder
    ) {
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<NotificationDeliveryAttemptEntity> attempt = countQuery
                .from(NotificationDeliveryAttemptEntity.class);
        countQuery.select(criteriaBuilder.count(attempt));
        countQuery.where(buildPredicates(criteriaBuilder, countQuery, attempt, userId, filter));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private NotificationHistoryFilter emptyFilterWhenNull(NotificationHistoryFilter filter) {
        if (filter != null) {
            return filter;
        }
        return new NotificationHistoryFilter(null, null, null, null, null, null, null);
    }

    private List<Predicate> buildPredicates(
            CriteriaBuilder criteriaBuilder,
            CriteriaQuery<?> query,
            Root<NotificationDeliveryAttemptEntity> attempt,
            UUID userId,
            NotificationHistoryFilter filter
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(ownerPredicate(criteriaBuilder, query, attempt, userId));

        if (filter.subscriptionId() != null) {
            predicates.add(criteriaBuilder.equal(
                    attempt.get("subscriptionId"),
                    filter.subscriptionId()
            ));
        }
        if (filter.serverId() != null) {
            predicates.add(criteriaBuilder.equal(attempt.get("serverId"), filter.serverId()));
        }
        if (filter.status() != null) {
            predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.upper(attempt.get("status")),
                    filter.status().name()
            ));
        }
        if (filter.channelType() != null && !filter.channelType().isBlank()) {
            predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(attempt.get("channelType")),
                    filter.channelType().toLowerCase(Locale.ROOT)
            ));
        }
        if (filter.mapId() != null && !filter.mapId().isBlank()) {
            predicates.add(criteriaBuilder.equal(attempt.get("mapId"), filter.mapId()));
        }
        if (filter.from() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    attempt.get("attemptedAt"),
                    filter.from()
            ));
        }
        if (filter.to() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    attempt.get("attemptedAt"),
                    filter.to()
            ));
        }

        return predicates;
    }

    private Predicate ownerPredicate(
            CriteriaBuilder criteriaBuilder,
            CriteriaQuery<?> query,
            Root<NotificationDeliveryAttemptEntity> attempt,
            UUID userId
    ) {
        Subquery<UUID> ownedSubscription = query.subquery(UUID.class);
        Root<ServerSubscriptionEntity> subscription = ownedSubscription
                .from(ServerSubscriptionEntity.class);
        ownedSubscription.select(subscription.get("id"));
        ownedSubscription.where(
                criteriaBuilder.and(
                        criteriaBuilder.equal(subscription.get("id"), attempt.get("subscriptionId")),
                        criteriaBuilder.equal(subscription.get("userId"), userId)
                )
        );

        return criteriaBuilder.or(
                criteriaBuilder.equal(attempt.get("ownerUserId"), userId),
                criteriaBuilder.exists(ownedSubscription)
        );
    }

    private void applyOrder(
            CriteriaBuilder criteriaBuilder,
            CriteriaQuery<NotificationDeliveryAttemptEntity> query,
            Root<NotificationDeliveryAttemptEntity> attempt,
            Sort sort
    ) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            String property = resolveSortProperty(order.getProperty());
            if (order.isAscending()) {
                orders.add(criteriaBuilder.asc(attempt.get(property)));
            } else {
                orders.add(criteriaBuilder.desc(attempt.get(property)));
            }
        }

        if (orders.isEmpty()) {
            orders.add(criteriaBuilder.desc(attempt.get("attemptedAt")));
        }
        orders.add(criteriaBuilder.desc(attempt.get("id")));
        query.orderBy(orders);
    }

    private String resolveSortProperty(String property) {
        if ("channel".equals(property)) {
            return "channelType";
        }
        return property;
    }
}
