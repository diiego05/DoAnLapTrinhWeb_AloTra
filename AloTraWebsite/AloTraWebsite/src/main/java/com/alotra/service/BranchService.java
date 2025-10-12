package com.alotra.service;

import com.alotra.entity.Branch;
import com.alotra.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class BranchService {

    @Autowired
    private BranchRepository branchRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /**
     * Phương thức nội bộ để tạo slug từ một chuỗi đầu vào.
     * @param input Chuỗi cần chuyển đổi (ví dụ: tên chi nhánh)
     * @return Chuỗi slug đã được định dạng
     */
    private String generateSlug(String input) {
        if (input == null) {
            return "";
        }
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    public Branch findById(Long id) {
        return branchRepository.findById(id).orElse(null);
    }

    /**
     * Lưu một chi nhánh. Tự động tạo slug từ tên trước khi lưu.
     * @param branch Đối tượng chi nhánh cần lưu
     */
    public void save(Branch branch) {
        // Tự động tạo và gán slug từ tên chi nhánh trước khi lưu
        branch.setSlug(generateSlug(branch.getName()));
        branchRepository.save(branch);
    }

    public void deleteById(Long id) {
        branchRepository.deleteById(id);
    }

    /**
     * Phương thức tìm kiếm và lọc đã được tối ưu.
     * @param keyword Từ khóa tìm kiếm
     * @param status Trạng thái cần lọc
     * @return Danh sách chi nhánh phù hợp
     */
    public List<Branch> searchByKeywordAndStatus(String keyword, String status) {
        return branchRepository.searchAndFilter(
                StringUtils.hasText(keyword) ? keyword : null,
                StringUtils.hasText(status) ? status : null
        );
    }

    public List<Branch> getAllBranches() {
        // Bạn có thể lọc active nếu cần:
        // return branchRepository.findByStatus("ACTIVE");
        return branchRepository.findAll();
    }
}