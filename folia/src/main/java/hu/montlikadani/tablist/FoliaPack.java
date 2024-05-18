package hu.montlikadani.tablist;

import hu.montlikadani.api.Pair;
import hu.montlikadani.api.TicksPerSecondType;
import io.papermc.paper.threadedregions.TickRegionScheduler;
import io.papermc.paper.threadedregions.TickRegions;

public final class FoliaPack {

    public static Pair<Double, String> tickReportDataByType(TicksPerSecondType type) {
        io.papermc.paper.threadedregions.ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData,
                TickRegions.TickRegionSectionData> currentRegion = TickRegionScheduler.getCurrentRegion();

        if (currentRegion == null) {
            return new Pair<>(-1.0, "no current region");
        }

        if (currentRegion.getData() == null) {
            return new Pair<>(-1.0, "no region data available");
        }

        TickRegionScheduler.RegionScheduleHandle scheduleHandle = currentRegion.getData().getRegionSchedulingHandle();
        io.papermc.paper.threadedregions.TickData.TickReportData tickReportData;

        switch (type) {
            case SECONDS_5:
                tickReportData = scheduleHandle.getTickReport5s(System.nanoTime());
                break;
            case SECONDS_15:
                tickReportData = scheduleHandle.getTickReport15s(System.nanoTime());
                break;
            case MINUTES_1:
                tickReportData = scheduleHandle.getTickReport1m(System.nanoTime());
                break;
            case MINUTES_5:
                tickReportData = scheduleHandle.getTickReport5m(System.nanoTime());
                break;
            case MINUTES_15:
                tickReportData = scheduleHandle.getTickReport15m(System.nanoTime());
                break;
            default:
                return new Pair<>(-1.0, "-1");
        }

        if (tickReportData == null) {
            return new Pair<>(-1.0, "no tick report generated");
        }

        return new Pair<>(tickReportData.tpsData().segmentAll().average(), null);
    }
}
