package com.example.gstrecon.service;

import com.example.gstrecon.model.InvoiceRecord;
import com.example.gstrecon.util.NormalizerUtil;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationService {
    public List<String> validate(InvoiceRecord r, String source) {
        List<String> errors = new ArrayList<>();
        String prefix = source + " row " + r.rowNumber() + ": ";
        if (!NormalizerUtil.isValidGstin(r.gstin())) errors.add(prefix + "invalid GSTIN; row skipped");
        if (r.supplierName().isBlank()) errors.add(prefix + "supplier name is empty; row skipped");
        if (r.invoiceNumber().isBlank()) errors.add(prefix + "invoice number is empty; row skipped");
        if (r.taxableValue().signum() < 0 || r.igst().signum() < 0 || r.cgst().signum() < 0 || r.sgst().signum() < 0)
            errors.add(prefix + "negative amount; row skipped");
        if (r.taxableValue().compareTo(BigDecimal.ZERO) == 0 && r.totalTax().compareTo(BigDecimal.ZERO) == 0)
            errors.add(prefix + "all monetary amounts are zero; row skipped");
        return errors;
    }
}
