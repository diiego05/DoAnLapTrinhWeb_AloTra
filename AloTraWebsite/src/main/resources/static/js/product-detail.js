document.addEventListener("DOMContentLoaded", function() {
    const priceDisplay = document.getElementById("product-price");
    const addToCartBtn = document.getElementById("add-to-cart-btn");
    const addToCartPrice = document.getElementById("add-to-cart-price");
    const quantityInput = document.getElementById("quantity");
    const btnMinus = document.getElementById("btn-minus");
    const btnPlus = document.getElementById("btn-plus");
    const sizeButtons = document.querySelectorAll(".size-btn");

    let currentPrice = 0;
    let currentVariantId = null;

    const formatCurrency = (v) =>
        new Intl.NumberFormat("vi-VN").format(v) + " ₫";

    function updateTotal() {
        const qty = parseInt(quantityInput.value);
        addToCartPrice.textContent = formatCurrency(currentPrice * qty);
    }

    function selectSize(btn) {
        sizeButtons.forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        currentPrice = parseFloat(btn.dataset.price);
        currentVariantId = btn.dataset.variantId;
        priceDisplay.textContent = formatCurrency(currentPrice);
        updateTotal();
    }

    sizeButtons.forEach(btn => {
        btn.addEventListener("click", () => selectSize(btn));
    });

    btnMinus.addEventListener("click", () => {
        let qty = parseInt(quantityInput.value);
        if (qty > 1) quantityInput.value = qty - 1;
        updateTotal();
    });

    btnPlus.addEventListener("click", () => {
        let qty = parseInt(quantityInput.value);
        quantityInput.value = qty + 1;
        updateTotal();
    });

    addToCartBtn.addEventListener("click", () => {
        if (!currentVariantId) {
            alert("Vui lòng chọn kích cỡ!");
            return;
        }
        const qty = parseInt(quantityInput.value);
        fetch("/cart/add", {
            method: "POST",
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: `variantId=${currentVariantId}&quantity=${qty}`
        })
        .then(() => alert("Đã thêm sản phẩm vào giỏ hàng!"))
        .catch(err => console.error(err));
    });

    // Chọn size mặc định
    if (sizeButtons.length > 0) selectSize(sizeButtons[0]);
});
