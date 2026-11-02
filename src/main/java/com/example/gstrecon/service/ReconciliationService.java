package com.example.gstrecon.service;

import com.example.gstrecon.model.*;
import com.example.gstrecon.repository.ReconRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {
    private final MatchingEngine engine;
    private final ValidationService validation;
    private final ReconRunRepository repository;
    private final ReportWriterService reportWriter;
    public ReconciliationService(MatchingEngine engine, ValidationService validation, ReconRunRepository repository, ReportWriterService reportWriter) {
        this.engine=engine; this.validation=validation; this.repository=repository; this.reportWriter=reportWriter;
    }

    @Transactional
    public ReconSummary run(List<InvoiceRecord> books, List<InvoiceRecord> portal, String booksFile, String portalFile, String ownerUsername) {
        List<String> issues=new ArrayList<>();
        List<InvoiceRecord> validBooks=filter(books,"Books",issues), validPortal=filter(portal,"GSTR-2B",issues);
        if(validBooks.isEmpty() && validPortal.isEmpty()) throw new IllegalArgumentException("No valid invoice rows remain after validation. Check the workbook and row issues.");
        List<ReconResult> results=engine.reconcile(validBooks,validPortal);
        ReconRun run=new ReconRun(); run.setBooksFile(safeName(booksFile)); run.setPortalFile(safeName(portalFile));
        run.setOwnerUsername(ownerUsername);
        run.setTotalMatched(count(results,MatchStatus.MATCHED)); run.setTotalMismatch(count(results,MatchStatus.MISMATCH));
        run.setMissingInGst(count(results,MatchStatus.MISSING_IN_GST)); run.setMissingInBooks(count(results,MatchStatus.MISSING_IN_BOOKS));
        run.setItcAtRisk(results.stream().map(ReconResult::itcAtRisk).reduce(BigDecimal.ZERO,BigDecimal::add));
        for (ReconResult result : results) run.addResult(new ReconResultEntity(result));
        run=repository.save(run);
        return new ReconSummary(run.getId(),run.getBooksFile(),run.getPortalFile(),run.getTotalMatched(),run.getTotalMismatch(),run.getMissingInGst(),run.getMissingInBooks(),run.getItcAtRisk(),results,List.copyOf(issues));
    }
    private List<InvoiceRecord> filter(List<InvoiceRecord> input,String source,List<String> issues) {
        List<InvoiceRecord> valid=new ArrayList<>();
        for(InvoiceRecord r:input){if(r.invoiceDate()==null)issues.add(source+" row "+r.rowNumber()+": invoice date is missing or unrecognized; exact matching remains enabled, fuzzy matching is disabled for this row");List<String> rowIssues=validation.validate(r,source);if(rowIssues.isEmpty())valid.add(r);else issues.addAll(rowIssues);}
        return valid;
    }
    private int count(List<ReconResult> list,MatchStatus status){return (int)list.stream().filter(r->r.status()==status).count();}
    private String safeName(String name){if(name==null)return "upload.xlsx";String base=name.replace('\\','/');base=base.substring(base.lastIndexOf('/')+1);return base.length()>200?base.substring(0,200):base;}
    @Transactional(readOnly=true) public List<ReconRun> history(String username,boolean canViewAll){
        var sort=org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"runTime");
        return canViewAll?repository.findAll(sort):repository.findAllByOwnerUsername(username,sort);
    }
    @Transactional(readOnly=true) public ReconRun getRun(long id,String username,boolean canViewAll){
        ReconRun run=repository.findById(id).orElseThrow(()->new NoSuchElementException("Reconciliation run not found"));
        if(!canViewAll && !Objects.equals(username,run.getOwnerUsername())) throw new org.springframework.security.access.AccessDeniedException("You do not have access to this reconciliation run");
        return run;
    }
    @Transactional(readOnly=true) public byte[] export(long id,String username,boolean canViewAll) throws java.io.IOException {return reportWriter.write(getRun(id,username,canViewAll));}

    @Transactional(readOnly=true)
    public DashboardSnapshot dashboard(String username,boolean canViewAll){
        List<ReconRun> runs=history(username,canViewAll);
        List<ReconRun> recent=runs.stream().limit(10).toList();
        ReconRun latest=recent.isEmpty()?null:recent.get(0);
        List<ReconRun> chronological=new ArrayList<>(recent);Collections.reverse(chronological);
        List<String> labels=chronological.stream().map(r->r.getRunTime().format(DateTimeFormatter.ofPattern("MMM d HH:mm",Locale.ENGLISH))).toList();
        return new DashboardSnapshot(latest==null?0:invoiceCount(latest),latest==null?0:latest.getTotalMatched(),latest==null?0:latest.getTotalMismatch(),latest==null?0:latest.getMissingInGst(),latest==null?0:latest.getMissingInBooks(),latest==null?BigDecimal.ZERO:latest.getItcAtRisk(),labels,
                chronological.stream().map(ReconRun::getTotalMatched).toList(),chronological.stream().map(ReconRun::getTotalMismatch).toList(),chronological.stream().map(ReconRun::getMissingInGst).toList(),chronological.stream().map(ReconRun::getMissingInBooks).toList(),recent);
    }
    private int invoiceCount(ReconRun r){return 2*r.getTotalMatched()+2*r.getTotalMismatch()+r.getMissingInGst()+r.getMissingInBooks();}
}
