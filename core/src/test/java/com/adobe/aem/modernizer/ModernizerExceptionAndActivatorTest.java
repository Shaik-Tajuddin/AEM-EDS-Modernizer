package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.osgi.ModernizerBundleActivator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModernizerExceptionAndActivatorTest {

    @Test
    void testModernizerException() {
        ModernizerException ex1 = new ModernizerException("message");
        assertThat(ex1.getMessage()).isEqualTo("message");

        ModernizerException ex2 = new ModernizerException("message", new RuntimeException("cause"));
        assertThat(ex2.getCause()).isNotNull();

        ModernizerException ex3 = new ModernizerException(new RuntimeException("cause-only"));
        assertThat(ex3.getCause()).isNotNull();
    }

    @Test
    void testModernizerBundleActivator() {
        ModernizerBundleActivator activator = new ModernizerBundleActivator();
        activator.activate();
    }
}
