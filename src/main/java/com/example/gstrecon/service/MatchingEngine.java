package com.example.gstrecon.service;

import com.example.gstrecon.model.*;
import com.example.gstrecon.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingEngine {
    private static final BigDecimal TOLERANCE = new BigDecimal("1.00");
    @Value("${recon.fuzzy-threshold:0.85}") private double threshold;

    public List<ReconResult> reconcile(List<InvoiceRecord> books, List<InvoiceRecord> portal) {
        boolean[] usedBooks = new boolean[books.size()], usedPortal = new boolean[portal.size()];
        List<ReconResult> out = new ArrayList<>();
        Map<String, Deque<Integer>> exact = new HashMap<>();
        for (int j=0;j<portal.size();j++) exact.computeIfAbsent(key(portal.get(j)), k -> new ArrayDeque<>()).add(j);
        for (int i=0;i<books.size();i++) {
            Deque<Integer> matches=exact.get(key(books.get(i)));
            if (matches != null && !matches.isEmpty()) { int j=matches.removeFirst(); usedBooks[i]=true; usedPortal[j]=true; out.add(compare(books.get(i),portal.get(j),1.0,"Exact GSTIN and normalized invoice match")); }
        }
        Map<String,List<Integer>> byGstin=new HashMap<>();
        for (int j=0;j<portal.size();j++) if (!usedPortal[j]) byGstin.computeIfAbsent(portal.get(j).gstin(), k -> new ArrayList<>()).add(j);
        for (int i=0;i<books.size();i++) if (!usedBooks[i]) {
            InvoiceRecord b=books.get(i); List<Integer> candidates=byGstin.getOrDefault(b.gstin(),List.of());
            int best=-1; double score=0, runnerUp=0;
            for (int j:candidates) if (!usedPortal[j]) {
                InvoiceRecord p=portal.get(j);
                if (b.invoiceDate()==null || p.invoiceDate()==null) continue;
                if (Math.abs(ChronoUnit.DAYS.between(b.invoiceDate(),p.invoiceDate()))>7) continue;
                double s=LevenshteinUtil.similarity(NormalizerUtil.normalizeInvoice(b.invoiceNumber()),NormalizerUtil.normalizeInvoice(p.invoiceNumber()));
                if (s>score) { runnerUp=score; score=s; best=j; } else if (s>runnerUp) runnerUp=s;
            }
            // Refuse ambiguous ties; false matches can overstate supported ITC.
            if (best>=0 && score>=threshold && score-runnerUp>=0.03) { usedBooks[i]=true; usedPortal[best]=true; out.add(compare(b,portal.get(best),score,"Fuzzy invoice match; same GSTIN and within 7 days")); }
        }
        for (int i=0;i<books.size();i++) if (!usedBooks[i]) { InvoiceRecord b=books.get(i); String dateNote=b.invoiceDate()==null?"; fuzzy match unavailable because invoice date is missing or invalid":""; out.add(new ReconResult(b,null,MatchStatus.MISSING_IN_GST,0,"Not found in GSTR-2B"+dateNote+"; claimed ITC is at risk",b.totalTax())); }
        for (int j=0;j<portal.size();j++) if (!usedPortal[j]) { InvoiceRecord p=portal.get(j); String dateNote=p.invoiceDate()==null?"; fuzzy match unavailable because invoice date is missing or invalid":""; out.add(new ReconResult(null,p,MatchStatus.MISSING_IN_BOOKS,0,"On portal but not in purchase register"+dateNote,BigDecimal.ZERO)); }
        return out;
    }
    private String key(InvoiceRecord r) { return r.gstin()+"|"+NormalizerUtil.normalizeInvoice(r.invoiceNumber()); }
    private ReconResult compare(InvoiceRecord b, InvoiceRecord p, double score, String how) {
        boolean taxable=b.taxableValue().subtract(p.taxableValue()).abs().compareTo(TOLERANCE)<=0;
        boolean tax=b.totalTax().subtract(p.totalTax()).abs().compareTo(TOLERANCE)<=0;
        MatchStatus status=taxable && tax ? MatchStatus.MATCHED : MatchStatus.MISMATCH;
        BigDecimal risk=b.totalTax().subtract(p.totalTax()).max(BigDecimal.ZERO);
        String remarks= status==MatchStatus.MATCHED ? how : how+"; taxable or tax amount differs";
        return new ReconResult(b,p,status,score,remarks,risk);
    }
}
