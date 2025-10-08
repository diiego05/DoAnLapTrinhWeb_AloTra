package com.alotra.repository;

import com.alotra.entity.OtpCode;
import com.alotra.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    // Tìm mã OTP theo user, code và type
    Optional<OtpCode> findByUserAndCodeAndType(User user, String code, String type);

    /**
     * Câu query tùy chỉnh để tìm một mã OTP hợp lệ.
     * Hợp lệ nghĩa là:
     * - Mã OTP đúng.
     * - Đúng người dùng, đúng loại (REGISTER).
     * - Chưa được sử dụng (usedAt IS NULL).
     * - Chưa hết hạn (expiresAt > thời gian hiện tại).
     */
    @Query("SELECT o FROM OtpCode o WHERE o.user.id = :userId AND o.code = :code AND o.type = :type " +
           "AND o.usedAt IS NULL AND o.expiresAt > :now")
    Optional<OtpCode> findValidOtp(@Param("userId") Long userId,
                                   @Param("code") String code,
                                   @Param("type") String type,
                                   @Param("now") LocalDateTime now);
}