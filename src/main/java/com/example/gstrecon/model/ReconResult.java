package com.example.gstrecon.model;

import java.math.BigDecimal;

public record ReconResult(InvoiceRecord books, InvoiceRecord portal, MatchStatus status,
                          double similarity, String remarks, BigDecimal itcAtRisk) { }
