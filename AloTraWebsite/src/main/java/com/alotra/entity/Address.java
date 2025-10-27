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

    @Column(name = "City")
    private String city;

    @Column(name = "Label")
    private String label; // Nhãn địa chỉ (VD: "Nhà riêng", "Văn phòng", etc.)

    @Column(name = "IsDefault", nullable = false)
    private boolean isDefault = false;

    // 🆕 Toạ độ địa lý
    @Column(name = "Latitude")
    private Double latitude;

    @Column(name = "Longitude")
    private Double longitude;

    // Quan hệ nhiều-1: Nhiều địa chỉ thuộc về 1 user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User user;

    // Trả về chuỗi địa chỉ đầy đủ (line1, ward, city)
    public String getFullAddress() {
        String result = String.format("%s, %s, %s",
                line1 != null ? line1 : "",
                ward != null ? ward : "",
                city != null ? city : ""
        );
        // normalize duplicated commas/spaces
        return result.replaceAll(", ", ", ")
                .replaceAll(", ,", ",")
                .replaceAll("^, |, $", "")
                .trim();
    }

    // 🌐 Trả về địa chỉ đầy đủ kèm quốc gia (dùng cho geocoding)
    public String getFullAddressForGeocoding() {
        StringBuilder sb = new StringBuilder();

        if (line1 != null && !line1.trim().isEmpty()) {
            sb.append(line1.trim());
        }
        if (ward != null && !ward.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward.trim());
        }
        if (city != null && !city.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city.trim());
        }
        if (sb.length() > 0) sb.append(", Vietnam");

        String result = sb.toString();
        System.out.println("🗺️ [DEBUG] Full address for geocoding: " + result);
        return result;
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

    @Column(name = "City")
    private String city;

    @Column(name = "Label")
    private String label; // Nhãn địa chỉ (VD: "Nhà riêng", "Văn phòng", etc.)

    @Column(name = "IsDefault", nullable = false)
    private boolean isDefault = false;

    // 🆕 Toạ độ địa lý
    @Column(name = "Latitude")
    private Double latitude;

    @Column(name = "Longitude")
    private Double longitude;

    // Quan hệ nhiều-1: Nhiều địa chỉ thuộc về 1 user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User user;

    // Trả về chuỗi địa chỉ đầy đủ (line1, ward, city)
    public String getFullAddress() {
        String result = String.format("%s, %s, %s",
                line1 != null ? line1 : "",
                ward != null ? ward : "",
                city != null ? city : ""
        );
        // normalize duplicated commas/spaces
        return result.replaceAll(", ", ", ")
                .replaceAll(", ,", ",")
                .replaceAll("^, |, $", "")
                .trim();
    }

    // 🌐 Trả về địa chỉ đầy đủ kèm quốc gia (dùng cho geocoding)
    public String getFullAddressForGeocoding() {
        StringBuilder sb = new StringBuilder();

        if (line1 != null && !line1.trim().isEmpty()) {
            sb.append(line1.trim());
        }
        if (ward != null && !ward.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward.trim());
        }
        if (city != null && !city.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city.trim());
        }
        if (sb.length() > 0) sb.append(", Vietnam");

        String result = sb.toString();
        System.out.println("🗺️ [DEBUG] Full address for geocoding: " + result);
        return result;
    }

}