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

        List<String> pageNames = Arrays.asList(
                "home", "adventures", "magazine", "about-us", "contact-us",
                "adventures/riverside-camping", "adventures/ski-touring-mont-blanc",
                "adventures/downhill-mountain-biking", "adventures/surf-camp-bali",
                "magazine/san-diego-travel", "magazine/western-australia",
                "magazine/arctic-surfing", "magazine/fly-fishing-amazon",
                "faq", "privacy-policy"
        );

        int total = Math.max(count, pageNames.size());
        for (int i = 0; i < total; i++) {
            String subPath = i < pageNames.size() ? pageNames.get(i) : "page-" + i;
            String path = root + "/" + subPath;
            String title = formatTitle(subPath);

            SiteInventory.PageInfo p = new SiteInventory.PageInfo(path, title, "/conf/wknd/settings/wcm/templates/article-page");
            p.getComponentResourceTypes().add("wknd/components/hero");
            p.getComponentResourceTypes().add("wknd/components/text");
            p.getComponentResourceTypes().add("wknd/components/image");
            if (i % 2 == 0) p.getComponentResourceTypes().add("wknd/components/teaser");
            if (i % 3 == 0) p.getComponentResourceTypes().add("wknd/components/carousel");
            if (i % 4 == 0) p.getComponentResourceTypes().add("wknd/components/tabs");

            p.getAssetPaths().add("/content/dam/wknd/en/adventures/hero-" + (i % 5) + ".jpg");
            p.getAssetPaths().add("/content/dam/wknd/en/magazine/cover-" + (i % 3) + ".png");

            inv.getPages().add(p);
        }

        // Distinct Components
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/hero", "Hero Block", "WKND Content"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/teaser", "Teaser", "WKND Content"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/cards", "Cards List", "WKND Content"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/carousel", "Carousel", "WKND Content"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/tabs", "Tabs", "WKND Content"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/accordion", "Accordion", "WKND Content"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/text", "Text", "WKND Core"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/image", "Image", "WKND Core"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/navigation", "Navigation", "WKND Structure"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/footer", "Footer", "WKND Structure"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/form/container", "Form Container", "WKND Form"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/search", "Search Bar", "WKND Search"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/breadcrumb", "Breadcrumb", "WKND Structure"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/video", "Video Player", "WKND Media"));
        inv.getComponents().add(new SiteInventory.ComponentInfo("wknd/components/button", "Button", "WKND Core"));

        // Distinct Templates
        inv.getTemplates().add(new SiteInventory.TemplateInfo("/conf/wknd/settings/wcm/templates/landing-page", "Landing Page"));
        inv.getTemplates().add(new SiteInventory.TemplateInfo("/conf/wknd/settings/wcm/templates/article-page", "Article Page"));
        inv.getTemplates().add(new SiteInventory.TemplateInfo("/conf/wknd/settings/wcm/templates/adventure-page", "Adventure Page"));

        // Content Fragments
        inv.getContentFragments().add(new SiteInventory.ContentFragmentInfo("/content/dam/wknd/en/adventures/bali-surf/cf", "adventure-model", "Bali Surf Camp"));
        inv.getContentFragments().add(new SiteInventory.ContentFragmentInfo("/content/dam/wknd/en/adventures/mont-blanc/cf", "adventure-model", "Mont Blanc Skiing"));

        // Assets
        inv.getAssets().add(new SiteInventory.AssetInfo("/content/dam/wknd/en/adventures/hero-0.jpg", "image/jpeg"));
        inv.getAssets().add(new SiteInventory.AssetInfo("/content/dam/wknd/en/adventures/hero-1.jpg", "image/jpeg"));
        inv.getAssets().add(new SiteInventory.AssetInfo("/content/dam/wknd/en/magazine/cover-0.png", "image/png"));

        // MSM Live Copies
        inv.getLiveCopies().add(new SiteInventory.MsmLiveCopyInfo("/content/wknd/language-masters/en", "/content/wknd/us/en"));
        inv.getLiveCopies().add(new SiteInventory.MsmLiveCopyInfo("/content/wknd/language-masters/en", "/content/wknd/ca/en"));
        inv.getLiveCopies().add(new SiteInventory.MsmLiveCopyInfo("/content/wknd/language-masters/en", "/content/wknd/fr/fr"));

        // Figma Tokens
        inv.getFigmaTokens().put("--color-brand", "#eb1000");
        inv.getFigmaTokens().put("--color-text", "#222222");
        inv.getFigmaTokens().put("--color-bg", "#ffffff");
        inv.getFigmaTokens().put("--font-body", "'Source Sans Pro', sans-serif");
        inv.getFigmaTokens().put("--font-heading", "'Source Serif Pro', serif");

        return inv;
    }

    private static String formatTitle(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1).replace('-', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
