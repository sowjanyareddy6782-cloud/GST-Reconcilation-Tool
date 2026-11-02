package com.example.gstrecon.controller;

import com.example.gstrecon.model.ReconRun;
import com.example.gstrecon.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.NoSuchElementException;

@Controller
public class ReconController {
    private static final String XLSX="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final ExcelReaderService reader; private final ReconciliationService service;
    public ReconController(ExcelReaderService reader,ReconciliationService service){this.reader=reader;this.service=service;}
    @GetMapping("/") public String home(Authentication auth){return hasRole(auth,"ROLE_MANAGER")?"redirect:/dashboard":"upload";}
    @PostMapping("/reconcile") public String reconcile(@RequestParam("books") MultipartFile books,@RequestParam("portal") MultipartFile portal,Authentication auth,Model model){
        try{
            var b=reader.read(books);var p=reader.read(portal);
            var summary=service.run(b,p,books.getOriginalFilename(),portal.getOriginalFilename(),auth.getName());
            model.addAttribute("summary",summary);model.addAttribute("results",summary.results());model.addAttribute("issues",summary.validationIssues());return "result";
        }catch(Exception ex){model.addAttribute("error",safeMessage(ex));return "upload";}
    }
    @GetMapping("/history") public String history(Authentication auth,Model model){model.addAttribute("runs",service.history(auth.getName(),canViewAll(auth)));return "history";}
    @GetMapping("/history/{runId}") public String historyRun(@PathVariable long runId,Authentication auth,Model model){
        try{ReconRun run=service.getRun(runId,auth.getName(),canViewAll(auth));model.addAttribute("run",run);model.addAttribute("results",run.getResults());return "history-result";}
        catch(NoSuchElementException ex){model.addAttribute("error",ex.getMessage());return "history";}
    }
    @GetMapping("/download/{runId}") public void download(@PathVariable long runId,Authentication auth,HttpServletResponse response)throws IOException{
        try{byte[] bytes=service.export(runId,auth.getName(),canViewAll(auth));response.setContentType(XLSX);response.setHeader("Content-Disposition","attachment; filename=\"gst-reconciliation-"+runId+".xlsx\"");response.setContentLength(bytes.length);response.getOutputStream().write(bytes);}
        catch(NoSuchElementException ex){response.sendError(404,ex.getMessage());}
    }
    private boolean hasRole(Authentication auth,String role){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals(role));}
    private boolean canViewAll(Authentication auth){return hasRole(auth,"ROLE_MANAGER")||hasRole(auth,"ROLE_ADMIN");}
    private String safeMessage(Exception ex){String m=ex.getMessage();return m==null||m.isBlank()?"Unable to process these workbooks. Check the file format and try again.":m;}
}
