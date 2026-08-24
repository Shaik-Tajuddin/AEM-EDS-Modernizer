package com.adobe.aem.modernizer.services;

import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.persistence.model.UrlRedirectRecord;
import org.osgi.service.component.annotations.Component;

import java.util.*;

/**
 * Service for calculating URL transformations and detecting redirect conflicts (Master §22).
 */
@Component(service = UrlRedirectService.class, immediate = true)
public class UrlRedirectService {

    public List<UrlRedirectRecord> buildRedirects(String projectId, String jobId, SiteInventory inventory) {
        List<UrlRedirectRecord> list = new ArrayList<>();
        if (inventory == null || inventory.getPages() == null) {
            return list;
        }

        Set<String> seenTargets = new HashSet<>();

        for (SiteInventory.PageInfo page : inventory.getPages()) {
            String aemPath = page.getPath();
            String edsPath = transformToEdsPath(aemPath);

            UrlRedirectRecord rec = new UrlRedirectRecord(
                    UUID.randomUUID().toString(),
                    projectId,
                    jobId,
                    aemPath + ".html",
                    edsPath
            );

            if (seenTargets.contains(edsPath)) {
                rec.setConflict(true);
                rec.setConflictReason("Duplicate target URL generated for multiple source paths: " + edsPath);
            } else {
                seenTargets.add(edsPath);
            }

            list.add(rec);
        }

        return list;
    }

    public static String transformToEdsPath(String aemPath) {
        if (aemPath == null) return "/";
        String path = aemPath;
        if (path.startsWith("/content/wknd")) {
            path = path.substring("/content/wknd".length());
        } else if (path.startsWith("/content/")) {
            int secondSlash = path.indexOf('/', "/content/".length());
            if (secondSlash > 0) {
                path = path.substring(secondSlash);
            }
        }
        if (path.isEmpty()) return "/";
        return path;
    }
}
