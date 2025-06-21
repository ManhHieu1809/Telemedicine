// admin-app.js
class AdminApp {
    constructor() {
        this.apiBaseUrl = 'http://localhost:8080/api';
        this.token = this.getToken();
        this.currentTab = 'dashboard';
        this.init();
    }

    init() {
        this.checkAuth();
        this.bindEvents();
        this.loadDashboardData();
        this.initializeTabs();
    }

    getToken() {
        return localStorage.getItem('adminToken') || sessionStorage.getItem('adminToken');
    }

    checkAuth() {
        if (!this.token) {
            window.location.href = 'admin-login.html';
            return;
        }
        this.loadUserInfo();
    }

    async loadUserInfo() {
        try {
            const response = await this.apiRequest('/users/profile');
            if (response.success && response.data.role === 'ADMIN') {
                document.getElementById('adminName').textContent = response.data.fullName || response.data.username;
                document.getElementById('headerUserName').textContent = response.data.fullName || response.data.username;
            } else {
                this.logout();
            }
        } catch (error) {
            console.error('Failed to load user info:', error);
            this.logout();
        }
    }

    bindEvents() {
        // Sidebar navigation
        document.querySelectorAll('.menu-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const tab = item.dataset.tab;
                this.switchTab(tab);
            });
        });

        // Logout buttons
        document.getElementById('logoutBtn').addEventListener('click', () => this.logout());
        document.getElementById('headerLogout').addEventListener('click', () => this.logout());

        // Sidebar toggle
        document.getElementById('sidebarToggle').addEventListener('click', () => {
            document.getElementById('sidebar').classList.toggle('collapsed');
        });

        // Mobile menu
        document.getElementById('mobileMenuBtn').addEventListener('click', () => {
            document.getElementById('sidebar').classList.toggle('show');
        });

        // User dropdown
        document.getElementById('userDropdownBtn').addEventListener('click', () => {
            document.getElementById('userDropdownMenu').classList.toggle('show');
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.user-dropdown')) {
                document.getElementById('userDropdownMenu').classList.remove('show');
            }
        });

        // Modal events
        document.getElementById('modalClose').addEventListener('click', () => this.closeModal());
        document.getElementById('modalCancel').addEventListener('click', () => this.closeModal());
        document.getElementById('modal').addEventListener('click', (e) => {
            if (e.target === document.getElementById('modal')) {
                this.closeModal();
            }
        });

        // Action buttons
        document.getElementById('addUserBtn').addEventListener('click', () => this.showAddUserModal());
        document.getElementById('addDoctorBtn').addEventListener('click', () => this.showAddDoctorModal());
        document.getElementById('exportPaymentsBtn').addEventListener('click', () => this.exportPayments());
        document.getElementById('generateReportBtn').addEventListener('click', () => this.generateReport());

        // Search and filter events
        this.bindFilterEvents();
    }

    bindFilterEvents() {
        // User filters
        document.getElementById('roleFilter')?.addEventListener('change', () => this.filterUsers());
        document.getElementById('userSearch')?.addEventListener('input', this.debounce(() => this.filterUsers(), 300));

        // Doctor filters
        document.getElementById('specialtyFilter')?.addEventListener('change', () => this.filterDoctors());
        document.getElementById('doctorSearch')?.addEventListener('input', this.debounce(() => this.filterDoctors(), 300));

        // Patient filters
        document.getElementById('genderFilter')?.addEventListener('change', () => this.filterPatients());
        document.getElementById('patientSearch')?.addEventListener('input', this.debounce(() => this.filterPatients(), 300));

        // Payment filters
        document.getElementById('paymentStatusFilter')?.addEventListener('change', () => this.filterPayments());
        document.getElementById('paymentFromDate')?.addEventListener('change', () => this.filterPayments());
        document.getElementById('paymentToDate')?.addEventListener('change', () => this.filterPayments());
    }

    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    async apiRequest(endpoint, options = {}) {
        try {
            const response = await fetch(`${this.apiBaseUrl}${endpoint}`, {
                headers: {
                    'Authorization': `Bearer ${this.token}`,
                    'Content-Type': 'application/json',
                    ...options.headers
                },
                ...options
            });

            if (response.status === 401) {
                this.logout();
                return;
            }

            const data = await response.json();
            return data;
        } catch (error) {
            console.error('API Request failed:', error);
            throw error;
        }
    }

    switchTab(tabName) {
        // Update active menu item
        document.querySelectorAll('.menu-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');

        // Hide all tab contents
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });

        // Show selected tab content
        document.getElementById(`${tabName}-content`).classList.add('active');

        // Update page title
        const titles = {
            dashboard: 'Dashboard',
            users: 'Quản lý người dùng',
            doctors: 'Quản lý bác sĩ',
            patients: 'Quản lý bệnh nhân',
            payments: 'Quản lý thanh toán',
            reports: 'Báo cáo',
            settings: 'Cài đặt'
        };
        document.getElementById('pageTitle').textContent = titles[tabName];

        this.currentTab = tabName;
        this.loadTabData(tabName);
    }

    initializeTabs() {
        this.switchTab('dashboard');
    }

    async loadTabData(tabName) {
        switch (tabName) {
            case 'dashboard':
                await this.loadDashboardData();
                break;
            case 'users':
                await this.loadUsers();
                break;
            case 'doctors':
                await this.loadDoctors();
                break;
            case 'patients':
                await this.loadPatients();
                break;
            case 'payments':
                await this.loadPayments();
                break;
            case 'reports':
                await this.loadReports();
                break;
        }
    }

    async loadDashboardData() {
        try {
            this.showLoading();

            // Load system report
            const systemReport = await this.apiRequest('/admin/reports');
            if (systemReport?.success) {
                this.updateDashboardStats(systemReport.data);
            }

            // Load payment dashboard data
            const paymentData = await this.apiRequest('/admin/payments/dashboard');
            if (paymentData?.success) {
                this.updatePaymentStats(paymentData.data);
            }

            // Load user activities
            const activities = await this.apiRequest('/admin/user-activities');
            if (activities?.success) {
                this.updateUserActivities(activities.data);
            }

        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            this.showError('Không thể tải dữ liệu dashboard');
        } finally {
            this.hideLoading();
        }
    }

    updateDashboardStats(data) {
        document.getElementById('totalUsers').textContent = data.totalUsers || 0;
        document.getElementById('totalDoctors').textContent = data.totalDoctors || 0;
        document.getElementById('totalPatients').textContent = data.totalPatients || 0;
    }

    updatePaymentStats(data) {
        // Kiểm tra cấu trúc data từ API
        const statsData = data.totalStats || data;
        const todayData = data.todayStats || {};
        const monthData = data.monthStats || {};

        // Dashboard stats (tổng doanh thu)
        const totalRevenue = document.getElementById('totalRevenue');
        if (totalRevenue) {
            totalRevenue.textContent = this.formatCurrency(statsData.totalRevenue || 0);
        }

        // Payment page stats
        const todayRevenue = document.getElementById('todayRevenue');
        if (todayRevenue) {
            todayRevenue.textContent = this.formatCurrency(todayData.totalRevenue || 0);
        }

        const monthRevenue = document.getElementById('monthRevenue');
        if (monthRevenue) {
            monthRevenue.textContent = this.formatCurrency(monthData.revenue || statsData.totalRevenue || 0);
        }

        const totalTransactions = document.getElementById('totalTransactions');
        if (totalTransactions) {
            totalTransactions.textContent = statsData.totalPayments || statsData.completedPayments || 0;
        }
    }

    updateUserActivities(activities) {
        const chartElement = document.getElementById('userActivityChart');
        if (activities && activities.length > 0) {
            // Simple activity display
            chartElement.innerHTML = `
                <div class="activity-list">
                    ${activities.slice(0, 5).map(activity => `
                        <div class="activity-item">
                            <span class="activity-user">${activity.username}</span>
                            <span class="activity-action">${activity.action}</span>
                            <span class="activity-time">${this.formatDateTime(activity.timestamp)}</span>
                        </div>
                    `).join('')}
                </div>
            `;
        } else {
            chartElement.innerHTML = '<p>Không có hoạt động gần đây</p>';
        }
    }

    async loadUsers() {
        try {
            this.showTableLoading('usersTableBody');

            // Load all users (you might need to create this endpoint)
            const users = await this.apiRequest('/admin/users');
            if (users?.success) {
                this.displayUsers(users.data);
            }
        } catch (error) {
            console.error('Failed to load users:', error);
            this.showTableError('usersTableBody', 'Không thể tải danh sách người dùng');
        }
    }

    displayUsers(users) {
        const tbody = document.getElementById('usersTableBody');
        if (!users || users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">Không có dữ liệu</td></tr>';
            return;
        }

        tbody.innerHTML = users.map(user => `
            <tr>
                <td>${user.id}</td>
                <td>${user.username}</td>
                <td>${user.email}</td>
                <td><span class="status-badge status-${this.getRoleClass(user.role)}">${user.role}</span></td>
                <td>${this.formatDateTime(user.createdAt)}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-outline action-btn" onclick="adminApp.editUser(${user.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-danger action-btn" onclick="adminApp.deleteUser(${user.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    async loadDoctors() {
        try {
            this.showTableLoading('doctorsTableBody');

            const doctors = await this.apiRequest('/users/doctors');
            if (doctors?.success) {
                this.displayDoctors(doctors.data);
            }
        } catch (error) {
            console.error('Failed to load doctors:', error);
            this.showTableError('doctorsTableBody', 'Không thể tải danh sách bác sĩ');
        }
    }

    displayDoctors(doctors) {
        const tbody = document.getElementById('doctorsTableBody');
        if (!doctors || doctors.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">Không có dữ liệu</td></tr>';
            return;
        }

        tbody.innerHTML = doctors.map(doctor => `
            <tr>
                <td>${doctor.id}</td>
                <td>${doctor.fullName}</td>
                <td>${doctor.specialty}</td>
                <td>${doctor.experience} năm</td>
                <td>${doctor.phone}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-outline action-btn" onclick="adminApp.editDoctor(${doctor.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-danger action-btn" onclick="adminApp.deleteDoctor(${doctor.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    async loadPatients() {
        try {
            this.showTableLoading('patientsTableBody');

            const patients = await this.apiRequest('/admin/users/patients');
            if (patients?.success) {
                this.displayPatients(patients.data);
            }
        } catch (error) {
            console.error('Failed to load patients:', error);
            this.showTableError('patientsTableBody', 'Không thể tải danh sách bệnh nhân');
        }
    }

    displayPatients(patients) {
        const tbody = document.getElementById('patientsTableBody');
        if (!patients || patients.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">Không có dữ liệu</td></tr>';
            return;
        }

        tbody.innerHTML = patients.map(patient => `
            <tr>
                <td>${patient.id}</td>
                <td>${patient.fullName}</td>
                <td>${this.formatDate(patient.dateOfBirth)}</td>
                <td>${this.getGenderText(patient.gender)}</td>
                <td>${patient.phone}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-outline action-btn" onclick="adminApp.editPatient(${patient.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-danger action-btn" onclick="adminApp.deletePatient(${patient.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    async loadPayments() {
        try {
            this.showTableLoading('paymentsTableBody');

            // Load payment dashboard stats
            try {
                const paymentDashboard = await this.apiRequest('/admin/payments/dashboard');
                if (paymentDashboard?.success) {
                    this.updatePaymentStats(paymentDashboard.data);
                }
            } catch (error) {
                console.error('Failed to load payment dashboard:', error);
            }

            // Load payments list với phân trang
            await this.loadPaymentsList();


        } catch (error) {
            console.error('Failed to load payments:', error);
            this.showTableError('paymentsTableBody', 'Không thể tải danh sách thanh toán');
        }
    }

    async loadPaymentsList(page = 0, size = 10) {
        try {
            // Kiểm tra filter theo ngày
            const fromDate = document.getElementById('paymentFromDate')?.value;
            const toDate = document.getElementById('paymentToDate')?.value;

            let apiUrl;
            let params = new URLSearchParams();

            if (fromDate && toDate) {
                // Sử dụng API filter theo ngày
                apiUrl = '/admin/payments/by-date-range';
                params.append('startDate', fromDate);
                params.append('endDate', toDate);
            } else {
                // Sử dụng API lấy tất cả với phân trang
                apiUrl = '/admin/payments/all';
                params.append('page', page.toString());
                params.append('size', size.toString());
            }

            const response = await this.apiRequest(`${apiUrl}?${params.toString()}`);

            if (response?.success) {
                // Kiểm tra cấu trúc response
                let payments = response.data;

                // Nếu có phân trang, data có thể nằm trong content
                if (response.data.content) {
                    payments = response.data.content;
                    this.updatePagination(response.data);
                }

                this.displayPayments(payments);
                console.log('Payments loaded:', payments);
            } else {
                throw new Error('API response not successful');
            }

        } catch (error) {
            console.error('Failed to load payments list:', error);

            // Fallback với mock data
            const mockPayments = [
                {
                    id: 1,
                    transactionId: 'TXN001',
                    patientName: 'Nguyễn Văn An',
                    doctorName: 'BS. Trần Thị Bình',
                    amount: 410000,
                    status: 'SUCCESS',
                    paymentMethod: 'VNPAY',
                    createdAt: '2025-06-20T11:30:00'
                }
            ];
            this.displayPayments(mockPayments);
        }
    }

    updatePagination(pageData) {
        let paginationContainer = document.getElementById('paymentsPagination');

        // Tạo container nếu chưa có
        if (!paginationContainer) {
            paginationContainer = document.createElement('div');
            paginationContainer.id = 'paymentsPagination';
            paginationContainer.className = 'pagination-container';

            // Thêm vào sau table-container
            const tableContainer = document.querySelector('#payments-content .table-container');
            if (tableContainer) {
                tableContainer.parentNode.insertBefore(paginationContainer, tableContainer.nextSibling);
            }
        }

        const currentPage = pageData.number || 0;
        const totalPages = pageData.totalPages || 1;
        const totalElements = pageData.totalElements || 0;
        const pageSize = pageData.size || 10;

        // Ẩn pagination nếu chỉ có 1 trang
        if (totalPages <= 1) {
            paginationContainer.style.display = 'none';
            return;
        }

        paginationContainer.style.display = 'flex';

        let paginationHTML = `
        <div class="pagination-info">
            Hiển thị ${currentPage * pageSize + 1} - ${Math.min((currentPage + 1) * pageSize, totalElements)} 
            trong ${totalElements} kết quả
        </div>
        <div class="pagination-controls">
            <button class="btn btn-outline ${currentPage === 0 ? 'disabled' : ''}" 
                    onclick="adminApp.loadPaymentsList(${currentPage - 1})" 
                    ${currentPage === 0 ? 'disabled' : ''}>
                <i class="fas fa-chevron-left"></i>
            </button>
    `;

        // Hiển thị số trang (max 5 trang)
        const start = Math.max(0, currentPage - 2);
        const end = Math.min(totalPages, currentPage + 3);

        if (start > 0) {
            paginationHTML += `<button class="btn btn-outline" onclick="adminApp.loadPaymentsList(0)">1</button>`;
            if (start > 1) {
                paginationHTML += `<span>...</span>`;
            }
        }

        for (let i = start; i < end; i++) {
            paginationHTML += `
            <button class="btn btn-outline ${i === currentPage ? 'active' : ''}" 
                    onclick="adminApp.loadPaymentsList(${i})">
                ${i + 1}
            </button>
        `;
        }

        if (end < totalPages) {
            if (end < totalPages - 1) {
                paginationHTML += `<span>...</span>`;
            }
            paginationHTML += `<button class="btn btn-outline" onclick="adminApp.loadPaymentsList(${totalPages - 1})">${totalPages}</button>`;
        }

        paginationHTML += `
            <button class="btn btn-outline ${currentPage === totalPages - 1 ? 'disabled' : ''}" 
                    onclick="adminApp.loadPaymentsList(${currentPage + 1})"
                    ${currentPage === totalPages - 1 ? 'disabled' : ''}>
                <i class="fas fa-chevron-right"></i>
            </button>
        </div>
    `;

        paginationContainer.innerHTML = paginationHTML;
    }

    filterPaymentsByStatus() {
        const selectedStatus = document.getElementById('paymentStatusFilter')?.value;
        const rows = document.querySelectorAll('#paymentsTable tbody tr');

        rows.forEach(row => {
            if (row.cells.length === 1) return; // Skip "no data" row

            const statusCell = row.cells[4]; // Status column
            const statusText = statusCell.textContent.trim();

            if (!selectedStatus || statusText.includes(selectedStatus)) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    }

    bindAdminEvents() {
        // ... existing code ...

        // Payment date filters
        document.getElementById('paymentFromDate')?.addEventListener('change', () => {
            if (this.currentTab === 'payments') {
                this.loadPaymentsList();
            }
        });

        document.getElementById('paymentToDate')?.addEventListener('change', () => {
            if (this.currentTab === 'payments') {
                this.loadPaymentsList();
            }
        });

        // Payment status filter
        document.getElementById('paymentStatusFilter')?.addEventListener('change', () => {
            if (this.currentTab === 'payments') {
                this.filterPaymentsByStatus();
            }
        });
    }

    displayPayments(payments) {
        const tbody = document.getElementById('paymentsTableBody');
        if (!payments || payments.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">Không có dữ liệu</td></tr>';
            return;
        }

        tbody.innerHTML = payments.map(payment => `
        <tr>
            <td>${payment.transactionId || payment.id}</td>
            <td>${payment.patientName || payment.patient?.fullName || 'N/A'}</td>
            <td>${payment.doctorName || payment.doctor?.fullName || 'N/A'}</td>
            <td>${this.formatCurrency(payment.amount)}</td>
            <td><span class="status-badge status-${this.getPaymentStatusClass(payment.status)}">${payment.status}</span></td>
            <td>${this.formatDateTime(payment.createdAt || payment.paymentDate)}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-outline action-btn" onclick="adminApp.viewPaymentDetails('${payment.transactionId || payment.id}')">
                        <i class="fas fa-eye"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
    }

    async loadReports() {
        // Implementation for loading reports
        console.log('Loading reports...');
    }

    // Modal Methods
    showModal(title, content, footerButtons = null) {
        document.getElementById('modalTitle').textContent = title;
        document.getElementById('modalBody').innerHTML = content;

        if (footerButtons) {
            document.getElementById('modalFooter').innerHTML = footerButtons;
        }

        document.getElementById('modal').classList.add('show');
    }

    closeModal() {
        document.getElementById('modal').classList.remove('show');
    }

    showAddUserModal() {
        const content = `
            <form id="addUserForm">
                <div class="form-group">
                    <label>Tên đăng nhập:</label>
                    <input type="text" name="username" required>
                </div>
                <div class="form-group">
                    <label>Email:</label>
                    <input type="email" name="email" required>
                </div>
                <div class="form-group">
                    <label>Mật khẩu:</label>
                    <input type="password" name="password" required>
                </div>
                <div class="form-group">
                    <label>Vai trò:</label>
                    <select name="role" required>
                        <option value="ADMIN">Admin</option>
                        <option value="DOCTOR">Bác sĩ</option>
                        <option value="PATIENT">Bệnh nhân</option>
                    </select>
                </div>
            </form>
        `;

        const buttons = `
            <button class="btn btn-secondary" onclick="adminApp.closeModal()">Hủy</button>
            <button class="btn btn-primary" onclick="adminApp.submitAddUser()">Thêm người dùng</button>
        `;

        this.showModal('Thêm người dùng mới', content, buttons);
    }

    showAddDoctorModal() {
        const content = `
            <form id="addDoctorForm">
                <div class="form-group">
                    <label>Tên đăng nhập:</label>
                    <input type="text" name="username" required>
                </div>
                <div class="form-group">
                    <label>Email:</label>
                    <input type="email" name="email" required>
                </div>
                <div class="form-group">
                    <label>Mật khẩu:</label>
                    <input type="password" name="password" required>
                </div>
                <div class="form-group">
                    <label>Họ tên:</label>
                    <input type="text" name="fullName" required>
                </div>
                <div class="form-group">
                    <label>Chuyên khoa:</label>
                    <select name="specialty" required>
                        <option value="Nội khoa">Nội khoa</option>
                        <option value="Ngoại khoa">Ngoại khoa</option>
                        <option value="Sản phụ khoa">Sản phụ khoa</option>
                        <option value="Nhi khoa">Nhi khoa</option>
                        <option value="Tim mạch">Tim mạch</option>
                        <option value="Da liễu">Da liễu</option>
                        <option value="Mắt">Mắt</option>
                        <option value="Tai mũi họng">Tai mũi họng</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Kinh nghiệm (năm):</label>
                    <input type="number" name="experience" min="0" required>
                </div>
                <div class="form-group">
                    <label>Số điện thoại:</label>
                    <input type="tel" name="phone" required>
                </div>
                <div class="form-group">
                    <label>Địa chỉ:</label>
                    <textarea name="address" rows="3"></textarea>
                </div>
            </form>
        `;

        const buttons = `
            <button class="btn btn-secondary" onclick="adminApp.closeModal()">Hủy</button>
            <button class="btn btn-primary" onclick="adminApp.submitAddDoctor()">Thêm bác sĩ</button>
        `;

        this.showModal('Thêm bác sĩ mới', content, buttons);
    }

    async submitAddUser() {
        const form = document.getElementById('addUserForm');
        const formData = new FormData(form);
        const userData = Object.fromEntries(formData.entries());

        try {
            this.showLoading();

            // Create user account first
            const response = await this.apiRequest('/auth/register', {
                method: 'POST',
                body: JSON.stringify(userData)
            });

            if (response?.success) {
                this.showSuccess('Thêm người dùng thành công!');
                this.closeModal();
                if (this.currentTab === 'users') {
                    await this.loadUsers();
                }
            } else {
                this.showError(response?.message || 'Không thể thêm người dùng');
            }
        } catch (error) {
            console.error('Failed to add user:', error);
            this.showError('Có lỗi xảy ra khi thêm người dùng');
        } finally {
            this.hideLoading();
        }
    }

    async submitAddDoctor() {
        const form = document.getElementById('addDoctorForm');
        const formData = new FormData(form);
        const doctorData = Object.fromEntries(formData.entries());

        try {
            this.showLoading();

            const response = await this.apiRequest('/auth/create-doctor', {
                method: 'POST',
                body: JSON.stringify(doctorData)
            });

            if (response?.success) {
                this.showSuccess('Thêm bác sĩ thành công!');
                this.closeModal();
                if (this.currentTab === 'doctors') {
                    await this.loadDoctors();
                }
            } else {
                this.showError(response?.message || 'Không thể thêm bác sĩ');
            }
        } catch (error) {
            console.error('Failed to add doctor:', error);
            this.showError('Có lỗi xảy ra khi thêm bác sĩ');
        } finally {
            this.hideLoading();
        }
    }

    // CRUD Operations
    async editUser(userId) {
        // Implementation for editing user
        console.log('Edit user:', userId);
    }

    async deleteUser(userId) {
        if (confirm('Bạn có chắc chắn muốn xóa người dùng này?')) {
            try {
                this.showLoading();

                const response = await this.apiRequest(`/admin/users/${userId}`, {
                    method: 'DELETE'
                });

                if (response?.success) {
                    this.showSuccess('Xóa người dùng thành công!');
                    await this.loadUsers();
                } else {
                    this.showError('Không thể xóa người dùng');
                }
            } catch (error) {
                console.error('Failed to delete user:', error);
                this.showError('Có lỗi xảy ra khi xóa người dùng');
            } finally {
                this.hideLoading();
            }
        }
    }

    async editDoctor(doctorId) {
        console.log('Edit doctor:', doctorId);
    }

    async deleteDoctor(doctorId) {
        if (confirm('Bạn có chắc chắn muốn xóa bác sĩ này?')) {
            try {
                this.showLoading();

                const response = await this.apiRequest(`/admin/users/doctors/${doctorId}`, {
                    method: 'DELETE'
                });

                if (response?.success) {
                    this.showSuccess('Xóa bác sĩ thành công!');
                    await this.loadDoctors();
                } else {
                    this.showError('Không thể xóa bác sĩ');
                }
            } catch (error) {
                console.error('Failed to delete doctor:', error);
                this.showError('Có lỗi xảy ra khi xóa bác sĩ');
            } finally {
                this.hideLoading();
            }
        }
    }

    async editPatient(patientId) {
        console.log('Edit patient:', patientId);
    }

    async deletePatient(patientId) {
        if (confirm('Bạn có chắc chắn muốn xóa bệnh nhân này?')) {
            try {
                this.showLoading();

                const response = await this.apiRequest(`/admin/users/patients/${patientId}`, {
                    method: 'DELETE'
                });

                if (response?.success) {
                    this.showSuccess('Xóa bệnh nhân thành công!');
                    await this.loadPatients();
                } else {
                    this.showError('Không thể xóa bệnh nhân');
                }
            } catch (error) {
                console.error('Failed to delete patient:', error);
                this.showError('Có lỗi xảy ra khi xóa bệnh nhân');
            } finally {
                this.hideLoading();
            }
        }
    }

    async viewPaymentDetails(transactionId) {
        console.log('View payment details:', transactionId);
    }

    // Filter Methods
    filterUsers() {
        const role = document.getElementById('roleFilter').value;
        const search = document.getElementById('userSearch').value.toLowerCase();

        // In a real implementation, you would filter the data and re-render the table
        console.log('Filter users:', { role, search });
    }

    filterDoctors() {
        const specialty = document.getElementById('specialtyFilter').value;
        const search = document.getElementById('doctorSearch').value.toLowerCase();

        console.log('Filter doctors:', { specialty, search });
    }

    filterPatients() {
        const gender = document.getElementById('genderFilter').value;
        const search = document.getElementById('patientSearch').value.toLowerCase();

        console.log('Filter patients:', { gender, search });
    }

    filterPayments() {
        const status = document.getElementById('paymentStatusFilter').value;
        const fromDate = document.getElementById('paymentFromDate').value;
        const toDate = document.getElementById('paymentToDate').value;

        console.log('Filter payments:', { status, fromDate, toDate });
    }

    // Export and Report Methods
    async exportPayments() {
        try {
            this.showLoading();

            const response = await this.apiRequest('/admin/payments/export?format=xlsx');

            if (response) {
                // Handle file download
                const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `payments-export-${new Date().toISOString().split('T')[0]}.xlsx`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);

                this.showSuccess('Xuất báo cáo thành công!');
            }
        } catch (error) {
            console.error('Failed to export payments:', error);
            this.showError('Không thể xuất báo cáo');
        } finally {
            this.hideLoading();
        }
    }

    async generateReport() {
        this.showSuccess('Tính năng tạo báo cáo đang được phát triển');
    }

    // Utility Methods
    formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    }

    formatDate(dateString) {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('vi-VN');
    }

    formatDateTime(dateString) {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleString('vi-VN');
    }

    getRoleClass(role) {
        const classes = {
            'ADMIN': 'error',
            'DOCTOR': 'info',
            'PATIENT': 'success'
        };
        return classes[role] || 'info';
    }

    getGenderText(gender) {
        const texts = {
            'MALE': 'Nam',
            'FEMALE': 'Nữ',
            'OTHER': 'Khác'
        };
        return texts[gender] || gender;
    }

    getPaymentStatusClass(status) {
        const classes = {
            'SUCCESS': 'success',
            'PENDING': 'warning',
            'FAILED': 'error'
        };
        return classes[status] || 'info';
    }

    // Loading and Message Methods
    showLoading() {
        document.getElementById('loadingOverlay').classList.add('show');
    }

    hideLoading() {
        document.getElementById('loadingOverlay').classList.remove('show');
    }

    showTableLoading(tableBodyId) {
        const tbody = document.getElementById(tableBodyId);
        const colCount = tbody.closest('table').querySelectorAll('th').length;
        tbody.innerHTML = `<tr><td colspan="${colCount}" class="loading">Đang tải dữ liệu...</td></tr>`;
    }

    showTableError(tableBodyId, message) {
        const tbody = document.getElementById(tableBodyId);
        const colCount = tbody.closest('table').querySelectorAll('th').length;
        tbody.innerHTML = `<tr><td colspan="${colCount}" class="text-center text-error">${message}</td></tr>`;
    }

    showSuccess(message) {
        this.showNotification(message, 'success');
    }

    showError(message) {
        this.showNotification(message, 'error');
    }

    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
                <span>${message}</span>
            </div>
            <button class="notification-close">
                <i class="fas fa-times"></i>
            </button>
        `;

        // Add to page
        document.body.appendChild(notification);

        // Auto remove after 5 seconds
        setTimeout(() => {
            notification.remove();
        }, 5000);

        // Manual close
        notification.querySelector('.notification-close').addEventListener('click', () => {
            notification.remove();
        });
    }

    logout() {
        localStorage.removeItem('adminToken');
        sessionStorage.removeItem('adminToken');
        localStorage.removeItem('adminUser');
        sessionStorage.removeItem('adminUser');
        window.location.href = 'admin-login.html';
    }
}

// Initialize the app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.adminApp = new AdminApp();
});

// Add notification styles dynamically
const notificationStyles = `
    .notification {
        position: fixed;
        top: 20px;
        right: 20px;
        background: white;
        border-radius: 8px;
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 1rem;
        margin-bottom: 1rem;
        z-index: 10000;
        min-width: 300px;
        animation: slideInRight 0.3s ease-out;
    }

    .notification-success {
        border-left: 4px solid var(--success-color);
    }

    .notification-error {
        border-left: 4px solid var(--error-color);
    }

    .notification-info {
        border-left: 4px solid var(--info-color);
    }

    .notification-content {
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    .notification-success i {
        color: var(--success-color);
    }

    .notification-error i {
        color: var(--error-color);
    }

    .notification-info i {
        color: var(--info-color);
    }

    .notification-close {
        background: none;
        border: none;
        cursor: pointer;
        color: var(--gray-400);
        padding: 0.25rem;
    }

    .notification-close:hover {
        color: var(--gray-600);
    }

    @keyframes slideInRight {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }

    .activity-list {
        max-height: 300px;
        overflow-y: auto;
    }

    .activity-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem;
        border-bottom: 1px solid var(--gray-200);
    }

    .activity-item:last-child {
        border-bottom: none;
    }

    .activity-user {
        font-weight: 500;
        color: var(--gray-800);
    }

    .activity-action {
        color: var(--gray-600);
        font-size: 0.9rem;
    }
  `;