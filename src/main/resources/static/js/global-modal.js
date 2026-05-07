"use strict";

/* =====================================================
   🧩 TỰ ĐỘNG THÊM CSS VÀO TRANG
===================================================== */
(function injectGlobalModalCSS() {
    if (document.getElementById("globalModalStyle")) return;
    const style = document.createElement("style");
    style.id = "globalModalStyle";
    style.innerHTML = `
        .alert-green {
            background: linear-gradient(90deg, #28a745 0%, #218838 100%);
            color: white;
        }
        .btn-green {
            background: linear-gradient(90deg, #28a745 0%, #218838 100%);
            color: white;
            font-weight: 600;
            border: none;
            transition: 0.2s ease;
        }
        .btn-green:hover { opacity: 0.9; }

        /* SUCCESS MODAL */
        .success-modal-overlay {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.55);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 3000;
            animation: fadeIn 0.3s ease;
        }
        .success-modal {
            background: #fff;
            border-radius: 16px;
            max-width: 500px;
            width: 90%;
            overflow: hidden;
            box-shadow: 0 10px 30px rgba(0, 128, 0, 0.25);
            animation: scaleIn 0.35s ease;
            text-align: center;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        .success-modal-header {
            background: linear-gradient(90deg, #28a745 0%, #218838 100%);
            padding: 30px 20px 20px;
            color: white;
        }
        .success-modal-icon {
            width: 70px;
            height: 70px;
            margin: 0 auto 15px;
            border-radius: 50%;
            background: white;
            display: flex;
            justify-content: center;
            align-items: center;
        }
        .success-modal-icon i {
            color: #28a745;
            font-size: 36px;
        }
        .success-modal-title {
            font-size: 24px;
            font-weight: 700;
            margin-bottom: 8px;
        }
        .success-modal-subtitle {
            font-size: 15px;
            opacity: 0.9;
        }
        .success-modal-body { padding: 20px; font-size: 15px; }
        .order-code-box {
            background: #f6fff7;
            border: 1px dashed #28a745;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 15px;
        }
        .order-code-label { color: #28a745; font-weight: 500; }
        .order-code-value { font-size: 20px; font-weight: 700; color: #218838; }
        .success-modal-message { color: #555; line-height: 1.5; }
        .success-modal-message i { color: #28a745; margin-right: 5px; }
        .success-modal-footer {
            display: flex;
            justify-content: space-between;
            padding: 15px 20px 20px;
            gap: 10px;
        }
        .success-modal-btn {
            flex: 1;
            padding: 10px 14px;
            font-size: 15px;
            font-weight: 600;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            transition: 0.25s ease;
        }
        .success-modal-btn-primary {
            background: linear-gradient(90deg, #28a745 0%, #218838 100%);
            color: white;
        }
        .success-modal-btn-primary:hover { opacity: 0.9; }
        .success-modal-btn-secondary {
            background: #f1f8f3;
            color: #218838;
            border: 1px solid #c8e6c9;
        }
        .success-modal-btn-secondary:hover { background: #e8f5e9; }

        @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes scaleIn { from { transform: scale(0.8); opacity: 0; } to { transform: scale(1); opacity: 1; } }
    `;
    document.head.appendChild(style);
})();

/* =====================================================
   📢 MODAL THÔNG BÁO CHUNG (MÀU XANH)
===================================================== */
function showAlert(message, title = "Thông báo") {
    if (!document.getElementById("alertModal")) {
        const html = `
            <div class="modal fade" id="alertModal" tabindex="-1" aria-hidden="true">
              <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0">
                  <div class="modal-header alert-green">
                    <h5 class="modal-title" id="alertModalLabel"></h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                  </div>
                  <div class="modal-body" id="alertModalBody"></div>
                  <div class="modal-footer border-0">
                    <button type="button" class="btn btn-green w-100" data-bs-dismiss="modal">OK</button>
                  </div>
                </div>
              </div>
            </div>`;
        document.body.insertAdjacentHTML("beforeend", html);
    }

    document.getElementById("alertModalLabel").innerText = title;
    document.getElementById("alertModalBody").innerText = message;

    new bootstrap.Modal(document.getElementById("alertModal")).show();
}
