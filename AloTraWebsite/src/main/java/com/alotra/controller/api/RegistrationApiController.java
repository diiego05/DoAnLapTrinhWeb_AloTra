package com.alotra.controller.api;

import com.alotra.dto.*;
import com.alotra.entity.Branch;
import com.alotra.entity.Shipper;
import com.alotra.entity.request.BranchRegistrationRequest;
import com.alotra.service.AuthService;
import com.alotra.service.BranchService;
import com.alotra.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
@Validated
public class RegistrationApiController {

    private final RegistrationService registrationService;
    private final AuthService authService;
    private final BranchService branchService;

    // ================= 🏪 BRANCH =================

    @PostMapping("/branch")
    public ResponseEntity<?> submitBranch(@RequestBody @Validated BranchRegisterDTO dto) {
        Long userId = authService.getCurrentUserId();
        try {
            registrationService.createBranchRequest(userId, dto);
            return ResponseEntity.ok("✅ Gửi yêu cầu thành công.");
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        }
    }

    @GetMapping("/branch/{id}")
    public ResponseEntity<BranchRequestDTO> getBranchDetail(@PathVariable Long id) {
        var req = registrationService.getBranchRequestById(id);
        if (req == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(req);
    }


    @PutMapping("/branch/{id}")
    public ResponseEntity<?> updateBranchRequest(@PathVariable Long id,
                                                 @RequestBody @Validated BranchRegisterDTO dto) {
        Long userId = authService.getCurrentUserId();
        try {
            registrationService.updateBranchRequest(userId, id, dto);
            return ResponseEntity.ok("✏️ Cập nhật yêu cầu thành công.");
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        }
    }

    @DeleteMapping("/branch/{id}")
    public ResponseEntity<?> deleteBranchRequest(@PathVariable Long id) {
        Long userId = authService.getCurrentUserId();
        try {
            registrationService.deleteBranchRequest(userId, id);
            return ResponseEntity.ok("🗑️ Xóa yêu cầu thành công.");
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        }
    }

    @GetMapping("/branch/my-requests")
    public ResponseEntity<List<BranchRequestDTO>> getMyBranchRequests() {
        Long userId = authService.getCurrentUserId();
        List<BranchRequestDTO> list = registrationService.getMyBranchRequests(userId)
                .stream()
                .map(this::convertToBranchRequestDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    // ✅ API dùng chung cho lọc branch theo trạng thái (thay vì pending riêng lẻ)
    @GetMapping("/branch")
    public ResponseEntity<List<BranchRequestDTO>> getBranchRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {

        var list = registrationService.adminSearchBranchRequests(
                        status != null ? Enum.valueOf(com.alotra.entity.request.RequestStatus.class, status) : null,
                        type != null ? Enum.valueOf(com.alotra.entity.request.BranchRequestType.class, type) : null,
                        keyword
                )
                .stream()
                .map(this::convertToBranchRequestDTO)
                .toList();

        return ResponseEntity.ok(list);
    }

    @PutMapping("/branch/{id}/approve")
    public ResponseEntity<String> approveBranch(@PathVariable Long id) {
        registrationService.approveBranchRequest(id);
        return ResponseEntity.ok("✅ Yêu cầu đã được duyệt.");
    }

    @PutMapping("/branch/{id}/reject")
    public ResponseEntity<String> rejectBranch(@PathVariable Long id) {
        registrationService.rejectBranchRequest(id);
        return ResponseEntity.ok("❌ Yêu cầu đã bị từ chối.");
    }

    private BranchRequestDTO convertToBranchRequestDTO(BranchRegistrationRequest req) {
        BranchRequestDTO dto = new BranchRequestDTO();
        dto.setId(req.getId());
        dto.setType(req.getType());
        dto.setStatus(req.getStatus());
        dto.setCreatedAt(req.getCreatedAt());
        dto.setNote(req.getAdminNote() != null ? req.getAdminNote() : "");

        // 🏢 Thông tin chi nhánh
        if (req.getBranch() != null) {
            dto.setBranchName(req.getBranch().getName() != null ? req.getBranch().getName() : "");
            dto.setAddress(req.getBranch().getAddress() != null ? req.getBranch().getAddress() : "");
            dto.setPhone(req.getBranch().getPhone() != null ? req.getBranch().getPhone() : "");
        } else {
            dto.setBranchName(req.getName() != null ? req.getName() : "");
            dto.setAddress(req.getAddress() != null ? req.getAddress() : "");
            dto.setPhone(req.getPhone() != null ? req.getPhone() : "");
        }

        // 🧍 Thông tin người gửi yêu cầu
        if (req.getUser() != null) {
            dto.setRequesterName(req.getUser().getFullName());
            dto.setRequesterEmail(req.getUser().getEmail());
            dto.setRequesterPhone(req.getUser().getPhone());
            dto.setIdCardNumber(req.getUser().getIdCardNumber());
            dto.setGender(req.getUser().getGender());
            dto.setDob(req.getUser().getDateOfBirth());
        }

        return dto;
    }


    // ================= 🚚 SHIPPER =================

    @PostMapping("/shipper")
    public ResponseEntity<?> submitShipper(@RequestBody @Validated ShipperRegisterDTO dto) {
        Long userId = authService.getCurrentUserId();
        try {
            Shipper shipper = registrationService.createOrUpdateShipperRequest(userId, dto);
            return ResponseEntity.ok(convertToDTO(shipper));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        }
    }

    @PutMapping("/shipper/{id}")
    public ResponseEntity<?> updateShipper(@PathVariable Long id,
                                           @RequestBody @Validated ShipperRegisterDTO dto) {
        Long userId = authService.getCurrentUserId();
        try {
            Shipper shipper = registrationService.updateShipperRequest(userId, id, dto);
            return ResponseEntity.ok(convertToDTO(shipper));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        }
    }

    @DeleteMapping("/shipper/{id}")
    public ResponseEntity<?> deleteShipper(@PathVariable Long id) {
        Long userId = authService.getCurrentUserId();
        try {
            registrationService.deleteShipperRequest(userId, id);
            return ResponseEntity.ok("🗑️ Xóa yêu cầu shipper thành công.");
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        }
    }

    @GetMapping("/shipper/my-request")
    public ResponseEntity<ShipperDTO> getMyShipperRequest() {
        Long userId = authService.getCurrentUserId();
        Shipper shipper = registrationService.getMyShipper(userId);
        if (shipper == null) return ResponseEntity.noContent().build();
        ShipperDTO dto = convertToDTO(shipper);
        return ResponseEntity.ok(dto);
    }

    // ✅ API dùng chung cho lọc shipper theo trạng thái
    @GetMapping("/shipper")
    public ResponseEntity<List<ShipperDTO>> getShipperRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        var list = registrationService.adminSearchShippers(status, keyword)
                .stream()
                .map(this::convertToDTO)
                .toList();

        return ResponseEntity.ok(list);
    }

    @PutMapping("/shipper/{id}/approve")
    public ResponseEntity<Shipper> approveShipper(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.approveShipper(id));
    }

    @PutMapping("/shipper/{id}/reject")
    public ResponseEntity<Shipper> rejectShipper(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.rejectShipper(id));
    }

    private ShipperDTO convertToDTO(Shipper shipper) {
        ShipperDTO dto = new ShipperDTO();
        dto.setId(shipper.getId());
        dto.setStatus(shipper.getStatus());
        dto.setWard(shipper.getWard());
        dto.setDistrict(shipper.getDistrict());
        dto.setCity(shipper.getCity());
        dto.setVehicleType(shipper.getVehicleType());
        dto.setVehiclePlate(shipper.getVehiclePlate());
        dto.setCreatedAt(shipper.getCreatedAt());
        dto.setUpdatedAt(shipper.getUpdatedAt());

        // 🧍 Thông tin người dùng
        if (shipper.getUser() != null) {
            dto.setRequesterName(shipper.getUser().getFullName());
            dto.setRequesterEmail(shipper.getUser().getEmail());
            dto.setRequesterPhone(shipper.getUser().getPhone());
            dto.setIdCardNumber(shipper.getUser().getIdCardNumber());
            dto.setGender(shipper.getUser().getGender());
            dto.setDob(shipper.getUser().getDateOfBirth() != null ? shipper.getUser().getDateOfBirth().toString() : null);
        }

        // 🏢 Thông tin nhà vận chuyển
        if (shipper.getCarrier() != null) {
            dto.setCarrierId(shipper.getCarrier().getId());
            dto.setCarrierName(shipper.getCarrier().getName());
        } else {
            dto.setCarrierId(null);
            dto.setCarrierName(null);
        }

        dto.setAdminNote(shipper.getAdminNote());
        return dto;
    }

    // ================= 📜 LIST BRANCHES =================
    @GetMapping("/list-branches")
    public ResponseEntity<List<BranchDTO>> listBranches() {
        List<BranchDTO> list = branchService.getAllBranches()
                .stream()
                .map(branch -> {
                    BranchDTO dto = new BranchDTO();
                    dto.setId(branch.getId());
                    dto.setName(branch.getName());
                    dto.setAddress(branch.getAddress());
                    dto.setPhone(branch.getPhone());
                    dto.setStatus(branch.getStatus());
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(list);
    }

    // ================= 🏪 ADMIN - BRANCH =================
    @PutMapping("/admin/branch/{id}/approve")
    public ResponseEntity<?> adminApproveBranch(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        try {
            registrationService.adminApproveBranch(id, note);
            return ResponseEntity.ok("✅ Yêu cầu đã được duyệt");
        } catch (ResponseStatusException ex) {
            // 👉 Trường hợp bạn chủ động ném lỗi (HttpStatus.CONFLICT, NOT_FOUND, ...)
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (Exception ex) {
            // 👉 Trường hợp lỗi hệ thống, in stacktrace để debug
            ex.printStackTrace();
            return ResponseEntity.internalServerError().body("❌ Lỗi duyệt yêu cầu chi nhánh: " + ex.getMessage());
        }
    }


    @PutMapping("/admin/branch/{id}/reject")
    public ResponseEntity<?> adminRejectBranch(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        registrationService.adminRejectBranch(id, note);
        return ResponseEntity.ok("❌ Yêu cầu đã bị từ chối");
    }

    // ================= 🚚 ADMIN - SHIPPER =================
    @PutMapping("/admin/shipper/{id}/approve")
    public ResponseEntity<?> adminApproveShipper(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        registrationService.adminApproveShipper(id, note);
        return ResponseEntity.ok("✅ Yêu cầu Shipper đã được duyệt");
    }

    @PutMapping("/admin/shipper/{id}/reject")
    public ResponseEntity<?> adminRejectShipper(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        registrationService.adminRejectShipper(id, note);
        return ResponseEntity.ok("❌ Yêu cầu Shipper đã bị từ chối");
    }
}
