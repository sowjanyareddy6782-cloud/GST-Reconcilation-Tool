package com.example.gstrecon.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="recon_run")
public class ReconRun {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private LocalDateTime runTime=LocalDateTime.now();
    private String booksFile; private String portalFile;
    @Column(length=100) private String ownerUsername;
    private int totalMatched; private int totalMismatch; private int missingInGst; private int missingInBooks;
    @Column(precision=15,scale=2) private BigDecimal itcAtRisk=BigDecimal.ZERO;
    @OneToMany(mappedBy="run",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) private List<ReconResultEntity> results=new ArrayList<>();
    public ReconRun() { }
    public Long getId(){return id;} public LocalDateTime getRunTime(){return runTime;} public String getBooksFile(){return booksFile;} public String getPortalFile(){return portalFile;}
    public String getOwnerUsername(){return ownerUsername;}
    public int getTotalMatched(){return totalMatched;} public int getTotalMismatch(){return totalMismatch;} public int getMissingInGst(){return missingInGst;} public int getMissingInBooks(){return missingInBooks;} public BigDecimal getItcAtRisk(){return itcAtRisk;} public List<ReconResultEntity> getResults(){return results;}
    public void setBooksFile(String v){booksFile=v;} public void setPortalFile(String v){portalFile=v;} public void setTotalMatched(int v){totalMatched=v;} public void setTotalMismatch(int v){totalMismatch=v;} public void setMissingInGst(int v){missingInGst=v;} public void setMissingInBooks(int v){missingInBooks=v;} public void setItcAtRisk(BigDecimal v){itcAtRisk=v;}
    public void setOwnerUsername(String v){ownerUsername=v;}
    public void addResult(ReconResultEntity result){results.add(result);result.setRun(this);}
}
