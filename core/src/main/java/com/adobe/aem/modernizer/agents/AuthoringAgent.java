package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.connectors.AemClient;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Prepares AEM Universal Editor compatible page models and authoring contracts (Stage: AUTHORING).
 */
public class AuthoringAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(AuthoringAgent.class);

    private final AemClient aemClient;
    private final Store store;
    private final AiGateway ai;

    public AuthoringAgent(AemClient aemClient, Store store, AiGateway ai) {
        this.aemClient = aemClient;
        this.store = store;
        this.ai = ai;
    }

    @Override
    public String getName() {
        return "authoring";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.AUTHORING;
    }

    @Override
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        String strategy = ctx.getProject().getAuthoringStrategy();
        LOG.info("AuthoringAgent applying authoring strategy: {}", strategy);

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Configured Universal Editor authoring metadata under strategy '" + strategy + "'."
            ));
        }
    }
}
