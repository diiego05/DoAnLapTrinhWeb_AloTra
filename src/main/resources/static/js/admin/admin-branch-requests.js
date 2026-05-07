"use strict";

import { apiFetch } from '/alotra-website/js/auth-helper.js';

const BRANCH_STATUS_VI={
  PENDING:"Đang chờ duyệt",
  APPROVED:"Đã duyệt",
  REJECTED:"Từ chối"
};

const branchStatusToVi=s=>BRANCH_STATUS_VI[s]||s||"(Không rõ)";

const branchStatusBadgeClass=s=>{
  if(s==="PENDING")return"bg-warning";
  if(s==="APPROVED")return"bg-success";
  if(s==="REJECTED")return"bg-danger";
  return"bg-secondary";
};

document.addEventListener("DOMContentLoaded",async()=>{
  console.log("🏪 Quản lý yêu cầu chi nhánh (Admin) khởi chạy...");

  const tableBody=document.getElementById("branchRequestsTableBody");
  const statusFilter=document.getElementById("branchStatusFilter");
  const reloadBtn=document.getElementById("reloadBranchRequests");

  const rejectModal=new bootstrap.Modal(document.getElementById("rejectBranchModal"));
  const rejectBranchId=document.getElementById("rejectBranchId");
  const rejectBranchNote=document.getElementById("rejectBranchNote");
  const btnRejectConfirm=document.getElementById("btnRejectBranchConfirm");

  const detailModal=new bootstrap.Modal(document.getElementById("branchDetailModal"));

  async function loadBranchRequests(){
    tableBody.innerHTML=`<tr><td colspan="8" class="text-center text-muted">Đang tải...</td></tr>`;

    const status=statusFilter.value;
    const url=status?`/api/register/branch?status=${status}`:`/api/register/branch`;

    const res=await apiFetch(url);
    if(!res.ok){
      tableBody.innerHTML=`<tr><td colspan="8" class="text-center text-danger">Không thể tải dữ liệu</td></tr>`;
      return;
    }

    const data=await res.json();
    if(!data||data.length===0){
      tableBody.innerHTML=`<tr><td colspan="8" class="text-center text-muted">Không có yêu cầu</td></tr>`;
      return;
    }

    tableBody.innerHTML=data.map((r,idx)=>`
      <tr>
        <td>${idx+1}</td>
        <td>${r.branchName||"(Chưa có)"}</td>
        <td>${r.address||"(Chưa có)"}</td>
        <td>${r.phone||"(Chưa có)"}</td>
        <td>
          <span class="badge ${branchStatusBadgeClass(r.status)}">
            ${branchStatusToVi(r.status)}
          </span>
        </td>
        <td class="text-center">
          <button class="btn btn-sm btn-info me-2 btn-view" data-id="${r.id}" title="Xem chi tiết">👁️</button>
          ${r.status==="PENDING"?`
            <button class="btn btn-sm btn-success me-2 btn-approve" data-id="${r.id}" title="Duyệt">✅</button>
            <button class="btn btn-sm btn-danger btn-reject" data-id="${r.id}" title="Từ chối">❌</button>
          `:"-"}
        </td>
      </tr>
    `).join("");

    tableBody.querySelectorAll(".btn-approve").forEach(btn=>{
      btn.addEventListener("click",()=>approveBranch(btn.dataset.id));
    });

    tableBody.querySelectorAll(".btn-reject").forEach(btn=>{
      btn.addEventListener("click",()=>openRejectModal(btn.dataset.id));
    });

    tableBody.querySelectorAll(".btn-view").forEach(btn=>{
      btn.addEventListener("click",()=>viewBranchDetail(btn.dataset.id));
    });
  }

  async function approveBranch(id){
    if(!confirm("Xác nhận duyệt yêu cầu này?"))return;
    const res=await apiFetch(`/api/register/admin/branch/${id}/approve`,{method:"PUT"});
    if(res.ok){
      showAlert("✅ Đã duyệt yêu cầu thành công!");
      loadBranchRequests();
    }else{
      showAlert("❌ Lỗi duyệt yêu cầu.");
    }
  }

  function openRejectModal(id){
    rejectBranchId.value=id;
    rejectBranchNote.value="";
    rejectModal.show();
  }

  btnRejectConfirm.addEventListener("click",async()=>{
    const id=rejectBranchId.value;
    const note=encodeURIComponent(rejectBranchNote.value.trim());
    const res=await apiFetch(`/api/register/admin/branch/${id}/reject?note=${note}`,{method:"PUT"});
    if(res.ok){
      showAlert("❌ Đã từ chối yêu cầu.");
      rejectModal.hide();
      loadBranchRequests();
    }else{
      showAlert("❌ Lỗi từ chối yêu cầu.");
    }
  });

  async function viewBranchDetail(id){
    const res=await apiFetch(`/api/register/branch/${id}`);
    if(!res.ok){
      showAlert("❌ Không thể tải thông tin chi tiết.");
      return;
    }

    const r=await res.json();
    document.getElementById("detailBranchName").textContent=r.branchName||"(Chưa có)";
    document.getElementById("detailBranchAddress").textContent=r.address||"(Chưa có)";
    document.getElementById("detailBranchPhone").textContent=r.phone||"(Chưa có)";

    const statusWrap=document.getElementById("detailBranchStatus");
    statusWrap.innerHTML="";
    const span=document.createElement("span");
    span.className="badge "+branchStatusBadgeClass(r.status);
    span.textContent=branchStatusToVi(r.status);
    statusWrap.appendChild(span);

    document.getElementById("detailBranchNote").textContent=r.note||"(Không có)";
    document.getElementById("detailBranchCreated").textContent=r.createdAt?new Date(r.createdAt).toLocaleString("vi-VN"):"";

    document.getElementById("detailRequesterName").textContent=r.requesterName||"(Chưa có)";
    document.getElementById("detailRequesterEmail").textContent=r.requesterEmail||"(Chưa có)";
    document.getElementById("detailRequesterPhone").textContent=r.requesterPhone||"(Chưa có)";
    document.getElementById("detailRequesterIdCard").textContent=r.idCardNumber||"(Chưa có)";
    document.getElementById("detailRequesterGender").textContent=r.gender||"(Chưa có)";
    document.getElementById("detailRequesterDob").textContent=r.dob?new Date(r.dob).toLocaleDateString("vi-VN"):"(Chưa có)";

    detailModal.show();
  }

  statusFilter.addEventListener("change",loadBranchRequests);
  reloadBtn.addEventListener("click",loadBranchRequests);

  await loadBranchRequests();
});
