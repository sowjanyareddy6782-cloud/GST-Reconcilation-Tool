package com.example.gstrecon.service;

import com.example.gstrecon.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ReportWriterService {
    private static final List<String> COLUMNS=List.of("Status","GSTIN","Supplier","Books invoice","Portal invoice","Books tax","Portal tax","Difference (books - portal)","Similarity","Remarks");
    public byte[] write(ReconRun run) throws IOException {
        try(XSSFWorkbook wb=new XSSFWorkbook(); ByteArrayOutputStream out=new ByteArrayOutputStream()){
            CellStyle header=wb.createCellStyle(); Font font=wb.createFont();font.setBold(true);font.setColor(IndexedColors.WHITE.getIndex());header.setFont(font);header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle green=fillStyle(wb,IndexedColors.LIGHT_GREEN), amber=fillStyle(wb,IndexedColors.LIGHT_ORANGE), red=fillStyle(wb,IndexedColors.ROSE), blue=fillStyle(wb,IndexedColors.LIGHT_CORNFLOWER_BLUE);
            CellStyle money=wb.createCellStyle();money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00;[Red]-#,##0.00"));
            Sheet summary=wb.createSheet("Summary");
            String[][] info={{"GST Invoice Reconciliation","Value"},{"Run ID",String.valueOf(run.getId())},{"Run time",String.valueOf(run.getRunTime())},{"Purchase register",run.getBooksFile()},{"GSTR-2B file",run.getPortalFile()},{"Matched",String.valueOf(run.getTotalMatched())},{"Mismatch",String.valueOf(run.getTotalMismatch())},{"Missing in GST",String.valueOf(run.getMissingInGst())},{"Missing in Books",String.valueOf(run.getMissingInBooks())},{"ITC at risk",run.getItcAtRisk().toPlainString()}};
            for(int i=0;i<info.length;i++){Row row=summary.createRow(i);for(int j=0;j<2;j++){Cell c=row.createCell(j);c.setCellValue(info[i][j]);if(i==0)c.setCellStyle(header);}} summary.setColumnWidth(0,6500);summary.setColumnWidth(1,8000);
            for(MatchStatus status:MatchStatus.values()){
                String title=switch(status){case MATCHED->"Matched";case MISMATCH->"Mismatch";case MISSING_IN_GST->"Missing in GST";case MISSING_IN_BOOKS->"Missing in Books";};
                Sheet sheet=wb.createSheet(title);Row h=sheet.createRow(0);for(int c=0;c<COLUMNS.size();c++){Cell cell=h.createCell(c);cell.setCellValue(COLUMNS.get(c));cell.setCellStyle(header);}
                int rowNum=1;for(ReconResultEntity result:run.getResults())if(result.getStatus()==status){Row row=sheet.createRow(rowNum++);Cell statusCell=row.createCell(0);statusCell.setCellValue(result.getStatus().name());statusCell.setCellStyle(status==MatchStatus.MATCHED?green:status==MatchStatus.MISMATCH?amber:status==MatchStatus.MISSING_IN_GST?red:blue);String[] strings={result.getGstin(),result.getSupplierName(),result.getBooksInvoice(),result.getPortalInvoice(),null,null,null,null,String.format(Locale.ROOT,"%.3f",result.getSimilarity()),result.getRemarks()};for(int c=0;c<strings.length;c++)if(strings[c]!=null)row.createCell(c+1).setCellValue(strings[c]);setMoney(row,5,result.getBooksTax(),money);setMoney(row,6,result.getPortalTax(),money);BigDecimal delta=result.getBooksTax()==null||result.getPortalTax()==null?null:result.getBooksTax().subtract(result.getPortalTax());setMoney(row,7,delta,money);}
                for(int c=0;c<COLUMNS.size();c++)sheet.autoSizeColumn(c);
            }
            wb.write(out);return out.toByteArray();
        }
    }
    private CellStyle fillStyle(Workbook wb,IndexedColors color){CellStyle s=wb.createCellStyle();s.setFillForegroundColor(color.getIndex());s.setFillPattern(FillPatternType.SOLID_FOREGROUND);return s;}
    private void setMoney(Row row,int col,BigDecimal value,CellStyle style){if(value!=null){Cell c=row.createCell(col);c.setCellValue(value.doubleValue());c.setCellStyle(style);}}
}
