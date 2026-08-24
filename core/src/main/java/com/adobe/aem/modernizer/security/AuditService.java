package com.adobe.aem.modernizer.security;

import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Emits redacted audit records for privileged operations (Master §29).
 */
@Component(service = AuditService.class, immediate = true)
public class AuditService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);

    @Reference
    private transient Store store;

    public AuditService() {}

    public AuditService(Store store) {
        this.store = store;
    }

    public void audit(String projectId, String jobId, String actor, String agent, String message) {
        audit(projectId, jobId, actor, agent, "INFO", null, null, message);
    }

    public void audit(String projectId, String jobId, String actor, String agent, String level, String fromState, String toState, String message) {
        String cleanMessage = Redactor.redact(message);
        JobEventRecord event = new JobEventRecord(UUID.randomUUID().toString(), projectId, jobId, agent, cleanMessage);
        event.setActor(actor != null ? actor : "system");
        event.setLevel(level != null ? level : "INFO");
        event.setFromState(fromState);
        event.setToState(toState);

        LOG.info("[AUDIT] project={} job={} actor={} agent={} state={}>{} msg={}",
                projectId, jobId, event.getActor(), agent, fromState, toState, cleanMessage);

        if (store != null) {
            store.recordEvent(event);
        }
    }
}
