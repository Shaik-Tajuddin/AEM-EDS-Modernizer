package com.adobe.aem.modernizer.services;

import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.ClarificationRecord;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.UUID;

/**
 * Service managing human clarifications during migration planning and execution (Master §20).
 */
@Component(service = ClarificationService.class, immediate = true)
public class ClarificationService {

    @Reference
    private transient Store store;

    public ClarificationService() {}

    public ClarificationService(Store store) {
        this.store = store;
    }

    public ClarificationRecord ask(String projectId, String jobId, String question, String rationale, List<String> options, String defaultOption) {
        ClarificationRecord rec = new ClarificationRecord(UUID.randomUUID().toString(), projectId, jobId, question, defaultOption);
        rec.setRationale(rationale);
        if (options != null) {
            rec.getOptions().addAll(options);
        }
        if (store != null) {
            store.saveClarification(rec);
        }
        return rec;
    }

    public void resolve(String clarificationId, String selectedOption) {
        if (store == null) return;
        for (ClarificationRecord c : store.getClarificationsForProject(null)) {
            if (c.getId().equals(clarificationId)) {
                c.setSelectedOption(selectedOption);
                store.saveClarification(c);
                break;
            }
        }
    }
}
