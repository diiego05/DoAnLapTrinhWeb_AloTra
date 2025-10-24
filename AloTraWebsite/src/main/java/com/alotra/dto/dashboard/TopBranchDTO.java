package com.alotra.dto.dashboard;

import java.math.BigDecimal;

public class TopBranchDTO {

    private String branchName;
    private BigDecimal revenue;

    public TopBranchDTO(String branchName, BigDecimal revenue) {
        this.branchName = branchName;
        this.revenue = revenue;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}
