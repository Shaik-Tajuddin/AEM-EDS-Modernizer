package com.adobe.aem.modernizer.services;

import com.adobe.aem.modernizer.persistence.model.BenchmarkSampleRecord;
import org.osgi.service.component.annotations.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Historical performance and cost benchmark calculator (Phase 2).
 */
@Component(service = BenchmarkService.class, immediate = true)
public class BenchmarkService {

    public static class BenchmarkStats {
        private String agent;
        private int sampleCount;
        private long p50DurationMs;
        private long p95DurationMs;
        private double avgCostMicros;

        public BenchmarkStats(String agent, int sampleCount, long p50DurationMs, long p95DurationMs, double avgCostMicros) {
            this.agent = agent;
            this.sampleCount = sampleCount;
            this.p50DurationMs = p50DurationMs;
            this.p95DurationMs = p95DurationMs;
            this.avgCostMicros = avgCostMicros;
        }

        public String getAgent() { return agent; }
        public int getSampleCount() { return sampleCount; }
        public long getP50DurationMs() { return p50DurationMs; }
        public long getP95DurationMs() { return p95DurationMs; }
        public double getAvgCostMicros() { return avgCostMicros; }
    }

    public List<BenchmarkStats> computeStats(List<BenchmarkSampleRecord> samples) {
        if (samples == null || samples.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<BenchmarkSampleRecord>> byAgent = samples.stream()
                .collect(Collectors.groupingBy(BenchmarkSampleRecord::getAgent));

        List<BenchmarkStats> result = new ArrayList<>();
        for (Map.Entry<String, List<BenchmarkSampleRecord>> entry : byAgent.entrySet()) {
            String agent = entry.getKey();
            List<BenchmarkSampleRecord> list = entry.getValue();

            List<Long> durations = list.stream()
                    .map(BenchmarkSampleRecord::getDurationMs)
                    .sorted()
                    .collect(Collectors.toList());

            long p50 = durations.get(durations.size() / 2);
            int p95Index = (int) Math.min((double) durations.size() - 1.0, Math.ceil(durations.size() * 0.95) - 1.0);
            long p95 = durations.get(Math.max(0, p95Index));

            double avgCost = list.stream()
                    .mapToDouble(BenchmarkSampleRecord::getCostMicros)
                    .average()
                    .orElse(0.0);

            result.add(new BenchmarkStats(agent, list.size(), p50, p95, avgCost));
        }

        return result;
    }
}
