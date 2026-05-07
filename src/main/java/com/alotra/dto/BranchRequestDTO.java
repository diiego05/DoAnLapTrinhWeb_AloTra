package com.alotra.dto;

import com.alotra.entity.request.BranchRequestType;
import com.alotra.entity.request.RequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class BranchRequestDTO {
    private Long id;
    private BranchRequestType type;
    private RequestStatus status;

    // 🏢 Thông tin chi nhánh
    private String branchName;
    private String address;
    private String phone;

    // 📝 Ghi chú của admin
    private String note;

    // ⏱️ Thời gian
    private LocalDateTime createdAt;

    // 🧍 Thông tin người gửi yêu cầu
    private String requesterName;
    private String requesterEmail;
    private String requesterPhone;
    private String idCardNumber;
    private String gender;
    private LocalDate dob;
}
