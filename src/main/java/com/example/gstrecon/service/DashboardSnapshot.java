package com.example.gstrecon.service;

import com.example.gstrecon.model.ReconRun;
import java.math.BigDecimal;
import java.util.List;

public record DashboardSnapshot(int totalInvoices,int matched,int mismatch,int missingInGst,int missingInBooks,
                                BigDecimal itcAtRisk,List<String> trendLabels,List<Integer> matchedTrend,
                                List<Integer> mismatchTrend,List<Integer> missingInGstTrend,
                                List<Integer> missingInBooksTrend,List<ReconRun> recentRuns) { }
