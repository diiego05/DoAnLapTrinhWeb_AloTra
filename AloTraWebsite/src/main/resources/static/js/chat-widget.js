// ===== CHAT WIDGET CONFIGURATION =====
const ChatWidget = {
  WS_URL: 'http://localhost:8080/alotra-website/ws/chat',
  API_URL: 'http://localhost:8080/alotra-website/api/chat',

  stompClient: null,
  roomId: null,
  userId: null,
  lastUserId: null,
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
      // ✅ LOAD ĐƠN HÀNG
      await this.loadUserOrders();
    } else if (action === 'promotion') {
      // ✅ LOAD KHUYẾN MÃI
      await this.loadPromotions();
    } else {
      // ✅ GỬI TIN NHẮN CHO ADMIN
      const messages = {
        'consult-tea': '🍵 Tôi cần tư vấn loại trà sữa phù hợp',
        'contact-admin': '📞 Tôi cần hỗ trợ từ tư vấn viên'
      };

      const content = messages[action];
      if (content) {
        this.sendAutoMessage(content);
      }
    }
  },

  // ✅ LOAD ĐƠN HÀNG
  async loadUserOrders() {
    try {
      const token = localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
      const res = await fetch('http://localhost:8080/alotra-website/api/orders', {
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

      this.displayOrders(pendingOrders);

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

      // ✅ SỬA URL: Gọi API lấy campaigns ACTIVE
      const res = await fetch('http://localhost:8080/alotra-website/api/admin/promotions/campaigns', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!res.ok) throw new Error('Không thể tải khuyến mãi');

      const promotions = await res.json();

      // ✅ LỌC KHUYẾN MÃI ĐANG ACTIVE
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

      this.displayPromotions(activePromotions);

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

  // ✅ HIỂN THỊ KHUYẾN MÃI
  displayPromotions(promotions) {
    const container = document.getElementById('chatMessagesContainer');

    const promoCards = promotions.map(promo => {
      const startDate = new Date(promo.startDate).toLocaleDateString('vi-VN');
      const endDate = new Date(promo.endDate).toLocaleDateString('vi-VN');

      return `
        <div class="promo-card">
          <div class="promo-header">
            <strong>🎁 ${promo.name}</strong>
          </div>
          <div class="promo-body">
            <p>${promo.description || 'Giảm giá đặc biệt'}</p>
            <p><i class="fas fa-percentage"></i> Giảm: <strong>${promo.discountValue}${promo.discountType === 'PERCENTAGE' ? '%' : ' ₫'}</strong></p>
            <p><i class="fas fa-calendar"></i> ${startDate} - ${endDate}</p>
            ${promo.minOrderValue ? `<p><i class="fas fa-shopping-cart"></i> Đơn tối thiểu: ${promo.minOrderValue.toLocaleString('vi-VN')} ₫</p>` : ''}
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
  },

  displayOrders(orders) {
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
      const deliveryAddress = order.deliveryAddress || order.branchName || 'Đang cập nhật';
      const paymentMethod = order.paymentMethod === 'CASH' ? '💵 Tiền mặt' : '💳 Chuyển khoản';

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
          <a href="/alotra-website/orders" class="order-link">Xem chi tiết →</a>
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
  async sendAutoMessage(content) {
    if (!this.roomId || !this.stompClient || !this.stompClient.connected) {
      console.warn('⚠️ Cannot send message: not connected');
      return;
    }

    const message = {
      roomId: this.roomId,
      senderId: this.userId,
      content,
      senderType: 'USER'
    };

    console.log('📤 Sending message:', content);
    this.stompClient.send('/app/chat/send', {}, JSON.stringify(message));
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

    this.stompClient.connect({}, () => {
      console.log('✅ Chat WebSocket Connected');

      this.stompClient.subscribe(`/topic/chat/room/${this.roomId}`, (message) => {
        const msg = JSON.parse(message.body);
        if (msg?.id) {
          console.log('📨 Received message:', msg.content);
          this.displayMessage(msg);
          this.scrollToBottom();
        }
      });
    }, () => {
      console.log('❌ WebSocket disconnected, reconnecting...');
      setTimeout(() => this.connectWebSocket(), 3000);
    });

    this.stompClient.reconnect_delay = 5000;
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

    messageDiv.innerHTML = `
      <div>
        <div class="chat-message-bubble">${this.escapeHtml(msg.content)}</div>
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