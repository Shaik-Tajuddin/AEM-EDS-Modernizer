package com.adobe.aem.modernizer.ai;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi factory configuration for a cloud AI provider endpoint.
 * {@code providerName} + {@code baseUrl} + {@code apiKeyRef} are the live source of truth;
 * model for anthropic/openai/gemini comes from Project Setup ({@code aiModel}).
 */
@Component(
        service = AiProviderEndpointConfig.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        configurationPid = "com.adobe.aem.modernizer.ai.AiProviderEndpointConfig"
)
@Designate(ocd = AiProviderEndpointConfig.Config.class, factory = true)
public class AiProviderEndpointConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AiProviderEndpointConfig.class);

    @ObjectClassDefinition(name = "AEM EDS Modernizer — AI Provider Endpoint")
    public @interface Config {
        @AttributeDefinition(name = "Provider name", description = "anthropic | openai | gemini | ollama")
        String providerName() default "";

        @AttributeDefinition(name = "API base URL")
        String baseUrl() default "";

        @AttributeDefinition(name = "API key secret ref", description = "e.g. env:ANTHROPIC_API_KEY")
        String apiKeyRef() default "";

        @AttributeDefinition(name = "Default model",
                description = "Only used for ollama; cloud models come from Project Setup form")
        String defaultModel() default "";

        @AttributeDefinition(name = "Enabled")
        boolean enabled() default true;
    }

    private volatile AiProviderEndpoint endpoint;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.endpoint = new AiProviderEndpoint(
                trim(config.providerName()),
                trim(config.baseUrl()),
                trim(config.apiKeyRef()),
                trim(config.defaultModel()),
                config.enabled()
        );
        LOG.info("AiProviderEndpointConfig active: provider={} baseUrl={} enabled={}",
                endpoint.getProviderName(), endpoint.getBaseUrl(), endpoint.isEnabled());
    }

    @Deactivate
    protected void deactivate() {
        this.endpoint = null;
    }

    public AiProviderEndpoint getEndpoint() {
        return endpoint;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
