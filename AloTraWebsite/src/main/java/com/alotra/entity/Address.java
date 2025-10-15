/*package com.alotra.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "Recipient", nullable = false)
    private String recipient;

    @Column(name = "Phone", nullable = false)
    private String phone;

    @Column(name = "Line1", nullable = false)
    private String line1;

    @Column(name = "Ward")
    private String ward;

    @Column(name = "District")
    private String district;

    @Column(name = "City")
    private String city;

    @Column(name = "IsDefault", nullable = false)
    private boolean isDefault;

    // Mối quan hệ nhiều-một: Nhiều địa chỉ thuộc về một User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User user;

    // Phương thức tiện ích để hiển thị địa chỉ đầy đủ
    public String getFullAddress() {
        return String.format("%s, %s, %s, %s", line1, ward, district, city);
    }
}*/
package com.alotra.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "Recipient", nullable = false)
    private String recipient; // Người nhận hàng

    @Column(name = "Phone", nullable = false)
    private String phone;

    @Column(name = "Line1", nullable = false)
    private String line1; // Địa chỉ chi tiết (số nhà, tên đường)

    @Column(name = "Ward")
    private String ward;

    @Column(name = "District")
    private String district;

    @Column(name = "City")
    private String city;

    @Column(name = "IsDefault", nullable = false)
    private boolean isDefault = false;

    // Quan hệ nhiều-1: Nhiều địa chỉ thuộc về 1 user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User user;

    // Trả về chuỗi địa chỉ đầy đủ
    public String getFullAddress() {
        return String.format("%s, %s, %s, %s",
                line1 != null ? line1 : "",
                ward != null ? ward : "",
                district != null ? district : "",
                city != null ? city : ""
        ).replaceAll(", ,", ",");
    }

}
