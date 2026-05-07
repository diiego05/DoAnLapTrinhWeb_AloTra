"use strict";

// ✅ Tự động lấy context path (VD: /alotra-website)
const ctx = `/${window.location.pathname.split('/')[1]}`;

document.addEventListener('DOMContentLoaded', async () => {
    // ✅ Lấy campaignId từ URL cuối cùng
    const campaignId = window.location.pathname.split('/').pop();

    // ✅ Gọi API có context path
    const res = await fetch(`${ctx}/api/public/promotions/${campaignId}`);
    if (!res.ok) {
        document.body.innerHTML = `
            <div class="text-center my-5 text-danger fw-bold">
                ❌ Chiến dịch không tồn tại hoặc đã hết hạn (${res.status})
            </div>`;
        return;
    }

    const data = await res.json();

    // =================== GÁN DỮ LIỆU VÀO TRANG ===================
    document.getElementById('campaignBanner').src = data.banner || '/images/placeholder.png';
    document.getElementById('campaignName').textContent = data.name || '—';
    document.getElementById('campaignViews').textContent = data.viewCount ?? 0;
    document.getElementById('campaignDate').textContent = `${formatDate(data.startAt)} - ${formatDate(data.endAt)}`;
    document.getElementById('campaignDescription').innerHTML = data.description || '';

    // =================== ĐỐI TƯỢNG ÁP DỤNG ===================
    const targetList = document.getElementById('targetList');
    if (data.targetDetails && data.targetDetails.length > 0) {
        targetList.innerHTML = data.targetDetails.map(t => `<li>${t}</li>`).join('');
    } else {
        targetList.innerHTML = `<li class="text-muted">Không có đối tượng cụ thể</li>`;
    }

    // =================== MÃ GIẢM GIÁ ===================
	// =================== MÃ GIẢM GIÁ ===================
	const couponContainer = document.getElementById('couponContainer');

	// 1) Có danh sách coupon → render từng mã + giá trị giảm
	if (Array.isArray(data.coupons) && data.coupons.length > 0) {
	  couponContainer.innerHTML = data.coupons.map(c => {
	    const label = makeDiscountLabel(c.type ?? c.kind, c.value ?? c.amount ?? c.percent, c.maxDiscount);
	    return `
	      <div class="border rounded px-3 py-2 bg-light mb-2">
	        <span class="fw-bold">${c.code}</span>
	        <span class="text-muted ms-2">${label}</span>
	      </div>
	    `;
	  }).join('');
	} else {
	  // 2) Không có coupon → vẫn hiển thị "giá trị giảm" cấp campaign nếu có
	  // Hỗ trợ nhiều key khả dĩ từ BE: (discountType/discountValue) hoặc (type/value)
	  const campaignType =
	    data.discountType ?? data.type ?? null;
	  const campaignValue =
	    data.discountValue ?? data.value ?? null;
	  const campaignMax =
	    data.maxDiscount ?? data.campaignMaxDiscount ?? null;

	  if (campaignValue != null) {
	    const label = makeDiscountLabel(campaignType, campaignValue, campaignMax);
	    couponContainer.innerHTML = `
	      <div class="border rounded px-3 py-2 bg-light">
	        <span class="fw-bold">Không cần mã</span>
	        <span class="text-muted ms-2">${label}</span>
	      </div>
	    `;
	  } else {
	    couponContainer.innerHTML = `<div class="text-muted">Không có mã giảm giá</div>`;
	  }
	}

});

// ✅ Hàm format ngày kiểu VN
function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN');
}

// ✅ Định dạng tiền VND
function fmtVND(v) {
  const n = Number(v ?? 0);
  return n.toLocaleString('vi-VN') + 'đ';
}

// ✅ Tạo nhãn giảm giá từ type/value (+maxDiscount nếu có)
function makeDiscountLabel(type, value, maxDiscount) {
  const t = (type || '').toUpperCase();
  const val = Number(value ?? 0);
  let main = '';

  if (t === 'PERCENT') main = `Giảm ${Math.round(val)}%`;
  else if (t === 'AMOUNT') main = `Giảm ${fmtVND(val)}`;
  else if (val > 0 && val < 100) main = `Giảm ${Math.round(val)}%`; // fallback đoán %
  else if (val > 0) main = `Giảm ${fmtVND(val)}`;
  else main = 'Giảm';

  if (maxDiscount != null && Number(maxDiscount) > 0) {
    main += ` (tối đa ${fmtVND(maxDiscount)})`;
  }
  return main;
}

