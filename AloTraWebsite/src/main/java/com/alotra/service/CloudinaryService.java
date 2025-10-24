package com.alotra.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    // 🖼️ Phương thức cũ — chỉ dùng cho ảnh (không thay đổi)
    public String uploadFile(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return result.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("❌ Lỗi tải ảnh lên Cloudinary", e);
        }
    }

    public String uploadMediaFile(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            String originalName = file.getOriginalFilename();
            String resourceType = "image";

            if (contentType != null && contentType.startsWith("video")) {
                resourceType = "video";
            } else if (originalName != null && originalName.toLowerCase().endsWith(".mp4")) {
                resourceType = "video";
            } else if (contentType == null || !contentType.startsWith("image")) {
                throw new RuntimeException("❌ Chỉ hỗ trợ ảnh hoặc video.");
            }

            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "folder", "reviews"
            );

            System.out.println("🚀 Uploading with resource_type=" + resourceType);

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
            return result.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException("❌ Lỗi tải media lên Cloudinary", e);
        }
    }

}
