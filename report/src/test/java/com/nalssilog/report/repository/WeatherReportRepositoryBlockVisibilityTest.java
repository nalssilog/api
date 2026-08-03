package com.nalssilog.report.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.domain.QWeatherReport;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
class WeatherReportRepositoryBlockVisibilityTest {

    private final WeatherReportRepository repository = new WeatherReportRepository(
            mock(WeatherReportJpaRepository.class),
            mock(JPAQueryFactory.class),
            mock(EntityManager.class));

    @Test
    void memberFeedPredicateExcludesBlocksInBothDirections() {
        BooleanExpression predicate = repository.withoutBlockRelation(
                ReportActor.member(8L));
        String query = new JPAQuery<Void>()
                .from(QWeatherReport.weatherReport)
                .where(predicate)
                .toString();

        assertThat(query)
                .contains("where not exists")
                .contains("actorBlock.blockerType = ?1 and actorBlock.blockerKey = ?2")
                .contains("actorBlock.blockedType = weatherReport.authorType")
                .contains("or actorBlock.blockerType = weatherReport.authorType")
                .contains("actorBlock.blockedType = ?5 and actorBlock.blockedKey = ?6");
    }

    @Test
    void anonymousViewerHasNoMemberBlockRelation() {
        assertThat(repository.withoutBlockRelation(
                ReportActor.anonymous("anonymous-viewer")))
                .isNull();
    }
}
