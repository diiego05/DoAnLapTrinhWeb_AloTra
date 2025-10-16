package com.alotra.service;

import com.alotra.config.VnPayConfig;
import com.alotra.utils.HmacSHA512;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VnPayService {

    private final VnPayConfig config;

    public VnPayService(VnPayConfig config) {
        this.config = config;
    }

    public String createPaymentUrl(HttpServletRequest request, long amount, String orderId) {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", config.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderId);

        String orderInfo = "Thanh toan don hang #" + orderId;
        orderInfo = Normalizer.normalize(orderInfo, Normalizer.Form.NFC);
        vnp_Params.put("vnp_OrderInfo", orderInfo);

        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", config.getReturnUrl());

        String ipAddr = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ipAddr)) ipAddr = "127.0.0.1";
        vnp_Params.put("vnp_IpAddr", ipAddr);

        // Thời gian theo múi giờ VN
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Build hashData (⚠️ phải encode value trước khi hash) + query string
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnp_Params.get(fieldName);

            // ⚠️ Encode giá trị theo chuẩn UTF-8
            String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8);

            hashData.append(fieldName).append('=').append(encodedValue);
            query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                 .append('=')
                 .append(encodedValue);

            if (i < fieldNames.size() - 1) {
                hashData.append('&');
                query.append('&');
            }
        }

        // Sinh chữ ký HMAC SHA512
        String vnp_SecureHash = HmacSHA512.hmacSHA512(config.getHashSecret().trim(), hashData.toString());

        // Gắn SecureHash vào URL
        query.append("&vnp_SecureHashType=HmacSHA512");
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        String finalUrl = config.getPayUrl() + "?" + query;

        // Debug log
        System.out.println("=========== VNPay CREATE PAYMENT DEBUG ===========");
        System.out.println("HASH DATA:\n" + hashData);
        System.out.println("SECURE HASH:\n" + vnp_SecureHash);
        System.out.println("FINAL URL:\n" + finalUrl);
        System.out.println("SECRET KEY LENGTH: " + config.getHashSecret().trim().length());
        System.out.println("RETURN URL CONFIG:\n" + config.getReturnUrl());
        System.out.println("=================================================");

        return finalUrl;
    }

    public boolean validateSignature(Map<String, String> params) {
        // Sao chép map để không mutate params gốc
        Map<String, String> fields = new HashMap<>(params);

        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        // Lọc bỏ giá trị null hoặc rỗng
        fields.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = fields.get(fieldName);
            String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8);

            hashData.append(fieldName).append('=').append(encodedValue);
            if (i < fieldNames.size() - 1) hashData.append('&');
        }

        String signValue = HmacSHA512.hmacSHA512(config.getHashSecret().trim(), hashData.toString());

        System.out.println("=========== VNPay VALIDATE SIGNATURE DEBUG ===========");
        System.out.println("HASH DATA VERIFY:\n" + hashData);
        System.out.println("MY SIGN VALUE:\n" + signValue);
        System.out.println("VNPAY SIGN VALUE:\n" + vnp_SecureHash);
        System.out.println("SECRET KEY LENGTH: " + config.getHashSecret().trim().length());
        System.out.println("=====================================================");

        return signValue.equals(vnp_SecureHash);
    }
}
