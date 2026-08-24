package com.adobe.aem.modernizer.services;

import com.adobe.aem.modernizer.persistence.model.DependencyEdgeRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for computing page, block, style, and asset dependencies (Phase 2).
 */
@Component(service = DependencyGraphService.class, immediate = true)
public class DependencyGraphService {

    public List<DependencyEdgeRecord> buildGraph(String projectId, String jobId, SiteInventory inventory) {
        List<DependencyEdgeRecord> edges = new ArrayList<>();
        if (inventory == null || inventory.getPages() == null) {
            return edges;
        }

        for (SiteInventory.PageInfo page : inventory.getPages()) {
            String pageNode = "page:" + page.getPath();

            // Page -> Blocks
            for (String comp : page.getComponentResourceTypes()) {
                String blockName = comp.substring(comp.lastIndexOf('/') + 1);
                String blockNode = "block:" + blockName;
                edges.add(new DependencyEdgeRecord(UUID.randomUUID().toString(), projectId, jobId, pageNode, blockNode, "PAGE_TO_BLOCK"));
                edges.add(new DependencyEdgeRecord(UUID.randomUUID().toString(), projectId, jobId, blockNode, "style:" + blockName + ".css", "BLOCK_TO_CSS"));
                edges.add(new DependencyEdgeRecord(UUID.randomUUID().toString(), projectId, jobId, blockNode, "script:" + blockName + ".js", "BLOCK_TO_JS"));
            }

            // Page -> Assets
            for (String asset : page.getAssetPaths()) {
                String assetNode = "asset:" + asset;
                edges.add(new DependencyEdgeRecord(UUID.randomUUID().toString(), projectId, jobId, pageNode, assetNode, "PAGE_TO_ASSET"));
            }
        }

        return edges;
    }
}
