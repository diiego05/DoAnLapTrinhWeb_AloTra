function generateSlug(text) {
    return text.toLowerCase()
               .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
               .replace(/[^a-z0-9\s-]/g, '')
               .trim()
               .replace(/\s+/g, '-')
               .replace(/-+/g, '-');
}

document.addEventListener("DOMContentLoaded", function () {
    const nameInput = document.getElementById("name");
    const slugInput = document.getElementById("slug");
    const form = document.querySelector("form");

    if (nameInput && slugInput && form) {
        // Khi rời khỏi ô tên hoặc nhập ô khác
        form.addEventListener("focusin", function () {
            if (nameInput.value.trim() && !slugInput.value.trim()) {
                slugInput.value = generateSlug(nameInput.value);
            }
        });

        // Khi đang gõ tên
        nameInput.addEventListener("input", function () {
            if (!slugInput.value.trim()) {
                slugInput.value = generateSlug(nameInput.value);
            }
        });
    }
});
