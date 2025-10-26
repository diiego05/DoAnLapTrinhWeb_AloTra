package com.alotra.controller.api;

import com.alotra.dto.ShipperHistoryDTO;
import com.alotra.dto.ShipperRevenueDTO;
import com.alotra.dto.ShipperStatusCountDTO;
import com.alotra.dto.ShipperSummaryDTO;
import com.alotra.repository.ShipperRepository;
import com.alotra.service.ShipperStatisticsService;
import com.alotra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/shipper/statistics")
@RequiredArgsConstructor
public class ShipperStatisticsApiController {

    private final ShipperStatisticsService shipperStatisticsService;
    private final UserService userService;
    private final ShipperRepository shipperRepository;

    private Long getCurrentShipperId() {
        Long userId = userService.getCurrentUserId();
        Long shipperId = shipperRepository.findIdByUserId(userId);
        if (shipperId == null) {
            throw new IllegalStateException("Tài khoản hiện tại không phải shipper hợp lệ hoặc chưa được duyệt");
        }
        return shipperId;
    }

    // ====================== 📊 Tổng quan ======================
    @GetMapping("/summary")
    public ShipperSummaryDTO getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return shipperStatisticsService.getSummary(getCurrentShipperId(), from, to);
    }

    // ====================== 📈 Biểu đồ doanh thu ======================
    @GetMapping("/revenue")
    public List<ShipperRevenueDTO> getRevenueChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return shipperStatisticsService.getRevenueChart(getCurrentShipperId(), from, to);
    }

    // ====================== 🧾 Biểu đồ trạng thái ======================
    @GetMapping("/status-chart")
    public List<ShipperStatusCountDTO> getStatusChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return shipperStatisticsService.getStatusChart(getCurrentShipperId(), from, to);
    }

    // ====================== 🕒 Lịch sử thu nhập ======================
    @GetMapping("/history")
    public List<ShipperHistoryDTO> getHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return shipperStatisticsService.getHistory(getCurrentShipperId(), from, to);
    }

    // ====================== 📤 Xuất Excel ======================
    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportEarningsToExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        byte[] file = shipperStatisticsService.exportEarningsToExcel(getCurrentShipperId(), from, to);

        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = String.format("shipper-earnings-%s-%s.xlsx", fromStr, toStr);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .body(file);
    }
}
