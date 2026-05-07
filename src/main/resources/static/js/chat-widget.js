// ===== CHAT WIDGET CONFIGURATION =====
const ChatWidget = {
  // Derive application base from current location so widget works in any host/context path
  _deriveBase() {
    try {
      const origin = window.location.origin;
      const pathParts = window.location.pathname.split('/').filter(Boolean);
      // If app deployed under a context path (e.g. /alotra-website), use the first segment
      const context = pathParts.length > 0 ? `/${pathParts[0]}` : '';
      return origin + context;
    } catch (e) {
      return '';
    }
  },

  // WebSocket and API base (will be built from derived base)
  get WS_URL() { return this._deriveBase() + '/ws/chat'; },
  get API_BASE() { return this._deriveBase() + '/api'; },
  get API_URL() { return this.API_BASE + '/chat'; },

  stompClient: null,
  roomId: null,
  userId: null,
  lastUserId: null,
  // Pending echo resolvers waiting for server-broadcasted messages
  pendingEchoResolvers: [],
  menuListenersAdded: false, // ✅ THÊM: Track đã gắn event chưa

  init() {
	this.setupEventListeners();
	   this.showChatButton();
	   console.log('✅ ChatWidget initialized');
  },
  createQuickMenu() {
    const modal = document.getElementById('chatModal');
    if (!modal) {
      console.error('❌ Modal not found');
      return;
    }

    let menu = document.getElementById('quickMenu');
    if (menu) {
      console.log('⚠️ Menu already exists');

      // ✅ NẾU MENU ĐÃ CÓ, CHỈ TẠO NÚT MŨI TÊN
      let toggleBtn = document.getElementById('menuToggleBtn');
      if (!toggleBtn) {
        const toggleHTML = `
          <button class="chat-menu-toggle" id="menuToggleBtn">
            <i class="fas fa-chevron-down"></i>
          </button>
        `;
        menu.insertAdjacentHTML('afterend', toggleHTML);
        this.setupMenuToggle();
        console.log('✅ Toggle button added');
      }
      return;
    }

    const header = modal.querySelector('.chat-modal-header');
    const messagesContainer = modal.querySelector('.chat-messages-container');

    if (!header || !messagesContainer) {
      console.error('❌ Cannot find header or messages container');
      return;
    }

    // ✅ TẠO MENU + NÚT MŨI TÊN
    const menuHTML = `
      <div class="chat-quick-menu" id="quickMenu">
        <button class="chat-quick-btn" data-action="track-order">
          <i class="fas fa-box"></i>
          <span>📦 Theo dõi đơn hàng</span>
        </button>
        <button class="chat-quick-btn" data-action="consult-tea">
          <i class="fas fa-mug-hot"></i>
          <span>🍵 Tư vấn trà sữa phù hợp</span>
        </button>
        <button class="chat-quick-btn" data-action="promotion">
          <i class="fas fa-gift"></i>
          <span>🎁 Xem khuyến mãi</span>
        </button>
        <button class="chat-quick-btn" data-action="contact-admin">
          <i class="fas fa-headset"></i>
          <span>📞 Liên hệ tư vấn viên</span>
        </button>
      </div>
      <button class="chat-menu-toggle" id="menuToggleBtn">
        <i class="fas fa-chevron-down"></i>
      </button>
    `;

    messagesContainer.insertAdjacentHTML('beforebegin', menuHTML);
    this.setupMenuToggle();
    console.log('✅ Quick menu + toggle button created');
  },
  setupMenuToggle() {
    const toggleBtn = document.getElementById('menuToggleBtn');
    const menu = document.getElementById('quickMenu');

    if (!toggleBtn || !menu) {
      console.warn('⚠️ Toggle button or menu not found');
      return;
    }

    toggleBtn.addEventListener('click', () => {
      const isCollapsed = menu.classList.contains('collapsed');

      if (isCollapsed) {
        // MỞ RỘNG
        menu.classList.remove('collapsed');
        toggleBtn.classList.remove('collapsed');
        console.log('📂 Menu expanded');
      } else {
        // THU GỌN
        menu.classList.add('collapsed');
        toggleBtn.classList.add('collapsed');
        console.log('📁 Menu collapsed');
      }
    });

    console.log('✅ Menu toggle setup');
  },
  isLoggedIn() {
     const token = localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
     const userIdAttr = document.body.getAttribute('data-user-id');

     // ✅ KIỂM TRA KỸ HƠN
     const hasToken = token && token !== 'null' && token !== '';
     const hasUserId = userIdAttr && userIdAttr !== 'null' && userIdAttr !== '' && userIdAttr !== '0';

     return hasToken && hasUserId;
   },

  showChatButton() {
    const btn = document.getElementById('chatFloatingBtn');
    if (btn) btn.style.display = 'flex';
  },

  hideChatButton() {
	const btn = document.getElementById('chatFloatingBtn');
	  if (btn) {
	    btn.style.display = 'none';
	    btn.style.visibility = 'hidden';
	    btn.style.opacity = '0';
	    btn.style.pointerEvents = 'none';
	  }
  },
  showLoginToast() {
    // ✅ TẠO HOẶC LẤY TOAST ELEMENT
	let toast = document.getElementById('chat-login-toast');

	  if (!toast) {
	    const toastHTML = `
	      <div id="chat-login-toast" style="
	        position: fixed;
	        right: 100px;
	        bottom: 30px;
	        background: #dc3545;
	        color: white;
	        padding: 12px 18px;
	        border-radius: 8px;
	        font-size: 14px;
	        box-shadow: 0 4px 12px rgba(220, 53, 69, 0.4);
	        opacity: 0;
	        transform: translateX(20px);
	        transition: all 0.3s ease;
	        pointer-events: none;
	        z-index: 9998;
	        white-space: nowrap;
	      ">
	        ⚠️ Vui lòng đăng nhập để sử dụng chat.
	      </div>
	    `;
	    document.body.insertAdjacentHTML('beforeend', toastHTML);
	    toast = document.getElementById('chat-login-toast');
	  }

	  // Hiện toast (slide từ phải sang trái)
	  toast.style.opacity = '1';
	  toast.style.transform = 'translateX(0)';

	  // Tự động ẩn sau 2.5 giây
	  setTimeout(() => {
	    toast.style.opacity = '0';
	    toast.style.transform = 'translateX(20px)';
	  }, 2500);
  },
  getUserId() {
      const attr = document.body.getAttribute('data-user-id');

      // ✅ KIỂM TRA KỸ HƠN - KHÔNG DÙNG FALLBACK = 1
      if (!attr || attr === 'null' || attr === '' || attr === '0') {
        return null;
      }

      const userId = parseInt(attr, 10);

      // ✅ KIỂM TRA NaN
      if (isNaN(userId) || userId <= 0) {
        return null;
      }

      return userId;
    },

  setupEventListeners() {
    const floatingBtn   = document.getElementById('chatFloatingBtn');
    const closeBtn      = document.getElementById('closeChatBtn');
    const sendBtn       = document.getElementById('chatSendBtn');
    const messageInput  = document.getElementById('chatMessageInput');

    if (floatingBtn) floatingBtn.addEventListener('click', () => this.toggleModal());
    if (closeBtn)    closeBtn.addEventListener('click', () => this.toggleModal());
    if (sendBtn)     sendBtn.addEventListener('click', () => this.sendMessage());

    if (messageInput) {
      messageInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
          e.preventDefault();
          this.sendMessage();
        }
      });
      messageInput.addEventListener('input', (e) => {
        e.target.style.height = 'auto';
        e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px';
      });
    }
  },

  toggleModal() {
	const token = localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken");

	    // ❌ KHÔNG CÓ TOKEN → HIỆN TOAST → DỪNG LẠI
	    if (!token) {
	      console.log('⚠️ User not logged in - Show toast');
	      this.showLoginToast();
	      return;
	    }

	    // ✅ CÓ TOKEN → LẤY userId
	    this.userId = this.getUserId();

	    if (!this.userId) {
	      console.log('⚠️ No userId found - Show toast');
	      this.showLoginToast();
	      return;
	    }

	    // ✅ MỞ MODAL BÌNH THƯỜNG
	    const modal = document.getElementById('chatModal');
	    if (!modal) return;

	    const opening = !modal.classList.contains('active');

	    if (opening) {
	      console.log('🔵 Opening chat modal...');
	      modal.classList.add('active', 'is-open');

	      if (!this.roomId) {
	        console.log('📡 First time opening - loading chat data...');
	        this.createQuickMenu();
	        this.setupQuickMenuListeners();
	        this.loadChatData();
	      } else {
	        console.log('🔄 Already have room - reconnecting...');
	        if (!this.stompClient || !this.stompClient.connected) {
	          this.connectWebSocket();
	        }
	        this.loadChatHistory();
	      }

	      this.scrollToBottom();
	      setTimeout(() => document.getElementById('chatMessageInput')?.focus(), 0);
	    } else {
	      console.log('🔵 Closing chat modal...');
	      modal.classList.remove('active', 'is-open');
	    }
  },
  showLoginRequiredToast() {
      console.log('🔵 showLoginRequiredToast called');

      const oldToast = document.getElementById('chatLoginToast');
      if (oldToast) oldToast.remove();

      const toastHTML = `
        <div id="chatLoginToast" style="
          position: fixed;
          bottom: 30px;
          right: 110px;
          background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
          color: white;
          padding: 14px 20px;
          border-radius: 12px;
          box-shadow: 0 4px 16px rgba(245, 158, 11, 0.4);
          display: flex;
          align-items: center;
          gap: 10px;
          font-size: 14px;
          font-weight: 500;
          z-index: 99999;
          max-width: 320px;
        ">
          <i class="fas fa-exclamation-triangle"></i>
          <span>Vui lòng đăng nhập để sử dụng chat.</span>
        </div>
      `;

      document.body.insertAdjacentHTML('beforeend', toastHTML);
      console.log('✅ Toast added to DOM');

      setTimeout(() => {
        const toast = document.getElementById('chatLoginToast');
        if (toast) {
          toast.style.animation = 'slideOut 0.3s ease';
          setTimeout(() => toast.remove(), 300);
        }
      }, 3000);
    },

  async loadChatData() {
    if (!this.userId) return;

    try {
      console.log('📡 Loading chat for userId:', this.userId);

      const res = await fetch(`${this.API_URL}/user/${this.userId}`);
      const data = await res.json();
      this.roomId = data.id;

      localStorage.setItem('currentChatRoomId', this.roomId);
      console.log('✅ Chat room:', this.roomId, '| User:', data.userName);

      await this.loadChatHistory();
      this.connectWebSocket();

    } catch (error) {
      console.error('❌ Error loading chat:', error);
      this.showError('Lỗi kết nối');
    }
  },

  async loadChatHistory() {
    if (!this.roomId) return;
    try {
      const res = await fetch(`${this.API_URL}/rooms/${this.roomId}/messages`);
      const messages = await res.json();

      const container = document.getElementById('chatMessagesContainer');
      container.innerHTML = '';

      if (messages.length > 0) {
        messages.forEach((m) => this.displayMessage(m));
      } else {
        container.innerHTML = '<div class="chat-loading"><p>👋 Hãy bắt đầu cuộc trò chuyện!</p></div>';
      }

      this.scrollToBottom(true);
    } catch (error) {
      console.error('Error loading chat history:', error);
    }
  },

  // ✅ GẮN EVENT CHO MENU - CHỈ 1 LẦN
  setupQuickMenuListeners() {
    if (this.menuListenersAdded) return; // ✅ Đã gắn rồi thì bỏ qua

    const menuBtns = document.querySelectorAll('.chat-quick-btn');

    menuBtns.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();

        const action = btn.dataset.action;
        console.log('🖱️ Menu clicked:', action);
        this.handleQuickAction(action);
      });
    });

    this.menuListenersAdded = true; // ✅ Đánh dấu đã gắn
    console.log('✅ Menu listeners added');
  },

  // ✅ XỬ LÝ KHI CLICK MENU
  async handleQuickAction(action) {
    console.log('🎯 Handling action:', action);

    if (action === 'track-order') {
      // Gửi thông báo cho admin rồi load đơn hàng
      await this.sendAutoMessage('📦 Tôi muốn kiểm tra trạng thái đơn hàng', { waitForEcho: true, timeout: 4000 }).catch(() => {});
      await this.loadUserOrders();
    } else if (action === 'promotion') {
      // Gửi thông báo cho admin rồi load khuyến mãi
      await this.sendAutoMessage('🎁 Tôi muốn xem khuyến mãi', { waitForEcho: true, timeout: 4000 }).catch(() => {});
      await this.loadPromotions();
    } else {
      // ✅ GỬI TIN NHẮN CHO ADMIN & XỬ LÝ TỰ ĐỘNG
      if (action === 'consult-tea') {
        // Gửi thông báo cho admin rằng user cần tư vấn (KHÔNG hiển thị gợi ý tự động)
        await this.sendAutoMessage('🍵 Tôi cần tư vấn loại trà sữa phù hợp', { waitForEcho: true, timeout: 4000 });
        // (Không hiển thị danh sách tự động theo yêu cầu)
      } else if (action === 'contact-admin') {
        // Gửi thông báo cho admin và chờ server echo trước khi hiển thị lời cảm ơn
        await this.sendAutoMessage('📞 Tôi cần hỗ trợ từ tư vấn viên', { waitForEcho: true, timeout: 4000 });
        // Hiển thị lời cảm ơn và thông báo chuyển tiếp SAU KHI server đã nhận và broadcast tin nhắn người dùng
        this.displayContactAcknowledgement();
      } else {
        const messages = {
            'consult-tea': '🍵 Tôi cần tư vấn loại trà sữa phù hợp',
            'contact-admin': '📞 Tôi cần hỗ trợ từ tư vấn viên',
            'track-order': '📦 Tôi muốn kiểm tra trạng thái đơn hàng',
            'promotion': '🎁 Tôi muốn xem khuyến mãi'
          };

          const content = messages[action];
          if (content) await this.sendAutoMessage(content, { waitForEcho: true, timeout: 4000 });
      }
    }
  },

  // === HIỂN THỊ 5 MÓN TRÀ SỮA BEST SELLER ===
  async displayTeaRecommendations(limit = 5) {
    try {
      const res = await fetch(`${this.API_URL.replace('/api/chat','')}/products/public/top-bestsellers?limit=${limit}`);
      if (!res.ok) throw new Error('Không lấy được best sellers');
      const items = await res.json();

      if (!Array.isArray(items) || items.length === 0) {
        this.displayMessage({ id: Date.now(), senderId: 1, content: '🍵 Hiện chưa có sản phẩm bán chạy để gợi ý.', timestamp: new Date().toISOString() });
        return;
      }

      const listHtml = items.slice(0, limit).map(p => `- ${p.name} (${p.price ? p.price.toLocaleString('vi-VN') + ' ₫' : 'Giá liên hệ'})`).join('\n');

      this.displayMessage({ id: Date.now(), senderId: 1, content: `🍵 Gợi ý ${items.length >= limit ? limit : items.length} món bán chạy:\n${listHtml}`, timestamp: new Date().toISOString() });
      this.scrollToBottom(true);

    } catch (err) {
      console.error('Error fetching best sellers', err);
      this.displayMessage({ id: Date.now(), senderId: 1, content: '❌ Không thể lấy danh sách gợi ý. Vui lòng thử lại sau.', timestamp: new Date().toISOString() });
    }
  },

  // === HIỂN THỊ LỜI CẢM ƠN KHI LIÊN HỆ TƯ VẤN VIÊN ===
  displayContactAcknowledgement() {
    const content = 'Cảm ơn bạn đã liên hệ, tôi sẽ chuyển cho nhân viên gần nhất.';
    this.displayMessage({ id: Date.now(), senderId: 1, content, timestamp: new Date().toISOString() });
    this.scrollToBottom(true);
  },

  // ✅ LOAD ĐƠN HÀNG
  async loadUserOrders() {
    try {
      const token = localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
      const res = await fetch(`${this.API_BASE}/orders`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!res.ok) throw new Error('Không thể tải đơn hàng');

      const orders = await res.json();

      console.log('📦 Orders API response:', orders);

      // ✅ LỌC ĐƠN HÀNG CHƯA HOÀN THÀNH (bao gồm cả PAID)
      const pendingOrders = orders.filter(o =>
        o.status !== 'COMPLETED' && o.status !== 'CANCELED'
      );

      console.log('📦 Filtered orders:', pendingOrders);

      if (pendingOrders.length === 0) {
        this.displayMessage({
          id: Date.now(),
          senderId: 1,
          content: '📦 Bạn chưa có đơn hàng nào đang xử lý.',
          timestamp: new Date().toISOString()
        });
        return;
      }

  await this.displayOrders(pendingOrders);

    } catch (error) {
      console.error('Error loading orders:', error);
      this.displayMessage({
        id: Date.now(),
        senderId: 1,
        content: '❌ Không thể tải danh sách đơn hàng. Vui lòng thử lại!',
        timestamp: new Date().toISOString()
      });
    }
  }
,

  // ✅ LOAD KHUYẾN MÃI
  async loadPromotions() {
    try {
      const token = localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
      const res = await fetch(`${this.API_BASE}/admin/promotions/campaigns`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!res.ok) throw new Error('Không thể tải khuyến mãi');

      const promotions = await res.json();
      const activePromotions = promotions.filter(p => p.status === 'ACTIVE');

      if (activePromotions.length === 0) {
        this.displayMessage({
          id: Date.now(),
          senderId: 1,
          content: '🎁 Hiện tại chưa có chương trình khuyến mãi nào. Vui lòng quay lại sau!',
          timestamp: new Date().toISOString()
        });
        return;
      }

  await this.displayPromotions(activePromotions);

    } catch (error) {
      console.error('Error loading promotions:', error);
      this.displayMessage({
        id: Date.now(),
        senderId: 1,
        content: '❌ Không thể tải danh sách khuyến mãi. Vui lòng thử lại!',
        timestamp: new Date().toISOString()
      });
    }
  },

  async displayPromotions(promotions) {
    const container = document.getElementById('chatMessagesContainer');

    const promoCards = promotions.map(promo => {
      const startDate = new Date(promo.startAt).toLocaleDateString('vi-VN');
      const endDate = new Date(promo.endAt).toLocaleDateString('vi-VN');

      let discountText = '';
      if (promo.type === 'ORDER_PERCENT' || promo.type === 'SHIPPING_PERCENT') {
        // nếu là percent
        if (promo.value > 100) {
          discountText = `${promo.value.toLocaleString('vi-VN')} ₫`;
        } else {
          discountText = `${promo.value}%`;
        }
      } else if (promo.type === 'ORDER_FIXED') {
        discountText = `${promo.value.toLocaleString('vi-VN')} ₫`;
      }

      // 👉 Thêm onclick để điều hướng
      return `
        <div class="promo-card"
             style="cursor:pointer"
             onclick="window.location.href='${this._deriveBase()}/promotions/${promo.id}'">
          <div class="promo-header">
            <strong>🎁 ${promo.name}</strong>
          </div>
          <div class="promo-body">
            <p>${promo.description || 'Giảm giá đặc biệt'}</p>
            <p><i class="fas fa-tag"></i> Ưu đãi: <strong>${discountText}</strong></p>
            <p><i class="fas fa-calendar"></i> ${startDate} - ${endDate}</p>
          </div>
        </div>
      `;
    }).join('');

    const promoMessage = `
      <div class="chat-message received">
        <div>
          <div class="chat-message-bubble orders-list">
            <strong>🎁 Các chương trình khuyến mãi:</strong>
            ${promoCards}
          </div>
          <div class="chat-message-time">${new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</div>
        </div>
      </div>
    `;

    container.insertAdjacentHTML('beforeend', promoMessage);
    this.scrollToBottom(true);
    // (No server notification for promotions — decided to keep promotions as client-only UI)
  }

,

  async displayOrders(orders) {
    const container = document.getElementById('chatMessagesContainer');

    const statusText = {
      'PENDING': '⏳ Chờ xác nhận',
      'CONFIRMED': '✅ Đã xác nhận',
      'PREPARING': '👨‍🍳 Đang chuẩn bị',
      'SHIPPING': '🚚 Đang giao',
      'COMPLETED': '✅ Hoàn thành',
      'CANCELED': '❌ Đã hủy',
      'PAID': '💳 Đã thanh toán'
    };

    const orderCards = orders.map(order => {
      // ✅ DÙNG ĐÚNG FIELD TỪ API
      const orderCode = order.code || `#${order.id}`;
      const orderStatus = statusText[order.status] || order.status || 'Không rõ';
      const orderDate = order.createdAt
        ? new Date(order.createdAt).toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
          })
        : 'Chưa có thông tin';
      const totalAmount = order.total
        ? order.total.toLocaleString('vi-VN')
        : '0';
		let deliveryAddress = null;
		      const addrCandidates = [
		        order.deliveryAddress,
		        order.deliveryAddressLine,
		        order.address,
		        order.addressFull,
		        order.shippingAddress,
		        order.addressDetail,
		        order.receiverAddress,
		        order.branchName
		      ];

		      for (const a of addrCandidates) {
		        if (!a) continue;
		        if (typeof a === 'string' && a.trim()) { deliveryAddress = a.trim(); break; }
		        // sometimes backend returns an object for address
		        if (typeof a === 'object') {
		          const parts = [];
		          if (a.street) parts.push(a.street);
		          if (a.ward) parts.push(a.ward);
		          if (a.district) parts.push(a.district);
		          if (a.city) parts.push(a.city);
		          if (a.fullAddress) parts.unshift(a.fullAddress); // prefer fullAddress if present
		          const composed = parts.filter(Boolean).join(', ').trim();
		          if (composed) { deliveryAddress = composed; break; }
		        }
		      }

		      if (!deliveryAddress) {
		        // Last attempt: try to build from address object under order.addressObject or order.receiver
		        const obj = order.addressObject || order.receiver || null;
		        if (obj && typeof obj === 'object') {
		          const parts = [obj.street || obj.addressLine, obj.ward, obj.district, obj.city].filter(Boolean);
		          if (parts.length) deliveryAddress = parts.join(', ');
		        }
		      }

		      if (!deliveryAddress) deliveryAddress = 'Đang cập nhật';
	  const rawMethodLabel = order.paymentMethodLabel || order.paymentMethodName || order.paymentMethodDisplay;
	        let paymentMethod;
	        if (rawMethodLabel && typeof rawMethodLabel === 'string' && rawMethodLabel.trim()) {
	          // Use the human-friendly label returned by backend (keeps translations/formatting)
	          paymentMethod = rawMethodLabel;
	        } else {
	          // Normalize payment method codes and map to friendly labels as a fallback
	          const pmCode = (order.paymentMethod || '').toString().trim().toUpperCase();
	          if (pmCode === 'CASH' || pmCode === 'COD' || pmCode === 'PAY_ON_DELIVERY' || pmCode === 'PAY_ON_RECEIPT') {
	            // Treat cash / cash-on-delivery variants as payment on delivery
	            paymentMethod = '💵 Thanh toán khi nhận hàng';
	          } else if (pmCode === 'TRANSFER' || pmCode === 'BANK_TRANSFER' || pmCode === 'BANK' || pmCode === 'CARD') {
	            paymentMethod = '💳 Chuyển khoản';
	          } else if (pmCode === 'ONLINE' || pmCode === 'VNPAY' || pmCode === 'MOMO' || pmCode === 'PAYMENT_GATEWAY') {
	            paymentMethod = '💳 Thanh toán trực tuyến';
	          } else if (!pmCode) {
	            paymentMethod = 'Không xác định';
	          } else {
	            // Fallback: show raw code but log for debugging
	            console.warn('Unknown paymentMethod code from API:', order.paymentMethod);
	            paymentMethod = pmCode;
	          }
	        }

      return `
        <div class="order-card">
          <div class="order-header">
            <strong>${orderCode}</strong>
            <span class="order-status">${orderStatus}</span>
          </div>
          <div class="order-body">
            <p><i class="fas fa-calendar"></i> ${orderDate}</p>
            <p><i class="fas fa-dollar-sign"></i> <strong>${totalAmount} ₫</strong></p>
            <p><i class="fas fa-credit-card"></i> ${paymentMethod}</p>
            <p><i class="fas fa-map-marker-alt"></i> ${deliveryAddress}</p>
          </div>
          <a href="${this._deriveBase()}/orders" class="order-link">Xem chi tiết →</a>
        </div>
      `;
    }).join('');

    const orderMessage = `
      <div class="chat-message received">
        <div>
          <div class="chat-message-bubble orders-list">
            <strong>📦 Đơn hàng của bạn (${orders.length}):</strong>
            ${orderCards}
          </div>
          <div class="chat-message-time">${new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</div>
        </div>
      </div>
    `;

    container.insertAdjacentHTML('beforeend', orderMessage);
    this.scrollToBottom(true);

  },

  // ✅ GỬI TIN NHẮN TỰ ĐỘNG (CHỈ 1 LẦN)
  async sendAutoMessage(content, options = {}) {
    if (!this.roomId) {
      console.warn('⚠️ Cannot send message: no roomId');
      return;
    }

    // Ensure WebSocket is connected. If not, attempt to connect and wait.
    if (!this.stompClient || !this.stompClient.connected) {
      console.log('⏳ WebSocket not connected yet - attempting to connect...');
      try {
        this.connectWebSocket();
        await this.ensureConnected(5000); // wait up to 5s
      } catch (err) {
        console.warn('⚠️ Failed to establish WebSocket connection:', err);
        return;
      }
    }

    const message = {
      roomId: this.roomId,
      senderId: this.userId,
      content,
      senderType: 'USER'
    };

    console.log('📤 Sending message:', content);
    try {
      this.stompClient.send('/app/chat/send', {}, JSON.stringify(message));
    } catch (err) {
      console.error('❌ Error sending STOMP message', err);
      throw err;
    }

  // If caller wants to wait for the server broadcast (echo), support that
  if (options && options.waitForEcho) {
      return new Promise((resolve, reject) => {
        const match = content;
        const timeoutMs = options.timeout || 3000;

        const resolver = {
          match,
          userId: this.userId,
          resolve: (msg) => {
            clearTimeout(resolver._timeoutId);
            resolve(msg);
          },
          reject: (err) => {
            clearTimeout(resolver._timeoutId);
            reject(err);
          }
        };

        // timeout
        resolver._timeoutId = setTimeout(() => {
          // remove resolver
          this.pendingEchoResolvers = this.pendingEchoResolvers.filter(r => r !== resolver);
          resolver.reject(new Error('Timeout waiting for server echo'));
        }, timeoutMs);

        this.pendingEchoResolvers.push(resolver);
      });
    }

    return Promise.resolve();
  },

  // Gửi thông báo dạng "hệ thống" tới server để admin cũng nhìn thấy
  async sendSystemNotification(content, options = {}) {
    if (!this.roomId) {
      console.warn('⚠️ Cannot send system notification: no roomId');
      return;
    }

    if (!this.stompClient || !this.stompClient.connected) {
      try {
        this.connectWebSocket();
        await this.ensureConnected(3000);
      } catch (err) {
        console.warn('⚠️ Failed to connect before sending system notification', err);
        return;
      }
    }

    const message = {
      roomId: this.roomId,
      senderId: this.userId,
      content,
      senderType: options.type || 'SYSTEM'
    };

    try {
      this.stompClient.send('/app/chat/send', {}, JSON.stringify(message));
      console.log('🔔 System notification sent to server:', content);
    } catch (err) {
      console.error('❌ Error sending system notification', err);
    }
  },

  // Ensure stomp client is connected with a timeout
  ensureConnected(timeoutMs = 3000) {
    return new Promise((resolve, reject) => {
      const interval = 100;
      let waited = 0;

      const check = () => {
        if (this.stompClient && this.stompClient.connected) {
          return resolve(true);
        }
        waited += interval;
        if (waited >= timeoutMs) {
          return reject(new Error('Timeout waiting for STOMP connection'));
        }
        setTimeout(check, interval);
      };

      check();
    });
  },

  connectWebSocket() {
    if (this.stompClient && this.stompClient.connected) {
      console.log('⚠️ Already connected');
      return;
    }
    if (!this.roomId) return;

    const socket = new SockJS(this.WS_URL);
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null;

    // Include JWT token in CONNECT headers so server can authenticate WebSocket session
    const token = localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
    const connectHeaders = {};
    if (token) connectHeaders['Authorization'] = `Bearer ${token}`;

    // track reconnect attempts for exponential backoff (client-side)
    let reconnectAttempts = 0;

    const doConnect = () => {
      this.stompClient.connect(connectHeaders, () => {
        reconnectAttempts = 0;
        console.log('✅ Chat WebSocket Connected');

        this.stompClient.subscribe(`/topic/chat/room/${this.roomId}`, (message) => {
          try {
            const msg = JSON.parse(message.body);
            if (msg?.id) {
              // If there are pending echo resolvers, try to resolve matching ones
              if (Array.isArray(this.pendingEchoResolvers) && this.pendingEchoResolvers.length > 0) {
                const resolvers = this.pendingEchoResolvers.slice();
                resolvers.forEach(res => {
                  try {
                    if (res.userId && String(msg.senderId) !== String(res.userId)) return;
                    if (res.match && msg.content === res.match) {
                      res.resolve(msg);
                      this.pendingEchoResolvers = this.pendingEchoResolvers.filter(r => r !== res);
                    }
                  } catch (e) { console.warn('Error checking pending echo resolver', e); }
                });
              }

              this.displayMessage(msg);
              this.scrollToBottom();
            }
          } catch (e) { console.warn('Error parsing STOMP message', e); }
        });
      }, (err) => {
        console.warn('❌ STOMP connection error or disconnected', err);
        reconnectAttempts++;
        const backoff = Math.min(30000, 1000 * Math.pow(2, reconnectAttempts));
        setTimeout(() => {
          try {
            // recreate socket and stomp client to avoid stale state
            const sock = new SockJS(this.WS_URL);
            this.stompClient = Stomp.over(sock);
            this.stompClient.debug = null;
          } catch (e) { console.warn('Error recreating SockJS client', e); }
          doConnect();
        }, backoff);
      });
    };

    doConnect();
    this.stompClient.reconnect_delay = 0; // handled by our backoff
  },

  disconnectWebSocket() {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.disconnect(() => {
        console.log('✅ Disconnected');
      });
    }
    this.stompClient = null;
  },

  clearChatUI() {
    console.log('🧹 Clearing chat UI...');

    this.disconnectWebSocket();
    this.roomId = null;
    this.userId = null;
    this.lastUserId = null;
    this.menuListenersAdded = false; // ✅ Reset flag
    this.hideChatButton();

    const container = document.getElementById('chatMessagesContainer');
    if (container) {
      container.innerHTML = '<div class="chat-loading"><p>👋 Chào bạn! Hãy bắt đầu cuộc trò chuyện</p></div>';
    }

    localStorage.removeItem('currentChatRoomId');

    console.log('✅ Chat cleared completely');
  },

  displayMessage(msg) {
    const container = document.getElementById('chatMessagesContainer');
    const isSent = String(msg.senderId) === String(this.userId);

    const messageDiv = document.createElement('div');
    messageDiv.className = `chat-message ${isSent ? 'sent' : 'received'}`;

    const time = msg.timestamp
      ? new Date(msg.timestamp).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
      : '';

    // Preserve newlines in messages by replacing them with <br>
    const escaped = this.escapeHtml(msg.content).replace(/\n/g, '<br>');
    messageDiv.innerHTML = `
      <div>
        <div class="chat-message-bubble">${escaped}</div>
        <div class="chat-message-time">${time}</div>
      </div>
    `;
    container.appendChild(messageDiv);
  },

  sendMessage() {
    const input = document.getElementById('chatMessageInput');
    const content = (input?.value || '').trim();
    if (!content || !this.roomId) return;

    const message = { roomId: this.roomId, senderId: this.userId, content, senderType: 'USER' };

    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.send('/app/chat/send', {}, JSON.stringify(message));
      input.value = '';
      input.style.height = 'auto';
      this.scrollToBottom(true);
    } else {
      this.showError('Chưa kết nối. Vui lòng thử lại.');
    }
  },

  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text ?? '';
    return div.innerHTML;
  },

  scrollToBottom(force = false) {
    const container = document.getElementById('chatMessagesContainer');
    if (!container) return;
    const nearBottom = (container.scrollHeight - container.scrollTop - container.clientHeight) < 80;
    if (force || nearBottom) {
      requestAnimationFrame(() => { container.scrollTop = container.scrollHeight; });
    }
  },

  showError(message) {
    const container = document.getElementById('chatMessagesContainer');
    const errorDiv = document.createElement('div');
    errorDiv.style.cssText = 'color:#d32f2f;padding:10px;text-align:center;font-size:12px;';
    errorDiv.textContent = message;
    container.appendChild(errorDiv);
  }
};

window.ChatWidget = ChatWidget;

document.addEventListener('DOMContentLoaded', () => ChatWidget.init());