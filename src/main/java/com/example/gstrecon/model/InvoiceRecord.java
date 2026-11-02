package com.example.gstrecon.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceRecord(int rowNumber, String gstin, String supplierName, String invoiceNumber,
                            LocalDate invoiceDate, BigDecimal taxableValue, BigDecimal igst,
                            BigDecimal cgst, BigDecimal sgst) {
    public BigDecimal totalTax() { return igst.add(cgst).add(sgst); }
}
