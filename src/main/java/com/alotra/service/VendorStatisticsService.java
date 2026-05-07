package com.alotra.service;

import com.alotra.dto.statistics.RevenueStatisticDTO;
import com.alotra.dto.statistics.TopCustomerStatisticDTO;
import com.alotra.dto.statistics.TopProductStatisticDTO;
import com.alotra.repository.OrderItemRepository;
import com.alotra.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorStatisticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;

    private Long getBranchIdForCurrentVendor() {
        Long vendorId = userService.getCurrentUserId();
        return orderRepository.findBranchIdByVendor(vendorId);
    }

    // ===================== 📊 Doanh thu =====================
    public List<RevenueStatisticDTO> getRevenueStatistics(LocalDate from, LocalDate to) {
        Long branchId = getBranchIdForCurrentVendor();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);

        List<Object[]> results = orderRepository.findRevenueStatisticsByBranchAndDateRange(branchId, start, end);
        return results.stream().map(row -> {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long totalOrders = ((Number) row[1]).longValue();
            BigDecimal totalRevenue = (BigDecimal) row[2];
            BigDecimal avgPerOrder = totalOrders > 0
                    ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;
            return new RevenueStatisticDTO(date, totalOrders, totalRevenue, avgPerOrder);
        }).collect(Collectors.toList());
    }

    public byte[] exportRevenueExcel(LocalDate from, LocalDate to) {
        List<RevenueStatisticDTO> data = getRevenueStatistics(from, to);
        return createExcelFile("Revenue Report", new String[]{"Ngày", "Số đơn", "Tổng doanh thu", "TB/đơn"}, data);
    }

    // ===================== 🧋 Top sản phẩm =====================
    public List<TopProductStatisticDTO> getTopProducts(LocalDate from, LocalDate to) {
        Long branchId = getBranchIdForCurrentVendor();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);

        List<Object[]> results = orderItemRepository.findTopProductsByBranchAndDateRange(branchId, start, end);
        return results.stream().map(row -> new TopProductStatisticDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).longValue(),
                (BigDecimal) row[3]
        )).toList();
    }

    public byte[] exportTopProductsExcel(LocalDate from, LocalDate to) {
        List<TopProductStatisticDTO> data = getTopProducts(from, to);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Top Products");
            int rowIdx = 0;

            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("Mã SP");
            header.createCell(1).setCellValue("Tên sản phẩm");
            header.createCell(2).setCellValue("Số lượng bán");
            header.createCell(3).setCellValue("Doanh thu");

            for (TopProductStatisticDTO dto : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getProductId());
                row.createCell(1).setCellValue(dto.getProductName());
                row.createCell(2).setCellValue(dto.getTotalQuantity());
                row.createCell(3).setCellValue(dto.getTotalRevenue().doubleValue());
            }

            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Excel Top sản phẩm", e);
        }
    }

    // ===================== 👤 Top khách hàng =====================
    public List<TopCustomerStatisticDTO> getTopCustomers(LocalDate from, LocalDate to) {
        Long branchId = getBranchIdForCurrentVendor();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);

        List<Object[]> results = orderRepository.findTopCustomersByBranchAndDateRange(branchId, start, end);
        return results.stream().map(row -> new TopCustomerStatisticDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).longValue(),
                (BigDecimal) row[3]
        )).toList();
    }

    public byte[] exportTopCustomersExcel(LocalDate from, LocalDate to) {
        List<TopCustomerStatisticDTO> data = getTopCustomers(from, to);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Top Customers");
            int rowIdx = 0;

            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("Mã KH");
            header.createCell(1).setCellValue("Tên khách hàng");
            header.createCell(2).setCellValue("Tổng đơn");
            header.createCell(3).setCellValue("Tổng chi tiêu");

            for (TopCustomerStatisticDTO dto : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getCustomerId());
                row.createCell(1).setCellValue(dto.getCustomerName());
                row.createCell(2).setCellValue(dto.getTotalOrders());
                row.createCell(3).setCellValue(dto.getTotalSpent().doubleValue());
            }

            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Excel Top khách hàng", e);
        }
    }

    // ===================== 📝 Helper =====================
    private byte[] createExcelFile(String sheetName, String[] headers, List<RevenueStatisticDTO> data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            int rowIdx = 0;

            Row header = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            for (RevenueStatisticDTO dto : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getDate().toString());
                row.createCell(1).setCellValue(dto.getTotalOrders());
                row.createCell(2).setCellValue(dto.getTotalRevenue().doubleValue());
                row.createCell(3).setCellValue(dto.getAvgPerOrder().doubleValue());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo Excel", e);
        }
    }
}
