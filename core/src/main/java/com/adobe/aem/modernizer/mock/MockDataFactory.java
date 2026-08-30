package com.adobe.aem.modernizer.mock;

import com.adobe.aem.modernizer.persistence.model.SiteInventory;

import java.util.*;

/**
 * Factory creating deterministic WKND sample site structures for offline testing and demos.
 */
public final class MockDataFactory {

    private MockDataFactory() {}

    public static SiteInventory createWkndInventory(String contentRoot, String pageScope, int count) {
        SiteInventory inv = new SiteInventory();
        inv.setTotalPages(count);
        inv.setEligiblePages(count);
        inv.setExcludedPages(0);

        String root = (contentRoot != null && !contentRoot.isEmpty()) ? contentRoot : "/content/wknd";
        String scope = (pageScope != null && !pageScope.isEmpty()) ? pageScope : root;

        boolean isSinglePage = scope.endsWith("ski-touring-mont-blanc")
                || (!scope.endsWith("/*") && !scope.equals("/content/wknd") && !scope.equals("/content/wknd/us/en") && scope.contains("/adventures/"));

        if (isSinglePage) {
            inv.setTotalPages(1);
            inv.setEligiblePages(1);
            inv.setExcludedPages(0);

            SiteInventory.PageInfo p = new SiteInventory.PageInfo(
                    scope.replace(".html", ""),
                    "Ski Touring Mont Blanc",
                    "/conf/wknd/settings/wcm/templates/adventure-page"
            );
            p.setComponentResourceTypes(Arrays.asList(
                    "wknd/components/breadcrumb",
                    "wknd/components/carousel",
                    "wknd/components/tabs",
                    "wknd/components/cards"
            ));
            p.setAssetPaths(Arrays.asList(
                    "/content/dam/wknd-shared/en/adventures/ski-touring-mont-blanc/adobestock-238230356.jpeg",
                    "/content/dam/wknd-shared/en/adventures/ski-touring-mont-blanc/adobestock-21422513.jpeg",
                    "/content/dam/wknd-shared/en/adventures/ski-touring-mont-blanc/adobestock-291339093.jpeg"
            ));
            inv.setPages(Collections.singletonList(p));

            List<SiteInventory.ComponentInfo> components = new ArrayList<>();
            components.add(new SiteInventory.ComponentInfo("wknd/components/breadcrumb", "Breadcrumb", "WKND Structure"));
            components.add(new SiteInventory.ComponentInfo("wknd/components/carousel", "Carousel", "WKND Content"));
            components.add(new SiteInventory.ComponentInfo("wknd/components/tabs", "Tabs", "WKND Content"));
            components.add(new SiteInventory.ComponentInfo("wknd/components/cards", "Cards List", "WKND Content"));
            inv.setComponents(components);

            List<SiteInventory.TemplateInfo> templates = new ArrayList<>();
            templates.add(new SiteInventory.TemplateInfo("/conf/wknd/settings/wcm/templates/adventure-page", "Adventure Page"));
            inv.setTemplates(templates);

            List<SiteInventory.ContentFragmentInfo> cfs = new ArrayList<>();
            cfs.add(new SiteInventory.ContentFragmentInfo("/content/dam/wknd-shared/en/adventures/ski-touring-mont-blanc/ski-touring-mont-blanc", "adventure-model", "Ski Touring Mont Blanc"));
            inv.setContentFragments(cfs);

            List<SiteInventory.AssetInfo> assets = new ArrayList<>();
            assets.add(new SiteInventory.AssetInfo("/content/dam/wknd-shared/en/adventures/ski-touring-mont-blanc/adobestock-238230356.jpeg", "image/jpeg"));
            assets.add(new SiteInventory.AssetInfo("/content/dam/wknd-shared/en/adventures/ski-touring-mont-blanc/adobestock-21422513.jpeg", "image/jpeg"));
            assets.add(new SiteInventory.AssetInfo("/content/dam/wknd-shared/en/adventures/ski-touring-mont-blanc/adobestock-291339093.jpeg", "image/jpeg"));
            inv.setAssets(assets);

            List<SiteInventory.MsmLiveCopyInfo> liveCopies = new ArrayList<>();
            liveCopies.add(new SiteInventory.MsmLiveCopyInfo("/content/wknd/language-masters/en/adventures/ski-touring-mont-blanc", "/content/wknd/us/en/adventures/ski-touring-mont-blanc"));
            inv.setLiveCopies(liveCopies);

            return inv;
        }

        // Distinct Components
        List<SiteInventory.ComponentInfo> components = new ArrayList<>();
        components.add(new SiteInventory.ComponentInfo("wknd/components/hero", "Hero Block", "WKND Content"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/teaser", "Teaser", "WKND Content"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/cards", "Cards List", "WKND Content"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/carousel", "Carousel", "WKND Content"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/tabs", "Tabs", "WKND Content"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/accordion", "Accordion", "WKND Content"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/text", "Text", "WKND Core"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/image", "Image", "WKND Core"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/navigation", "Navigation", "WKND Structure"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/footer", "Footer", "WKND Structure"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/form/container", "Form Container", "WKND Form"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/search", "Search Bar", "WKND Search"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/breadcrumb", "Breadcrumb", "WKND Structure"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/video", "Video Player", "WKND Media"));
        components.add(new SiteInventory.ComponentInfo("wknd/components/button", "Button", "WKND Core"));
        inv.setComponents(components);

        // Populate Pages (Multi-page inventory fallback)
        List<SiteInventory.PageInfo> pages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SiteInventory.PageInfo p = new SiteInventory.PageInfo(
                    root + "/page-" + i,
                    "Page " + i,
                    "/conf/wknd/settings/wcm/templates/article-page"
            );
            p.setComponentResourceTypes(Arrays.asList("wknd/components/hero", "wknd/components/text"));
            p.setAssetPaths(Arrays.asList("/content/dam/wknd/en/adventures/hero-0.jpg"));
            pages.add(p);
        }
        inv.setPages(pages);

        // Distinct Templates
        List<SiteInventory.TemplateInfo> templates = new ArrayList<>();
        templates.add(new SiteInventory.TemplateInfo("/conf/wknd/settings/wcm/templates/landing-page", "Landing Page"));
        templates.add(new SiteInventory.TemplateInfo("/conf/wknd/settings/wcm/templates/article-page", "Article Page"));
        templates.add(new SiteInventory.TemplateInfo("/conf/wknd/settings/wcm/templates/adventure-page", "Adventure Page"));
        inv.setTemplates(templates);

        // Content Fragments
        List<SiteInventory.ContentFragmentInfo> cfs = new ArrayList<>();
        cfs.add(new SiteInventory.ContentFragmentInfo("/content/dam/wknd/en/adventures/bali-surf/cf", "adventure-model", "Bali Surf Camp"));
        cfs.add(new SiteInventory.ContentFragmentInfo("/content/dam/wknd/en/adventures/mont-blanc/cf", "adventure-model", "Mont Blanc Skiing"));
        inv.setContentFragments(cfs);

        // Assets
        List<SiteInventory.AssetInfo> assets = new ArrayList<>();
        assets.add(new SiteInventory.AssetInfo("/content/dam/wknd/en/adventures/hero-0.jpg", "image/jpeg"));
        assets.add(new SiteInventory.AssetInfo("/content/dam/wknd/en/adventures/hero-1.jpg", "image/jpeg"));
        assets.add(new SiteInventory.AssetInfo("/content/dam/wknd/en/magazine/cover-0.png", "image/png"));
        inv.setAssets(assets);

        // MSM Live Copies
        List<SiteInventory.MsmLiveCopyInfo> liveCopies = new ArrayList<>();
        liveCopies.add(new SiteInventory.MsmLiveCopyInfo("/content/wknd/language-masters/en", "/content/wknd/us/en"));
        liveCopies.add(new SiteInventory.MsmLiveCopyInfo("/content/wknd/language-masters/en", "/content/wknd/ca/en"));
        liveCopies.add(new SiteInventory.MsmLiveCopyInfo("/content/wknd/language-masters/en", "/content/wknd/fr/fr"));
        inv.setLiveCopies(liveCopies);

        // Figma Tokens
        Map<String, String> figmaTokens = new HashMap<>();
        figmaTokens.put("--color-brand", "#eb1000");
        figmaTokens.put("--color-text", "#222222");
        figmaTokens.put("--color-bg", "#ffffff");
        figmaTokens.put("--font-body", "'Source Sans Pro', sans-serif");
        figmaTokens.put("--font-heading", "'Source Serif Pro', serif");
        inv.setFigmaTokens(figmaTokens);

        return inv;
    }

    private static String formatTitle(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1).replace('-', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
