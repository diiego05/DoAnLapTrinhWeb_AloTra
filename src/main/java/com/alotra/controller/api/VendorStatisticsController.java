package com.alotra.controller.api;

import com.alotra.dto.statistics.*;
import com.alotra.service.VendorStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/vendor/statistics")
@RequiredArgsConstructor
public class VendorStatisticsController {

    private final VendorStatisticsService vendorStatisticsService;

    // ================= 📊 Doanh thu =================
    @GetMapping("/revenue")
    public List<RevenueStatisticDTO> getRevenue(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return vendorStatisticsService.getRevenueStatistics(from, to);
    }

    @GetMapping("/revenue/export-excel")
    public ResponseEntity<byte[]> exportRevenueExcel(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        byte[] file = vendorStatisticsService.exportRevenueExcel(from, to);
        return buildExcelResponse(file, "revenue", from, to);
    }

    // ================= 🧋 Top sản phẩm =================
    @GetMapping("/products")
    public List<TopProductStatisticDTO> getTopProducts(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return vendorStatisticsService.getTopProducts(from, to);
    }

    @GetMapping("/products/export-excel")
    public ResponseEntity<byte[]> exportTopProductsExcel(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        byte[] file = vendorStatisticsService.exportTopProductsExcel(from, to);
        return buildExcelResponse(file, "top-products", from, to);
    }

    // ================= 👤 Top khách hàng =================
    @GetMapping("/customers")
    public List<TopCustomerStatisticDTO> getTopCustomers(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return vendorStatisticsService.getTopCustomers(from, to);
    }

    @GetMapping("/customers/export-excel")
    public ResponseEntity<byte[]> exportTopCustomersExcel(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        byte[] file = vendorStatisticsService.exportTopCustomersExcel(from, to);
        return buildExcelResponse(file, "top-customers", from, to);
    }

    // ================= Helper =================
    private ResponseEntity<byte[]> buildExcelResponse(byte[] file, String prefix, LocalDate from, LocalDate to) {
        String fromStr = from.format(DateTimeFormatter.BASIC_ISO_DATE);
        String toStr = to.format(DateTimeFormatter.BASIC_ISO_DATE);
        String filename = prefix + "-" + fromStr + "-" + toStr + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok().headers(headers).body(file);
    }
}
