package com.alotra.dto;

import com.alotra.entity.request.BranchRequestType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchRegisterDTO {

    @NotNull(message = "Loại yêu cầu không được để trống")
    private BranchRequestType type;   // CREATE | JOIN

    private Long branchId;            // nếu JOIN

    // ✅ Tên chi nhánh khi tạo mới
    @Size(max = 100, message = "Tên chi nhánh tối đa 100 ký tự")
    private String name;

    // ✅ Số điện thoại khi tạo mới
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    // ✅ Địa chỉ chi nhánh khi tạo mới
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;


    private Double latitude;
    private Double longitude;

    // ✅ Ghi chú (tùy chọn)
    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String note;
}
