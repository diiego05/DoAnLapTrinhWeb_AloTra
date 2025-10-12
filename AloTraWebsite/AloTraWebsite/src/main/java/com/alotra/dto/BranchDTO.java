package com.alotra.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchDTO {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String status;
}
