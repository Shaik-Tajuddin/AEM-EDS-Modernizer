package com.adobe.aem.modernizer.scopes;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Map;

/**
 * Evaluates marker-based opt-in eligibility for resources (ADR 0006).
 */
@Component(service = MarkerEvaluator.class, immediate = true)
@Designate(ocd = MarkerEvaluator.Config.class)
public class MarkerEvaluator {

    @ObjectClassDefinition(name = "AEM → EDS Modernizer - Marker Evaluator Config")
    public @interface Config {
        @AttributeDefinition(name = "Marker Property Name", description = "JCR property name to check for opt-in")
        String marker_property() default "edsModernize";

        @AttributeDefinition(name = "Marker Property Expected Value", description = "Value required for eligibility ('true' or '*' for presence)")
        String marker_value() default "true";
    }

    private String markerProperty = "edsModernize";
    private String markerValue = "true";

    public MarkerEvaluator() {}

    public MarkerEvaluator(String markerProperty, String markerValue) {
        this.markerProperty = markerProperty != null ? markerProperty : "edsModernize";
        this.markerValue = markerValue != null ? markerValue : "true";
    }

    @Activate
    public void activate(Config config) {
        if (config != null) {
            this.markerProperty = config.marker_property();
            this.markerValue = config.marker_value();
        }
    }

    public boolean isEligible(Map<String, Object> properties, String projectOverrideProp, String projectOverrideVal) {
        if (properties == null) {
            return false;
        }

        String prop = (projectOverrideProp != null && !projectOverrideProp.isEmpty())
                ? projectOverrideProp : this.markerProperty;
        String val = (projectOverrideVal != null && !projectOverrideVal.isEmpty())
                ? projectOverrideVal : this.markerValue;

        if (prop == null || prop.isEmpty()) {
            return true; // No marker required
        }

        Object actual = properties.get(prop);
        if (actual == null) {
            return false;
        }

        if ("*".equals(val)) {
            return true;
        }

        return val.equalsIgnoreCase(String.valueOf(actual));
    }

    public String getMarkerProperty() { return markerProperty; }
    public String getMarkerValue() { return markerValue; }
}
