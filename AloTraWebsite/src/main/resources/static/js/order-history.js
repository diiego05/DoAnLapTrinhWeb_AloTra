import { apiFetch } from '/alotra-website/js/auth-helper.js';

/* ======================= ⚙️ CONFIG ======================= */
const ordersList=document.getElementById('ordersList');
const filterButtons=document.querySelectorAll('[data-status]');
let currentStatus='';
let selectedRating=0;
let currentReviewOrderItemId=null;
let currentReviewProductId=null;
let currentReviewId=null;

// Cloudinary
const CLOUD_NAME='dmxxo6wgl';
const UPLOAD_PRESET='ml_default';

/* ======================= HELPERS ======================= */
const toNum=v=>Number.isNaN(Number(v))?0:Number(v);
const fmtVND=v=>toNum(v).toLocaleString('vi-VN')+' ₫';

function mapStatusColor(s){
  switch(s){
    case'PENDING':return'warning';
    case'CONFIRMED':return'secondary';
    case'AWAITING_PAYMENT':return'info';
    case'PAID':return'primary';
    case'SHIPPING':return'info';
    case'COMPLETED':return'success';
    case'CANCELED':return'danger';
    case'FAILED':return'dark';
    default:return'secondary';
  }
}
function mapStatusText(s){
  switch(s){
    case'PENDING':return'Chờ xác nhận';
    case'CONFIRMED':return'Đã xác nhận';
    case'AWAITING_PAYMENT':return'Chờ thanh toán';
    case'PAID':return'Đã thanh toán';
    case'SHIPPING':return'Đang giao';
    case'COMPLETED':return'Hoàn thành';
    case'CANCELED':return'Đã hủy';
    case'FAILED':return'Thất bại';
    default:return s;
  }
}
function mapPaymentMethodText(m){
  if(!m)return'—';
  switch(m){
    case'PICKUP':return'Nhận tại cửa hàng';
    case'COD':return'Thanh toán khi nhận hàng';
    case'BANK':return'Chuyển khoản ngân hàng';
    default:return m;
  }
}

/* ======================= LỌC TRẠNG THÁI ======================= */
filterButtons.forEach(btn=>{
  btn.addEventListener('click',()=>{
    filterButtons.forEach(b=>b.classList.remove('active'));
    btn.classList.add('active');
    currentStatus=btn.dataset.status;
    loadOrders();
  });
});

/* ======================= LOAD ĐƠN HÀNG ======================= */
async function loadOrders(){
  ordersList.innerHTML=`<div class="text-center text-muted py-5">Đang tải dữ liệu...</div>`;
  try{
    const res=await apiFetch(`/api/orders${currentStatus?`?status=${currentStatus}`:''}`);
    if(!res.ok)throw new Error(`orders ${res.status}`);
    const orders=await res.json();
    if(!orders||orders.length===0){
      ordersList.innerHTML=`<div class="text-center text-muted py-5">Không có đơn hàng</div>`;
      return;
    }
    ordersList.innerHTML=orders.map(renderOrderCard).join('');
  }catch(e){
    console.error('❌ Lỗi loadOrders:',e);
    ordersList.innerHTML=`<div class="text-center text-danger py-5">Lỗi tải đơn hàng</div>`;
  }
}

function renderOrderCard(o){
  return `
  <div class="card shadow-sm border-0 order-card">
    <div class="card-body">
      <div class="d-flex justify-content-between align-items-center mb-2">
        <div>
          <div class="fw-bold text-dark fs-5">#${o.code}</div>
          <small class="text-muted">${new Date(o.createdAt).toLocaleString('vi-VN')}</small>
        </div>
        <span class="badge bg-${mapStatusColor(o.status)}">${mapStatusText(o.status)}</span>
      </div>

      <div class="mb-2"><strong>Tổng tiền:</strong> <span class="text-success fw-bold">${fmtVND(o.total)}</span></div>
      <div><strong>Phương thức:</strong> ${mapPaymentMethodText(o.paymentMethod)}</div>

      <div class="mt-3 border-top pt-2">
        ${o.items?.length?o.items.map(it=>`
          <div class="d-flex justify-content-between small mb-1">
            <div>${it.productName} (${it.sizeName||'-'}) x ${it.quantity}</div>
            <div>${fmtVND(it.lineTotal)}</div>
          </div>
        `).join(''):'<div class="text-muted small fst-italic">Không có sản phẩm</div>'}
      </div>

      <div class="mt-3 d-flex justify-content-end gap-2">
        ${(o.status==='PENDING'||o.status==='AWAITING_PAYMENT')?`
          <button class="btn btn-sm btn-danger" onclick="cancelOrder(${o.id})">
            <i class="fas fa-times"></i> Hủy
          </button>`:''}
        <button class="btn btn-sm btn-primary" onclick="showOrderDetail(${o.id})">
          <i class="fas fa-eye"></i> Xem chi tiết
        </button>
      </div>
    </div>
  </div>`;
}

/* ======================= CHI TIẾT ĐƠN ======================= */
window.showOrderDetail=async function(orderId){
  const modal=new bootstrap.Modal(document.getElementById("orderDetailModal"));
  const loadingEl=document.getElementById("orderModalLoading");
  const contentEl=document.getElementById("orderModalContent");

  modal.show();
  loadingEl.style.display="block";
  contentEl.style.display="none";

  try{
    const res=await apiFetch(`/api/orders/${orderId}`);
    if(!res.ok)throw new Error(`order ${res.status}`);
    const order=await res.json();

    document.getElementById("modalOrderCode").textContent=`#${order.code}`;
    document.getElementById("modalOrderDate").textContent=new Date(order.createdAt).toLocaleString('vi-VN');
    document.getElementById("modalOrderStatus").textContent=mapStatusText(order.status);
    document.getElementById("modalOrderStatus").className=`badge bg-${mapStatusColor(order.status)}`;
    document.getElementById("modalOrderPayment").textContent=mapPaymentMethodText(order.paymentMethod);
    document.getElementById("modalOrderAddress").textContent=order.deliveryAddress||'—';
    document.getElementById("modalSubtotal").textContent=fmtVND(order.subtotal);
    document.getElementById("modalDiscount").textContent=fmtVND(order.discount);
    document.getElementById("modalShipping").textContent=fmtVND(order.shippingFee);
    document.getElementById("modalOrderTotal").textContent=fmtVND(order.total);

    const items=order.items||[];
    document.getElementById("modalOrderItems").innerHTML=items.length
      ?(await Promise.all(items.map(renderOrderItemRow(order.status)))).join('')
      :`<tr><td colspan="5" class="text-center text-muted">Không có sản phẩm</td></tr>`;

    loadingEl.style.display="none";
    contentEl.style.display="block";
  }catch(e){
    console.error('❌ Lỗi showOrderDetail:',e);
    loadingEl.textContent="⚠️ Lỗi tải dữ liệu đơn hàng!";
  }
};

function pickProductId(it){
  return(it.productId??it.product?.id??it.product?.productId??it.productVariant?.productId??it.variant?.productId??null);
}

function renderOrderItemRow(orderStatus){
  return async(it)=>{
    let reviewBtn='';
    let starHtml='';
    const safeProductId=pickProductId(it);
    try{
      const reviewRes=await apiFetch(`/api/reviews/order-item/${it.id}`);
      if(reviewRes.ok){
        const review=await reviewRes.json();
        starHtml=Array.from({length:5},(_,i)=>
          `<i class="fa${i<review.rating?'s':'r'} fa-star text-warning"></i>`).join('');
        reviewBtn=`
          <button class="btn btn-sm btn-outline-secondary"
            onclick="openReviewModal(${it.id}, ${safeProductId??'null'}, '${it.productName?.replace(/'/g,"\\'")}', true, ${review.id})">
            <i class="fas fa-eye"></i> Xem đánh giá
          </button>`;
      }else if(orderStatus==='COMPLETED'){
        reviewBtn=`
          <button class="btn btn-sm btn-outline-warning"
            onclick="openReviewModal(${it.id}, ${safeProductId??'null'}, '${it.productName?.replace(/'/g,"\\'")}', false, null)">
            <i class="fas fa-star"></i> Đánh giá
          </button>`;
      }
    }catch(e){
      console.warn(`⚠️ Không thể lấy review cho item ${it.id}`,e);
    }

    return `
    <tr>
      <td>
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <strong>${it.productName}</strong>
            ${starHtml?`<div class="mt-1">${starHtml}</div>`:''}
          </div>
          ${reviewBtn}
        </div>
      </td>
      <td>${it.sizeName||'-'}</td>
      <td>${it.quantity}</td>
      <td>${fmtVND(it.unitPrice)}</td>
      <td>${fmtVND(it.lineTotal)}</td>
    </tr>`;
  };
}

/* ======================= MODAL ĐÁNH GIÁ ======================= */
window.openReviewModal=async function(orderItemId,productId,productName,isViewMode=false,reviewId=null){
  currentReviewOrderItemId=orderItemId;
  currentReviewProductId=productId;
  currentReviewId=reviewId;
  selectedRating=0;

  const modal=new bootstrap.Modal(document.getElementById("reviewModal"));
  const contentEl=document.getElementById("reviewContent");
  const mediaInput=document.getElementById("reviewMedia");
  const submitBtn=document.getElementById("btnSubmitReview");
  const editBtn=document.getElementById("btnEditReview");

  document.getElementById("reviewProductName").textContent=productName;
  contentEl.value="";
  mediaInput.value="";
  document.querySelectorAll("#reviewStars i").forEach(star=>star.className="far fa-star");
  const oldPreview=document.getElementById("mediaPreview");
  if(oldPreview)oldPreview.remove();

  if(isViewMode&&reviewId){
    const res=await apiFetch(`/api/reviews/${reviewId}`);
    if(res.ok){
      const review=await res.json();
      selectedRating=review.rating;
      document.querySelectorAll("#reviewStars i").forEach(star=>{
        const val=parseInt(star.dataset.value);
        star.className=val<=selectedRating?"fas fa-star":"far fa-star";
      });
      contentEl.value=review.content;
      contentEl.readOnly=true;
      mediaInput.classList.add("d-none");
      submitBtn.classList.add("d-none");
      editBtn.classList.remove("d-none");

      if(review.mediaUrls?.length){
        const preview=review.mediaUrls.map(url=>
          url.match(/\.(mp4|mov|avi|mkv)$/i)
            ?`<video src="${url}" controls class="w-100 rounded mb-2"></video>`
            :`<img src="${url}" class="img-fluid rounded mb-2" alt="Review media">`
        ).join('');
        contentEl.insertAdjacentHTML("afterend",`<div id="mediaPreview" class="mt-2">${preview}</div>`);
      }
    }
  }else{
    contentEl.readOnly=false;
    mediaInput.classList.remove("d-none");
    submitBtn.classList.remove("d-none");
    editBtn.classList.add("d-none");
  }
  modal.show();
};

document.getElementById("btnEditReview").addEventListener("click",()=>{
  document.getElementById("reviewContent").readOnly=false;
  document.getElementById("reviewMedia").classList.remove("d-none");
  document.getElementById("btnSubmitReview").classList.remove("d-none");
  document.getElementById("btnEditReview").classList.add("d-none");
});

document.querySelectorAll("#reviewStars i").forEach(star=>{
  star.addEventListener("click",()=>{
    selectedRating=parseInt(star.dataset.value);
    document.querySelectorAll("#reviewStars i").forEach(s=>
      s.className=parseInt(s.dataset.value)<=selectedRating?"fas fa-star":"far fa-star"
    );
  });
});

async function uploadMediaFiles(mediaInput){
  const urls=[];
  for(const file of mediaInput.files){
    const fd=new FormData();
    fd.append("file",file);
    fd.append("upload_preset",UPLOAD_PRESET);
    const up=await fetch(`https://api.cloudinary.com/v1_1/${CLOUD_NAME}/auto/upload`,{method:"POST",body:fd});
    if(up.ok){
      const data=await up.json();
      urls.push(data.secure_url);
    }
  }
  return urls;
}

document.getElementById("btnSubmitReview").addEventListener("click",async()=>{
  const content=document.getElementById("reviewContent").value.trim();
  const mediaInput=document.getElementById("reviewMedia");
  if(!currentReviewOrderItemId)return alert("Thiếu orderItemId.");
  if(!currentReviewProductId)return alert("Không xác định được sản phẩm để đánh giá.");
  if(selectedRating===0)return alert("Vui lòng chọn số sao đánh giá.");
  if(content.length<50)return alert("Nội dung tối thiểu 50 ký tự.");

  const mediaUrls=await uploadMediaFiles(mediaInput);
  const reviewDto={orderItemId:currentReviewOrderItemId,productId:currentReviewProductId,rating:selectedRating,content,mediaUrls};

  const formData=new FormData();
  formData.append("review",new Blob([JSON.stringify(reviewDto)],{type:"application/json"}));
  for(const f of mediaInput.files)formData.append("files",f);

  const res=await fetch(
    currentReviewId?`/alotra-website/api/reviews/${currentReviewId}`:`/alotra-website/api/reviews`,
    {method:currentReviewId?"PUT":"POST",body:formData,credentials:'include'}
  );

  if(res.ok){
    alert(currentReviewId?"✅ Cập nhật đánh giá thành công!":"✅ Gửi đánh giá thành công!");
    bootstrap.Modal.getInstance(document.getElementById("reviewModal")).hide();
    loadOrders();
  }else{
    const txt=await res.text();
    console.error("❌ Gửi đánh giá lỗi:",txt);
    alert("❌ Gửi đánh giá thất bại.");
  }
});

/* ======================= HỦY ĐƠN ======================= */
window.cancelOrder=async function(orderId){
  if(!confirm("Bạn có chắc chắn muốn hủy đơn hàng này không?"))return;
  try{
    const res=await apiFetch(`/api/orders/${orderId}/cancel`,{method:"PUT"});
    if(res.ok){
      alert("✅ Đã hủy đơn hàng thành công!");
      loadOrders();
    }else{
      alert("❌ Không thể hủy đơn hàng.");
    }
  }catch(e){
    console.error("❌ Lỗi khi hủy đơn hàng:",e);
    alert("⚠️ Đã xảy ra lỗi khi hủy đơn.");
  }
};

/* ======================= STYLE ======================= */
const style=document.createElement('style');
style.textContent=`
  .order-card:hover { background-color: #f9f9f9; transition: all 0.2s ease; }
  #reviewStars i { cursor: pointer; margin-right: 5px; }
  #reviewStars i:hover { transform: scale(1.2); transition: 0.2s; }
`;
document.head.appendChild(style);

/* ======================= INIT ======================= */
loadOrders();
