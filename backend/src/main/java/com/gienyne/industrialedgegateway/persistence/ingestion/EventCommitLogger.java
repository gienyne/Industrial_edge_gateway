package com.gienyne.industrialedgegateway.persistence.ingestion;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;



/**
 * Temporary listener that proves events are only announced after COMMIT.
 */
@Component
class EventCommitLogger{

    private static final Logger log = LoggerFactory.getLogger(EventCommitLogger.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onCommitted(EventRecorded recorded){
        log.info("Event committed: {} device={}", recorded.event().getType(), recorded.event().getDeviceId());
    }
}