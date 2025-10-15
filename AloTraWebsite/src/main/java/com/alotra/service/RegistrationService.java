package com.alotra.service;

import com.alotra.dto.BranchRegisterDTO;
import com.alotra.dto.BranchRequestDTO;
import com.alotra.dto.ShipperRegisterDTO;
import com.alotra.entity.*;
import com.alotra.entity.request.BranchRegistrationRequest;
import com.alotra.entity.request.BranchRequestType;
import com.alotra.entity.request.RequestStatus;
import com.alotra.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final BranchRegistrationRequestRepository branchReqRepo;
    private final BranchRepository branchRepo;
    private final ShippingCarrierRepository carrierRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final ShipperRepository shipperRepo;
    private final RoleRepository roleRepo;

    // ================== 🏪 USER - BRANCH ==================
    @Transactional
    public BranchRegistrationRequest createBranchRequest(Long userId, BranchRegisterDTO dto) {
        boolean hasPending = branchReqRepo.existsByUserIdAndStatus(userId, RequestStatus.PENDING);
        if (hasPending) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã có yêu cầu đang chờ duyệt.");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User không tồn tại"));

        BranchRegistrationRequest newReq = new BranchRegistrationRequest();
        newReq.setUser(user);
        newReq.setType(dto.getType());
        newReq.setStatus(RequestStatus.PENDING);
        newReq.setCreatedAt(LocalDateTime.now());


        if (dto.getType() == BranchRequestType.JOIN) {
            if (dto.getBranchId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu branchId");
            }
            Branch branch = branchRepo.findById(dto.getBranchId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi nhánh không tồn tại"));
            newReq.setBranch(branch);
            newReq.setName(branch.getName());
            newReq.setPhone(branch.getPhone());
            newReq.setAddress(branch.getAddress());
        } else {
            if (dto.getName() == null || dto.getName().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chi nhánh không được để trống");
            if (dto.getPhone() == null || dto.getPhone().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không được để trống");
            if (dto.getAddress() == null || dto.getAddress().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ không được để trống");

            newReq.setName(dto.getName().trim());
            newReq.setPhone(dto.getPhone().trim());
            newReq.setAddress(dto.getAddress().trim());
        }

        return branchReqRepo.save(newReq);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                   .trim()
                   .replaceAll("[^a-z0-9\\s-]", "") // bỏ ký tự đặc biệt
                   .replaceAll("\\s+", "-");       // thay khoảng trắng bằng dấu gạch ngang
    }
    public List<BranchRegistrationRequest> getMyBranchRequests(Long userId) {
        return branchReqRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public BranchRegistrationRequest updateBranchRequest(Long userId, Long id, BranchRegisterDTO dto) {
        BranchRegistrationRequest req = branchReqRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yêu cầu không tồn tại"));

        if (!req.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền sửa yêu cầu này.");
        }

        if (req.getStatus() == RequestStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đã được duyệt, không thể sửa.");
        }

        if (req.getType() == BranchRequestType.JOIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu tham gia chi nhánh không thể sửa.");
        }

        req.setName(dto.getName().trim());
        req.setPhone(dto.getPhone().trim());
        req.setAddress(dto.getAddress().trim());
        req.setUpdatedAt(LocalDateTime.now());

        if (req.getStatus() == RequestStatus.REJECTED) {
            req.setStatus(RequestStatus.PENDING);
        }

        return branchReqRepo.save(req);
    }

    @Transactional
    public void deleteBranchRequest(Long userId, Long requestId) {
        BranchRegistrationRequest req = branchReqRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu"));

        if (req.getStatus() != RequestStatus.PENDING && req.getStatus() != RequestStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đã được phê duyệt, không thể xóa");
        }

        if (!req.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa yêu cầu này");
        }

        branchReqRepo.delete(req);
    }

    // ================== 🏪 ADMIN - BRANCH ==================
    @Transactional
    public BranchRegistrationRequest adminApproveBranch(Long id, String note) {
        BranchRegistrationRequest req = branchReqRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yêu cầu không tồn tại"));

        if (req.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đã được xử lý.");
        }

        if (req.getType() == BranchRequestType.CREATE) {
            if (branchRepo.existsByName(req.getName().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên chi nhánh đã tồn tại.");
            }

            Branch branch = new Branch();
            branch.setName(req.getName().trim());
            branch.setPhone(req.getPhone().trim());
            branch.setAddress(req.getAddress().trim());
            branch.setStatus("ACTIVE");
            branch.setManager(req.getUser());
            branch.setSlug(generateSlug(req.getName()));
            Branch saved = branchRepo.save(branch);
            req.setBranch(saved);
        }

        Role vendorRole = roleRepo.findByCode("VENDOR")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role VENDOR không tồn tại"));
        req.getUser().setRole(vendorRole);
        userRepo.save(req.getUser());

        req.setStatus(RequestStatus.APPROVED);
        req.setAdminNote(note);
        req.setUpdatedAt(LocalDateTime.now());
        branchReqRepo.save(req);

        // 📩 Thông báo
        notificationService.create(
                req.getUser().getId(),
                "BRANCH_APPROVAL",
                "Yêu cầu chi nhánh đã được duyệt",
                "Yêu cầu tạo chi nhánh của bạn đã được duyệt thành công ✅",
                "Branch",
                req.getId()
        );

        return req;
    }

    public BranchRequestDTO getBranchRequestById(Long id) {
        BranchRegistrationRequest entity = branchReqRepo.findById(id)
                .orElse(null);
        if (entity == null) return null;

        BranchRequestDTO dto = new BranchRequestDTO();
        dto.setId(entity.getId());
        dto.setBranchName(entity.getName());
        dto.setAddress(entity.getAddress());
        dto.setPhone(entity.getPhone());
        dto.setStatus(entity.getStatus());
        dto.setNote(entity.getAdminNote());
        dto.setCreatedAt(entity.getCreatedAt());

        if (entity.getUser() != null) {
            dto.setRequesterName(entity.getUser().getFullName());
            dto.setRequesterEmail(entity.getUser().getEmail());
            dto.setRequesterPhone(entity.getUser().getPhone());
            dto.setGender(entity.getUser().getGender());
            dto.setIdCardNumber(entity.getUser().getIdCardNumber());
            dto.setDob(entity.getUser().getDateOfBirth());
        }

        return dto;
    }


    @Transactional
    public BranchRegistrationRequest adminRejectBranch(Long id, String note) {
        BranchRegistrationRequest req = branchReqRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yêu cầu không tồn tại"));

        if (req.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đã được xử lý.");
        }

        req.setStatus(RequestStatus.REJECTED);
        req.setAdminNote(note);
        req.setUpdatedAt(LocalDateTime.now());
        branchReqRepo.save(req);

        // 📩 Thông báo
        notificationService.create(
                req.getUser().getId(),
                "BRANCH_REJECT",
                "Yêu cầu chi nhánh bị từ chối",
                "Yêu cầu tạo chi nhánh của bạn đã bị từ chối ❌.\nLý do: " + (note != null ? note : "Không có ghi chú"),
                "Branch",
                req.getId()
        );

        return req;
    }

    public List<BranchRegistrationRequest> adminSearchBranchRequests(RequestStatus status,
                                                                     BranchRequestType type,
                                                                     String keyword) {
        return branchReqRepo.searchRequests(status, type, keyword);
    }

    // ================== 🚚 USER - SHIPPER ==================
    @Transactional
    public Shipper createOrUpdateShipperRequest(Long userId, ShipperRegisterDTO dto) {
        boolean hasPending = shipperRepo.existsByUserIdAndStatus(userId, "PENDING");
        if (hasPending) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã có yêu cầu shipper đang chờ duyệt.");
        }

        User user = userRepo.getReferenceById(userId);
        Shipper shipper = new Shipper();
        shipper.setUser(user);

        if (dto.getCarrierId() != null) {
            ShippingCarrier carrier = carrierRepo.findById(dto.getCarrierId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhà vận chuyển không tồn tại"));
            shipper.setCarrier(carrier);
        }

        shipper.setWard(dto.getWard());
        shipper.setDistrict(dto.getDistrict());
        shipper.setCity(dto.getCity());
        shipper.setVehicleType(dto.getVehicleType());
        shipper.setVehiclePlate(dto.getVehiclePlate());
        shipper.setStatus("PENDING");
        shipper.setCreatedAt(LocalDateTime.now());

        return shipperRepo.save(shipper);
    }

    @Transactional
    public Shipper updateShipperRequest(Long userId, Long shipperId, ShipperRegisterDTO dto) {
        Shipper shipper = shipperRepo.findById(shipperId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yêu cầu shipper không tồn tại"));

        if (!shipper.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền sửa yêu cầu này.");
        }

        if ("APPROVED".equals(shipper.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đã được phê duyệt, không thể sửa.");
        }

        if (dto.getCarrierId() != null) {
            ShippingCarrier carrier = carrierRepo.findById(dto.getCarrierId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhà vận chuyển không tồn tại"));
            shipper.setCarrier(carrier);
        }

        shipper.setWard(dto.getWard());
        shipper.setDistrict(dto.getDistrict());
        shipper.setCity(dto.getCity());
        shipper.setVehicleType(dto.getVehicleType());
        shipper.setVehiclePlate(dto.getVehiclePlate());
        shipper.setUpdatedAt(LocalDateTime.now());

        if ("REJECTED".equals(shipper.getStatus())) {
            shipper.setStatus("PENDING");
        }

        return shipperRepo.save(shipper);
    }

    @Transactional
    public void deleteShipperRequest(Long userId, Long shipperId) {
        Shipper shipper = shipperRepo.findById(shipperId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yêu cầu shipper không tồn tại"));

        if (!shipper.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa yêu cầu này.");
        }

        if (!"PENDING".equals(shipper.getStatus()) && !"REJECTED".equals(shipper.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đã được phê duyệt, không thể xóa.");
        }

        shipperRepo.delete(shipper);
    }

    public Shipper getMyShipper(Long userId) {
        User user = userRepo.getReferenceById(userId);
        return shipperRepo.findByUser(user).orElse(null);
    }

    public List<Shipper> getPendingShippers() {
        return shipperRepo.findByStatus("PENDING");
    }

    // ================== 🚚 ADMIN - SHIPPER ==================
    @Transactional
    public Shipper adminApproveShipper(Long id, String note) {
        Shipper shipper = shipperRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yêu cầu không tồn tại"));

        if (!"PENDING".equals(shipper.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đã được xử lý.");
        }

        Role shipperRole = roleRepo.findByCode("SHIPPER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy role SHIPPER"));

        User user = shipper.getUser();
        user.setRole(shipperRole);
        userRepo.save(user);

        shipper.setStatus("APPROVED");
        shipper.setAdminNote(note);
        shipper.setUpdatedAt(LocalDateTime.now());
        shipperRepo.save(shipper);

        notificationService.create(
                user.getId(),
                "SHIPPER_APPROVAL",
                "Yêu cầu Shipper đã được duyệt",
                "Yêu cầu trở thành Shipper của bạn đã được duyệt thành công ✅",
                "Shipper",
                shipper.getId()
        );

        return shipper;
    }

    @Transactional
    public Shipper adminRejectShipper(Long id, String note) {
        Shipper shipper = shipperRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yêu cầu không tồn tại"));

        if (!"PENDING".equals(shipper.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đã được xử lý.");
        }

        shipper.setStatus("REJECTED");
        shipper.setAdminNote(note);
        shipper.setUpdatedAt(LocalDateTime.now());
        shipperRepo.save(shipper);

        notificationService.create(
                shipper.getUser().getId(),
                "SHIPPER_REJECT",
                "Yêu cầu Shipper bị từ chối",
                "Yêu cầu trở thành Shipper của bạn đã bị từ chối ❌.\nLý do: " + (note != null ? note : "Không có ghi chú"),
                "Shipper",
                shipper.getId()
        );

        return shipper;
    }

    public List<Shipper> adminSearchShippers(String status, String keyword) {
        return shipperRepo.searchShippers(status, keyword);
    }

    // ================== ⚡ WRAPPER API CŨ ==================
    @Transactional
    public BranchRegistrationRequest approveBranchRequest(Long id) {
        return adminApproveBranch(id, null);
    }

    @Transactional
    public BranchRegistrationRequest rejectBranchRequest(Long id) {
        return adminRejectBranch(id, null);
    }

    @Transactional
    public Shipper approveShipper(Long id) {
        return adminApproveShipper(id, null);
    }

    @Transactional
    public Shipper rejectShipper(Long id) {
        return adminRejectShipper(id, null);
    }

    public List<BranchRegistrationRequest> getPendingBranchRequests() {
        return branchReqRepo.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING);
    }
}
