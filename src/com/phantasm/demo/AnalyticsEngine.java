package com.phantasm.demo;

/**
 * Second target payload class standing in for proprietary telemetry & analytics algorithms.
 * Demonstrates parallel multi-class decryption and loading.
 */
public class AnalyticsEngine {

    public String getEngineVersion() {
        return "AnalyticsEngine-v2.5.0-PROTECTED";
    }

    public double calculateSystemHealthScore(int activeThreads, double cpuUsagePercent, long freeMemoryMb) {
        double score = 100.0 - (cpuUsagePercent * 0.5) - (activeThreads * 0.2);
        if (freeMemoryMb < 512) {
            score -= 15.0;
        }
        return Math.max(0.0, Math.min(100.0, score));
    }

    public String generateTelemetryReport(String systemId) {
        return String.format("Telemetry Report for [%s]: System operational, memory protected, encryption active.", systemId);
    }
}
