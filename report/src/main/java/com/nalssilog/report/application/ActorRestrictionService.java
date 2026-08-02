package com.nalssilog.report.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.domain.ActorRestriction;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.report.repository.ActorRestrictionJpaRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActorRestrictionService {

    private final ActorRestrictionJpaRepository restrictionRepository;

    public void ensureCanPost(ReportActor actor) {
        if (findActive(actor).isPresent()) {
            throw new NalssiLogException(ReportErrorCode.ACTOR_POSTING_RESTRICTED);
        }
    }

    public Optional<ActorRestriction> findActive(ReportActor actor) {
        return restrictionRepository.findActive(actor.type(), actor.actorKey(), Instant.now())
                .stream()
                .findFirst();
    }
}
