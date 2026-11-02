package com.example.gstrecon.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="recon_result",indexes={@Index(name="idx_result_run",columnList="run_id"),@Index(name="idx_result_status",columnList="status")})
public class ReconResultEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="run_id") private ReconRun run;
    private String gstin; private String supplierName; private String booksInvoice; private String portalInvoice;
    @Column(precision=15,scale=2) private BigDecimal booksTax;
    @Column(precision=15,scale=2) private BigDecimal portalTax;
    @Enumerated(EnumType.STRING) private MatchStatus status;
    private double similarity;
    @Column(length=255) private String remarks;
    protected ReconResultEntity() { }
    public ReconResultEntity(ReconResult r){InvoiceRecord b=r.books(),p=r.portal();gstin=b!=null?b.gstin():p.gstin();supplierName=b!=null?b.supplierName():p.supplierName();booksInvoice=b==null?null:b.invoiceNumber();portalInvoice=p==null?null:p.invoiceNumber();booksTax=b==null?null:b.totalTax();portalTax=p==null?null:p.totalTax();status=r.status();similarity=r.similarity();remarks=r.remarks();}
    void setRun(ReconRun v){run=v;} public String getGstin(){return gstin;} public String getSupplierName(){return supplierName;} public String getBooksInvoice(){return booksInvoice;} public String getPortalInvoice(){return portalInvoice;} public BigDecimal getBooksTax(){return booksTax;} public BigDecimal getPortalTax(){return portalTax;} public MatchStatus getStatus(){return status;} public double getSimilarity(){return similarity;} public String getRemarks(){return remarks;}
}
