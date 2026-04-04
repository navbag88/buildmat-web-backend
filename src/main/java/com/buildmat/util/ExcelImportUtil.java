package com.buildmat.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

public class ExcelImportUtil {

    // ── Template generators ────────────────────────────────────────────────────

    public static byte[] generateCustomerTemplate() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Customers");
            CellStyle headerStyle = headerStyle(wb);
            CellStyle noteStyle   = noteStyle(wb);
            Row header = sheet.createRow(0);
            String[] cols = {"Name *", "Phone", "Email", "Address"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);
            }
            // Sample row
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("John Doe");
            sample.createCell(1).setCellValue("9876543210");
            sample.createCell(2).setCellValue("john@example.com");
            sample.createCell(3).setCellValue("123, Main Street, City");
            // Notes row
            Row note = sheet.createRow(3);
            Cell nc = note.createCell(0);
            nc.setCellValue("* Name is required. Row 2 is a sample - replace or delete it.");
            nc.setCellStyle(noteStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(3, 3, 0, 3));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    public static byte[] generateProductTemplate() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Products");
            CellStyle headerStyle = headerStyle(wb);
            CellStyle noteStyle   = noteStyle(wb);
            Row header = sheet.createRow(0);
            String[] cols = {"Name *", "Category", "Unit", "Price *", "Stock Qty", "SGST %", "CGST %"};
            int[] widths  = {6000, 5000, 4000, 4000, 4000, 3500, 3500};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, widths[i]);
            }
            // Sample row
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("Cement OPC 53");
            sample.createCell(1).setCellValue("Cement");
            sample.createCell(2).setCellValue("Bag");
            sample.createCell(3).setCellValue(350.00);
            sample.createCell(4).setCellValue(100);
            sample.createCell(5).setCellValue(9);
            sample.createCell(6).setCellValue(9);
            // Notes row
            Row note = sheet.createRow(3);
            Cell nc = note.createCell(0);
            nc.setCellValue("* Name and Price (> 0) are required. Row 2 is a sample - replace or delete it.");
            nc.setCellStyle(noteStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(3, 3, 0, 6));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private static CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private static CellStyle noteStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f);
        return s;
    }

    public static List<Map<String,Object>> importCustomers(InputStream is) throws Exception {
        List<Map<String,Object>> result = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            int[] idx = detectCols(sheet.getRow(0), new String[]{"name","phone","mobile","email","address"});
            for (int r=1; r<=sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row==null||isEmpty(row)) continue;
                String name = str(row, idx[0]);
                if (name.isEmpty()) { result.add(Map.of("error","Row "+(r+1)+": Name required")); continue; }
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("name",name); m.put("phone",str(row,idx[1])); m.put("email",str(row,idx[3])); m.put("address",str(row,idx[4]));
                result.add(m);
            }
        }
        return result;
    }

    public static List<Map<String,Object>> importProducts(InputStream is) throws Exception {
        List<Map<String,Object>> result = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            int[] idx = detectCols(sheet.getRow(0), new String[]{"name","category","unit","price","stock","sgst","cgst"});
            for (int r=1; r<=sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row==null||isEmpty(row)) continue;
                String name = str(row, idx[0]);
                if (name.isEmpty()) { result.add(Map.of("error","Row "+(r+1)+": Name required")); continue; }
                double price = num(row, idx[3]);
                if (price<=0) { result.add(Map.of("error","Row "+(r+1)+" ("+name+"): Price must be > 0")); continue; }
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("name",name); m.put("category",str(row,idx[1])); m.put("unit",str(row,idx[2]).isEmpty()?"Unit":str(row,idx[2]));
                m.put("price",price); m.put("stockQty",num(row,idx[4])); m.put("sgstPercent",num(row,idx[5])); m.put("cgstPercent",num(row,idx[6]));
                result.add(m);
            }
        }
        return result;
    }

    private static int[] detectCols(Row hdr, String[] targets) {
        int[] idx = new int[targets.length]; Arrays.fill(idx,-1);
        if (hdr==null) return idx;
        for (Cell c : hdr) {
            String h = str(c).toLowerCase().replaceAll("[^a-z]","");
            for (int i=0; i<targets.length; i++) {
                if (h.contains(targets[i]) && idx[i]==-1) idx[i]=c.getColumnIndex();
            }
        }
        return idx;
    }

    private static String str(Row row, int col) {
        if (col<0||row==null) return "";
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return c==null?"":str(c);
    }

    private static String str(Cell c) {
        if (c==null) return "";
        return switch(c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> { double v=c.getNumericCellValue(); yield v==(long)v?String.valueOf((long)v):String.valueOf(v); }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> "";
        };
    }

    private static double num(Row row, int col) {
        if (col<0||row==null) return 0;
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c==null) return 0;
        return switch(c.getCellType()) {
            case NUMERIC -> c.getNumericCellValue();
            case STRING -> { try { yield Double.parseDouble(c.getStringCellValue().trim()); } catch(Exception e) { yield 0; } }
            default -> 0;
        };
    }

    private static boolean isEmpty(Row row) {
        for (int i=row.getFirstCellNum(); i<row.getLastCellNum(); i++) {
            Cell c = row.getCell(i);
            if (c!=null && c.getCellType()!=CellType.BLANK && !str(c).isEmpty()) return false;
        }
        return true;
    }
}
