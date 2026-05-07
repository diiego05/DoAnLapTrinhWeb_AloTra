package com.alotra.service;

import com.alotra.dto.ShipperHistoryDTO;
import com.alotra.dto.ShipperRevenueDTO;
import com.alotra.dto.ShipperStatusCountDTO;
import com.alotra.dto.ShipperSummaryDTO;
import com.alotra.repository.OrderRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipperStatisticsService {

    private final OrderRepository orderRepository;

    /**
     * 📊 Tổng quan (số đơn hoàn thành, đang giao, tổng thu nhập)
     */
    public ShipperSummaryDTO getSummary(Long shipperId, LocalDateTime from, LocalDateTime to) {
        long completed = orderRepository.countCompletedByShipper(shipperId, from, to);
        long inProgress = orderRepository.countInProgressByShipper(shipperId, from, to);
        BigDecimal totalShippingFee = orderRepository.sumShippingFeeByShipper(shipperId, from, to);
        BigDecimal earnings = totalShippingFee.multiply(BigDecimal.valueOf(0.7));

        return new ShipperSummaryDTO(completed, inProgress, earnings);
    }

    /**
     * 📈 Biểu đồ doanh thu theo ngày
     */
    public List<ShipperRevenueDTO> getRevenueChart(Long shipperId, LocalDateTime from, LocalDateTime to) {
        return orderRepository.getDailyRevenueForShipper(shipperId, from, to);
    }

    /**
     * 🧾 Biểu đồ trạng thái đơn hàng
     */
    public List<ShipperStatusCountDTO> getStatusChart(Long shipperId, LocalDateTime from, LocalDateTime to) {
        return orderRepository.getStatusCountForShipper(shipperId, from, to);
    }

    /**
     * 🕒 Lịch sử thu nhập
     */
    public List<ShipperHistoryDTO> getHistory(Long shipperId, LocalDateTime from, LocalDateTime to) {
        return orderRepository.getHistoryForShipper(shipperId, from, to);
    }



    public byte[] exportEarningsToExcel(Long shipperId, LocalDateTime from, LocalDateTime to) {
        List<ShipperHistoryDTO> history = getHistory(shipperId, from, to);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Earnings");
            int rowIdx = 0;

            // ===== 📌 Header =====
            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("Mã đơn");
            header.createCell(1).setCellValue("Ngày giao");
            header.createCell(2).setCellValue("Phí vận chuyển");
            header.createCell(3).setCellValue("Thu nhập");

            // Style header (tuỳ chọn)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (Cell cell : header) cell.setCellStyle(headerStyle);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            BigDecimal totalEarnings = BigDecimal.ZERO;

            // ===== 📝 Dữ liệu =====
            for (ShipperHistoryDTO dto : history) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getOrderCode());
                row.createCell(1).setCellValue(dto.getDeliveredAt().format(dtf));
                row.createCell(2).setCellValue(dto.getShippingFee().doubleValue());
                row.createCell(3).setCellValue(dto.getEarnings().doubleValue());
                totalEarnings = totalEarnings.add(dto.getEarnings());
            }

            // ===== ➕ Dòng tổng =====
            if (!history.isEmpty()) {
                Row totalRow = sheet.createRow(rowIdx++);
                totalRow.createCell(2).setCellValue("Tổng:");
                totalRow.createCell(3).setCellValue(totalEarnings.doubleValue());
                Font boldFont = workbook.createFont();
                boldFont.setBold(true);
                CellStyle totalStyle = workbook.createCellStyle();
                totalStyle.setFont(boldFont);
                totalRow.getCell(2).setCellStyle(totalStyle);
                totalRow.getCell(3).setCellStyle(totalStyle);
            }

            // Auto resize columns
            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("❌ Lỗi xuất Excel", e);
        }
    }

}
