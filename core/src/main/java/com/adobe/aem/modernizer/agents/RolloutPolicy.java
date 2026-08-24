package com.adobe.aem.modernizer.agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 6-stage rollout policy configuration (Phase 2, Master §28).
 */
public class RolloutPolicy {

    public static class StageDefinition {
        private String name;
        private int trafficPercent;
        private double minVisualScore;

        public StageDefinition(String name, int trafficPercent, double minVisualScore) {
            this.name = name;
            this.trafficPercent = trafficPercent;
            this.minVisualScore = minVisualScore;
        }

        public String getName() { return name; }
        public int getTrafficPercent() { return trafficPercent; }
        public double getMinVisualScore() { return minVisualScore; }
    }

    private List<StageDefinition> stages = new ArrayList<>();

    public RolloutPolicy() {}

    public static RolloutPolicy defaultPolicy() {
        RolloutPolicy policy = new RolloutPolicy();
        policy.stages = Arrays.asList(
                new StageDefinition("PREVIEW", 0, 0.85),
                new StageDefinition("INTERNAL", 0, 0.90),
                new StageDefinition("CANARY", 5, 0.92),
                new StageDefinition("BATCH", 25, 0.95),
                new StageDefinition("BROAD", 50, 0.95),
                new StageDefinition("FULL", 100, 0.95)
        );
        return policy;
    }

    public List<StageDefinition> getStages() { return stages; }
}
