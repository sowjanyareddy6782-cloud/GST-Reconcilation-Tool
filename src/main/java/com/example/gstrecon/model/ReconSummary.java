package com.example.gstrecon.model;

import java.math.BigDecimal;
import java.util.List;

public record ReconSummary(Long runId, String booksFile, String portalFile, int matched,
                          int mismatch, int missingInGst, int missingInBooks,
                          BigDecimal itcAtRisk, List<ReconResult> results,
                          List<String> validationIssues) { }
