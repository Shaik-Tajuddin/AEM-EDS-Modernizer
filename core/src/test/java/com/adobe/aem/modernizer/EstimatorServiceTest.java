package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.mock.MockDataFactory;
import com.adobe.aem.modernizer.persistence.model.MigrationPlan;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.services.EstimatorService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstimatorServiceTest {

    @Test
    void testEstimateCalculationAndDerivationTrail() {
        EstimatorService service = new EstimatorService();
        SiteInventory inv = MockDataFactory.createWkndInventory("/content/wknd", null, 42);

        MigrationPlan plan = service.estimate("proj-1", "job-1", inv, null);

        assertThat(plan).isNotNull();
        assertThat(plan.getPagesEligible()).isEqualTo(42);
        assertThat(plan.getEdsBlocksNew()).isGreaterThan(0);
        assertThat(plan.getAiRequestsExpected()).isGreaterThan(0);
        assertThat(plan.getCostExpected()).isGreaterThan(0.0);
        assertThat(plan.getCostLo()).isLessThan(plan.getCostExpected());
        assertThat(plan.getCostHi()).isGreaterThan(plan.getCostExpected());
        assertThat(plan.getDerivationTrail()).isNotEmpty();
        assertThat(plan.getStatus()).isEqualTo("CURRENT");
    }
}
