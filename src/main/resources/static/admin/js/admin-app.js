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
        // Cleanup khi rời khỏi payments tab
        if (this.currentTab === 'payments' && tabName !== 'payments') {
            this.cleanupPaymentFilters();
        }

        // Update active menu item
        document.querySelectorAll('.menu-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[data-tab="${tabName}"]`)?.classList.add('active');

        // Hide all tab contents
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });

        // Show selected tab content
        const targetContent = document.getElementById(`${tabName}-content`);
        if (targetContent) {
            targetContent.classList.add('active');
        }

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

        const titleElement = document.getElementById('pageTitle');
        if (titleElement) {
            titleElement.textContent = titles[tabName] || tabName;
        }

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



    updateDashboardStats(data) {
        document.getElementById('totalUsers').textContent = data.totalUsers || 0;
        document.getElementById('totalDoctors').textContent = data.totalDoctors || 0;
        document.getElementById('totalPatients').textContent = data.totalPatients || 0;
    }

    updatePaymentStats(data) {
        console.log('Updating payment stats with data:', data);

        // Format số tiền
        const formatCurrency = (amount) => {
            return new Intl.NumberFormat('vi-VN', {
                style: 'currency',
                currency: 'VND'
            }).format(amount || 0);
        };

        // Lấy các element
        const totalRevenue = document.getElementById('totalRevenue');
        const todayRevenue = document.getElementById('todayRevenue');
        const monthRevenue = document.getElementById('monthRevenue');
        const totalTransactions = document.getElementById('totalTransactions');

        // Cập nhật theo cấu trúc dữ liệu mới
        if (data.totalStats) {
            if (totalRevenue) {
                totalRevenue.textContent = formatCurrency(data.totalStats.totalRevenue);
            }
            if (totalTransactions) {
                totalTransactions.textContent = data.totalStats.totalPayments || 0;
            }
        }

        if (data.todayStats) {
            if (todayRevenue) {
                todayRevenue.textContent = formatCurrency(data.todayStats.totalRevenue);
            }
        }

        if (data.monthStats) {
            if (monthRevenue) {
                monthRevenue.textContent = formatCurrency(data.monthStats.revenue);
            }
        }

        // Debug logging
        console.log('Payment stats updated:', {
            totalRevenue: data.totalStats?.totalRevenue,
            todayRevenue: data.todayStats?.totalRevenue,
            monthRevenue: data.monthStats?.revenue,
            totalTransactions: data.totalStats?.totalPayments
        });
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
            console.log('🔄 Loading users...');
            this.showTableLoading('usersTableBody');

            try {
                const response = await this.apiRequest('/admin/users');

                if (response?.success) {
                    this.originalUsers = response.data;
                    console.log(' Loaded users from API:', this.originalUsers);
                } else {
                    throw new Error('API response not successful');
                }
            } catch (apiError) {
                console.warn('️API failed, using mock data:', apiError);
                this.originalUsers = this.getMockUsers();
            }

            this.initializeUserFilters();
            this.filterUsers();

        } catch (error) {
            console.error('❌ Failed to load users:', error);
            this.showTableError('usersTableBody', 'Không thể tải danh sách người dùng');
        }
    }

    displayUsers(users) {
        const tbody = document.getElementById('usersTableBody');
        if (!tbody) return;

        if (!users || users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">Không có dữ liệu</td></tr>';
            return;
        }

        tbody.innerHTML = users.map(user => `
        <tr>
            <td>${user.userId}</td>
            <td>${user.username}</td>
            <td>${user.fullName || 'N/A'}</td>
            <td>${user.email}</td>
            <td><span class="role-badge role-${this.getRoleClass(user.role)}">${this.getRoleText(user.role)}</span></td>
            <td><span class="status-badge status-${this.getStatusClass(user.status)}">${this.getStatusText(user.status)}</span></td>
            <td>${this.formatDateTime(user.createdAt)}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn action-btn btn-view" onclick="adminApp.viewUser(${user.id})">Xem</button>
                    <button class="btn action-btn btn-edit" onclick="adminApp.editUser(${user.id})">Sửa</button>
                    <button class="btn action-btn btn-delete" onclick="adminApp.deleteUser(${user.id})">Xóa</button>
                </div>
            </td>
        </tr>
    `).join('');
    }

    viewUser(id) {
        console.log('View user:', id);
        alert(`Xem chi tiết người dùng ID: ${id}`);
    }

    editUser(id) {
        console.log('Edit user:', id);
        alert(`Chỉnh sửa người dùng ID: ${id}`);
    }

    viewDoctor(id) {
        console.log('View doctor:', id);
        alert(`Xem chi tiết bác sĩ ID: ${id}`);
    }

    viewPatient(id) {
        console.log('View patient:', id);
        alert(`Xem chi tiết bệnh nhân ID: ${id}`);
    }




    initializeUserFilters() {
        // Bind filter events
        const roleFilter = document.getElementById('roleFilter');
        const statusFilter = document.getElementById('userStatusFilter');
        const searchInput = document.getElementById('userSearch');

        if (roleFilter) {
            roleFilter.addEventListener('change', () => this.filterUsers());
        }
        if (statusFilter) {
            statusFilter.addEventListener('change', () => this.filterUsers());
        }
        if (searchInput) {
            searchInput.addEventListener('input', this.debounce(() => this.filterUsers(), 300));
        }

        // Bind quick filter buttons
        const quickFilterButtons = document.querySelectorAll('.user-quick-filter');
        quickFilterButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                const filterType = e.target.getAttribute('data-filter');
                this.setUserQuickFilter(filterType);
            });
        });
    }

    async loadDashboardData() {
        try {
            this.showLoading();

            // Load system report
            try {
                const systemReport = await this.apiRequest('/admin/reports');
                if (systemReport?.success) {
                    this.updateDashboardStats(systemReport.data);
                }
            } catch (error) {
                console.error('Failed to load system report:', error);
                // Fallback data
                this.updateDashboardStats({
                    totalUsers: 156,
                    totalDoctors: 45,
                    totalPatients: 111
                });
            }

            // Load payment dashboard data
            try {
                const paymentData = await this.apiRequest('/admin/payments/dashboard');
                if (paymentData?.success) {
                    this.updatePaymentStats(paymentData.data);
                }
            } catch (error) {
                console.error('Failed to load payment dashboard:', error);
                // Fallback data
                this.updatePaymentStats({
                    totalRevenue: 25000000,
                    todayRevenue: 1500000,
                    monthRevenue: 8500000,
                    totalTransactions: 342
                });
            }

            // Load user activities
            try {
                const activities = await this.apiRequest('/admin/user-activities');
                if (activities?.success) {
                    this.updateUserActivities(activities.data);
                }
            } catch (error) {
                console.error('Failed to load user activities:', error);
                // Hiển thị placeholder
                document.getElementById('userActivityChart').innerHTML =
                    '<p style="text-align: center; padding: 2rem; color: #666;">Không có dữ liệu hoạt động</p>';
            }

        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            this.showError('Không thể tải dữ liệu dashboard');
        } finally {
            this.hideLoading();
        }
    }


    async loadDoctors() {
        try {
            console.log(' Loading doctors...');
            this.showTableLoading('doctorsTableBody');


            try {
                const response = await this.apiRequest('/users/doctors');

                if (response?.success) {
                    this.originalDoctors = response.data;
                    console.log(' Loaded doctors from API:', this.originalDoctors);
                } else {
                    throw new Error('API response not successful');
                }
            } catch (apiError) {
                console.warn('API failed, using mock data:', apiError);
                this.originalDoctors = this.getMockDoctors();
            }

            this.initializeDoctorFilters();
            this.filterDoctors();

        } catch (error) {
            console.error(' Failed to load doctors:', error);
            this.showTableError('doctorsTableBody', 'Không thể tải danh sách bác sĩ');
        }
    }

    initializeDoctorFilters() {
        const specialtyFilter = document.getElementById('specialtyFilter');
        const statusFilter = document.getElementById('doctorStatusFilter');
        const experienceFilter = document.getElementById('experienceFilter');
        const searchInput = document.getElementById('doctorSearch');

        if (specialtyFilter) {
            specialtyFilter.addEventListener('change', () => this.filterDoctors());
        }
        if (statusFilter) {
            statusFilter.addEventListener('change', () => this.filterDoctors());
        }
        if (experienceFilter) {
            experienceFilter.addEventListener('change', () => this.filterDoctors());
        }
        if (searchInput) {
            searchInput.addEventListener('input', this.debounce(() => this.filterDoctors(), 300));
        }


        const quickFilterButtons = document.querySelectorAll('.doctor-quick-filter');
        quickFilterButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                const filterType = e.target.getAttribute('data-filter');
                console.log('🔘 Doctor quick filter clicked:', filterType);
                this.setDoctorQuickFilter(filterType);
            });
        });

        console.log('✅ Doctor filters initialized');
    }


    setDoctorQuickFilter(type) {
        const specialtyFilter = document.getElementById('specialtyFilter');
        const statusFilter = document.getElementById('doctorStatusFilter');
        const experienceFilter = document.getElementById('experienceFilter');
        const searchInput = document.getElementById('doctorSearch');

        if (specialtyFilter) specialtyFilter.value = '';
        if (statusFilter) statusFilter.value = '';
        if (experienceFilter) experienceFilter.value = '';
        if (searchInput) searchInput.value = '';

        switch (type) {
            case 'cardiology':
                if (specialtyFilter) {
                    specialtyFilter.value = 'Tim mạch';
                }
                break;
            case 'neurology':
                if (specialtyFilter) {
                    specialtyFilter.value = 'Thần Kinh';
                }
                break;
            case 'pediatrics':
                if (specialtyFilter) {
                    specialtyFilter.value = 'Nhi khoa';
                }
                break;
            case 'active':
                if (statusFilter) {
                    statusFilter.value = 'ACTIVE';
                }
                break;
            case 'reset':
                break;
        }

        this.filterDoctors();
    }

    displayDoctors(doctors) {
        const tbody = document.getElementById('doctorsTableBody');
        if (!tbody) return;

        if (!doctors || doctors.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">Không có dữ liệu</td></tr>';
            return;
        }

        tbody.innerHTML = doctors.map(doctor => `
        <tr>
            <td>${doctor.id}</td>
            <td>${doctor.fullName}</td>
            <td>${doctor.specialty}</td>
            <td>${doctor.experience || 0} năm</td>
            <td>${doctor.phone || 'N/A'}</td>
            <td>${doctor.email || 'N/A'}</td>
            <td><span class="status-badge status-${this.getStatusClass(doctor.status)}">${this.getStatusText(doctor.status)}</span></td>
            <td>
                <div class="action-buttons">
                    <button class="btn action-btn btn-view" onclick="adminApp.viewDoctor(${doctor.id})">Xem</button>
                    <button class="btn action-btn btn-edit" onclick="adminApp.editDoctor(${doctor.id})">Sửa</button>
                    <button class="btn action-btn btn-delete" onclick="adminApp.deleteDoctor(${doctor.id})">Xóa</button>
                </div>
            </td>
        </tr>
    `).join('');
    }


    async loadPatients() {
        try {
            console.log('Loading patients...');
            this.showTableLoading('patientsTableBody');


            try {

                const response = await this.apiRequest('/admin/users/patients');

                if (response?.success) {
                    this.originalPatients = response.data;
                    console.log(' Loaded patients from API:', this.originalPatients);
                } else {
                    throw new Error('API response not successful');
                }
            } catch (apiError) {
                console.warn('⚠ API failed, using mock data:', apiError);

            }

            this.initializePatientFilters();
            this.filterPatients();

        } catch (error) {
            console.error(' Failed to load patients:', error);
            this.showTableError('patientsTableBody', 'Không thể tải danh sách bệnh nhân');
        }
    }

    initializePatientFilters() {
        console.log(' Initializing patient filters...');

        const genderFilter = document.getElementById('genderFilter');
        const ageRangeFilter = document.getElementById('ageRangeFilter');
        const statusFilter = document.getElementById('patientStatusFilter');
        const searchInput = document.getElementById('patientSearch');


        if (genderFilter) {
            genderFilter.removeEventListener('change', this.patientGenderHandler);
            this.patientGenderHandler = () => {
                console.log('👥 Gender changed to:', genderFilter.value);
                this.filterPatients();
            };
            genderFilter.addEventListener('change', this.patientGenderHandler);
        }

        if (ageRangeFilter) {
            ageRangeFilter.removeEventListener('change', this.patientAgeHandler);
            this.patientAgeHandler = () => {
                console.log(' Age range changed to:', ageRangeFilter.value);
                this.filterPatients();
            };
            ageRangeFilter.addEventListener('change', this.patientAgeHandler);
        }

        if (statusFilter) {
            statusFilter.removeEventListener('change', this.patientStatusHandler);
            this.patientStatusHandler = () => {
                console.log(' Status changed to:', statusFilter.value);
                this.filterPatients();
            };
            statusFilter.addEventListener('change', this.patientStatusHandler);
        }

        if (searchInput) {
            searchInput.removeEventListener('input', this.patientSearchHandler);
            this.patientSearchHandler = this.debounce(() => {
                console.log(' Search changed to:', searchInput.value);
                this.filterPatients();
            }, 300);
            searchInput.addEventListener('input', this.patientSearchHandler);
        }

        const quickFiltersContainer = document.querySelector('#patients-content .quick-filters');
        if (quickFiltersContainer) {
            // Xóa event listener cũ
            quickFiltersContainer.removeEventListener('click', this.patientQuickFilterHandler);

            // Thêm event listener mới
            this.patientQuickFilterHandler = (e) => {
                if (e.target.classList.contains('patient-quick-filter')) {
                    e.preventDefault();
                    const filterType = e.target.getAttribute('data-filter');
                    console.log(' Patient quick filter clicked:', filterType);
                    this.setPatientQuickFilter(filterType);
                }
            };
            quickFiltersContainer.addEventListener('click', this.patientQuickFilterHandler);
        }
    }




    applyPatientFilters(patients) {
        const genderFilter = document.getElementById('genderFilter')?.value;
        const ageRangeFilter = document.getElementById('ageRangeFilter')?.value;
        const statusFilter = document.getElementById('patientStatusFilter')?.value;
        const searchTerm = document.getElementById('patientSearch')?.value?.toLowerCase();


        const filtered = patients.filter(patient => {

            if (genderFilter && genderFilter !== '') {
                if (!patient.gender || patient.gender !== genderFilter) {
                    return false;
                }
            }
            if (statusFilter && statusFilter !== '') {
                if (!patient.status || patient.status !== statusFilter) {
                    return false;
                }
            }

            if (ageRangeFilter && ageRangeFilter !== '') {
                const age = this.calculateAge(patient.dateOfBirth);
                let ageMatch = false;

                switch (ageRangeFilter) {
                    case '0-18':
                        ageMatch = age >= 0 && age <= 18;
                        break;
                    case '19-35':
                        ageMatch = age >= 19 && age <= 35;
                        break;
                    case '36-60':
                        ageMatch = age >= 36 && age <= 60;
                        break;
                    case '60+':
                        ageMatch = age >= 60;
                        break;
                }

                if (!ageMatch) {
                    return false;
                }
            }

            // Search filter
            if (searchTerm && searchTerm !== '') {
                const searchableText = `${patient.fullName} ${patient.phone} ${patient.address || ''}`.toLowerCase();
                if (!searchableText.includes(searchTerm)) {
                    return false;
                }
            }

            return true;
        });
        return filtered;
    }

    getFrontendPaginationConfig() {
        return {
            pageSize: 10,
            currentPage: 0
        };
    }

    displayDataWithFrontendPagination(data, displayFunction, paginationContainerId) {
        const config = this.getFrontendPaginationConfig();
        const totalPages = Math.ceil(data.length / config.pageSize);

        // Get current page data
        const startIndex = config.currentPage * config.pageSize;
        const endIndex = startIndex + config.pageSize;
        const pageData = data.slice(startIndex, endIndex);

        // Display data
        displayFunction(pageData);

        // Update pagination if needed
        if (totalPages > 1) {
            this.updateFrontendPagination(paginationContainerId, {
                currentPage: config.currentPage,
                totalPages: totalPages,
                totalElements: data.length,
                pageSize: config.pageSize
            });
        } else {
            this.hideFrontendPagination(paginationContainerId);
        }
    }

// Users with frontend pagination
    displayUsersWithFrontendPagination(users) {
        this.currentUsersData = users; // Store for pagination
        this.displayDataWithFrontendPagination(
            users,
            (pageData) => this.displayUsers(pageData),
            'usersPagination'
        );
    }

// Doctors with frontend pagination
    displayDoctorsWithFrontendPagination(doctors) {
        this.currentDoctorsData = doctors;
        this.displayDataWithFrontendPagination(
            doctors,
            (pageData) => this.displayDoctors(pageData),
            'doctorsPagination'
        );
    }

// Patients with frontend pagination
    displayPatientsWithFrontendPagination(patients) {
        this.currentPatientsData = patients;
        this.displayDataWithFrontendPagination(
            patients,
            (pageData) => this.displayPatients(pageData),
            'patientsPagination'
        );
    }

// Frontend pagination controls
    updateFrontendPagination(containerId, pageInfo) {
        let paginationContainer = document.getElementById(containerId);

        if (!paginationContainer) {
            // Tạo pagination container
            paginationContainer = document.createElement('div');
            paginationContainer.id = containerId;
            paginationContainer.className = 'pagination-container';

            // Thêm vào cuối table container
            const tableContainer = paginationContainer.id.includes('users') ?
                document.querySelector('#users-content .table-container') :
                paginationContainer.id.includes('doctors') ?
                    document.querySelector('#doctors-content .table-container') :
                    document.querySelector('#patients-content .table-container');

            if (tableContainer) {
                tableContainer.appendChild(paginationContainer);
            }
        }

        paginationContainer.style.display = 'flex';
        paginationContainer.innerHTML = '';

        const { currentPage, totalPages, totalElements, pageSize } = pageInfo;

        // Pagination info
        const paginationInfo = document.createElement('div');
        paginationInfo.className = 'pagination-info';
        paginationInfo.textContent = `Hiển thị ${currentPage * pageSize + 1} - ${Math.min((currentPage + 1) * pageSize, totalElements)} trong ${totalElements} kết quả`;

        // Pagination controls
        const paginationControls = document.createElement('div');
        paginationControls.className = 'pagination-controls';

        // Previous button
        const prevBtn = document.createElement('button');
        prevBtn.className = `btn btn-outline ${currentPage === 0 ? 'disabled' : ''}`;
        prevBtn.innerHTML = 'Trước';
        prevBtn.disabled = currentPage === 0;
        if (!prevBtn.disabled) {
            prevBtn.addEventListener('click', () => this.goToFrontendPage(containerId, currentPage - 1));
        }
        paginationControls.appendChild(prevBtn);

        // Page numbers
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            const pageBtn = document.createElement('button');
            pageBtn.className = `btn ${i === currentPage ? 'btn-primary' : 'btn-outline'}`;
            pageBtn.textContent = i + 1;
            if (i !== currentPage) {
                pageBtn.addEventListener('click', () => this.goToFrontendPage(containerId, i));
            }
            paginationControls.appendChild(pageBtn);
        }

        // Next button
        const nextBtn = document.createElement('button');
        nextBtn.className = `btn btn-outline ${currentPage === totalPages - 1 ? 'disabled' : ''}`;
        nextBtn.innerHTML = 'Sau';
        nextBtn.disabled = currentPage === totalPages - 1;
        if (!nextBtn.disabled) {
            nextBtn.addEventListener('click', () => this.goToFrontendPage(containerId, currentPage + 1));
        }
        paginationControls.appendChild(nextBtn);

        paginationContainer.appendChild(paginationInfo);
        paginationContainer.appendChild(paginationControls);
    }



// Navigate to specific page
    goToFrontendPage(containerId, pageNumber) {
        const config = this.getFrontendPaginationConfig();
        config.currentPage = pageNumber;

        // Redisplay current data with new page
        if (containerId.includes('users') && this.currentUsersData) {
            this.displayUsersWithFrontendPagination(this.currentUsersData);
        } else if (containerId.includes('doctors') && this.currentDoctorsData) {
            this.displayDoctorsWithFrontendPagination(this.currentDoctorsData);
        } else if (containerId.includes('patients') && this.currentPatientsData) {
            this.displayPatientsWithFrontendPagination(this.currentPatientsData);
        }
    }

// Hide pagination
    hideFrontendPagination(containerId) {
        const paginationContainer = document.getElementById(containerId);
        if (paginationContainer) {
            paginationContainer.style.display = 'none';
        }
    }


    displayPatients(patients) {
        const tbody = document.getElementById('patientsTableBody');
        if (!tbody) return;

        if (!patients || patients.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">Không có dữ liệu</td></tr>';
            return;
        }

        tbody.innerHTML = patients.map(patient => `
        <tr>
            <td>${patient.id}</td>
            <td>${patient.fullName}</td>
            <td>${this.formatDate(patient.dateOfBirth)}</td>
            <td>${this.getGenderText(patient.gender)}</td>
            <td>${patient.phone}</td>
            <td>${patient.address || 'N/A'}</td>
            <td><span class="status-badge status-${this.getStatusClass(patient.status)}">${this.getStatusText(patient.status)}</span></td>
            <td>
                <div class="action-buttons">
                    <button class="btn action-btn btn-view" onclick="adminApp.viewPatient(${patient.id})">Xem</button>
                    <button class="btn action-btn btn-edit" onclick="adminApp.editPatient(${patient.id})">Sửa</button>
                    <button class="btn action-btn btn-delete" onclick="adminApp.deletePatient(${patient.id})">Xóa</button>
                </div>
            </td>
        </tr>
    `).join('');
    }

    async loadPayments() {
        try {
            console.log(' Loading payments module...');
            this.showTableLoading('paymentsTableBody');

            // Load payment dashboard stats TRƯỚC
            try {
                const paymentDashboard = await this.apiRequest('/admin/payments/dashboard');
                if (paymentDashboard?.success) {
                    this.updatePaymentStats(paymentDashboard.data);
                    console.log(' Payment dashboard loaded');
                }
            } catch (error) {
                console.error(' Failed to load payment dashboard:', error);
            }

            // Khởi tạo filter system SAU khi dashboard load xong
            setTimeout(() => {
                this.initializePaymentFilters();
            }, 100);

            // Load payments list với filters
            await this.loadPaymentsList();

        } catch (error) {
            console.error(' Failed to load payments module:', error);
            this.showTableError('paymentsTableBody', 'Không thể tải trang thanh toán');
        }
    }

// Khởi tạo hệ thống filter
    initializePaymentFilters() {
        console.log(' Initializing payment filter system...');

        // Kiểm tra DOM elements có sẵn chưa
        const requiredElements = [
            'paymentFromDate',
            'paymentToDate',
            'paymentStatusFilter'
        ];

        const missingElements = requiredElements.filter(id => !document.getElementById(id));
        if (missingElements.length > 0) {
            console.error('Missing filter elements:', missingElements);
            // Retry after 500ms
            setTimeout(() => this.initializePaymentFilters(), 500);
            return;
        }
        this.loadFilterFromUrl();
        this.bindPaymentFilterEvents();
        this.addQuickFilters();

        console.log('Payment filter system initialized successfully');
    }


    setDefaultDateInputs() {
        const fromDateInput = document.getElementById('paymentFromDate');
        const toDateInput = document.getElementById('paymentToDate');

        if (fromDateInput && !fromDateInput.value) {
            // Mặc định 30 ngày trước
            const defaultFrom = new Date();
            defaultFrom.setDate(defaultFrom.getDate() - 30);
            fromDateInput.value = defaultFrom.toISOString().split('T')[0];
        }

        if (toDateInput && !toDateInput.value) {
            // Mặc định hôm nay
            const today = new Date();
            toDateInput.value = today.toISOString().split('T')[0];
        }
    }

    loadFilterFromUrl() {
        const params = new URLSearchParams(window.location.search);

        const fromDate = params.get('fromDate');
        const toDate = params.get('toDate');
        const status = params.get('status');

        console.log('🔗 Loading filters from URL:', { fromDate, toDate, status });

        if (fromDate) {
            const fromInput = document.getElementById('paymentFromDate');
            if (fromInput) fromInput.value = fromDate;
        }

        if (toDate) {
            const toInput = document.getElementById('paymentToDate');
            if (toInput) toInput.value = toDate;
        }

        if (status) {
            const statusSelect = document.getElementById('paymentStatusFilter');
            if (statusSelect) statusSelect.value = status;
        }
    }

// Cập nhật URL parameters
    updateFilterUrlParams() {
        const params = new URLSearchParams();

        const fromDate = document.getElementById('paymentFromDate')?.value;
        const toDate = document.getElementById('paymentToDate')?.value;
        const status = document.getElementById('paymentStatusFilter')?.value;

        if (fromDate) params.set('fromDate', fromDate);
        if (toDate) params.set('toDate', toDate);
        if (status && status !== '') params.set('status', status);

        const newUrl = window.location.pathname + (params.toString() ? '?' + params.toString() : '');
        window.history.replaceState({}, '', newUrl);

        console.log('🔗 URL updated with filters');
    }


    addQuickFilters() {
        const filterContainer = document.querySelector('#payments-content .table-filters');
        if (!filterContainer) {
            console.error('Filter container not found');
            return;
        }

        // Xóa quick filters cũ nếu có
        const existingQuickFilters = filterContainer.querySelector('.quick-filters');
        if (existingQuickFilters) {
            existingQuickFilters.remove();
        }

        const quickFiltersHtml = `
        <div class="quick-filters">
            <label>Lọc nhanh:</label>
            <button type="button" class="btn btn-outline btn-sm quick-filter-btn" data-filter="today">
                <i class="fas fa-calendar-day"></i> Hôm nay
            </button>
            <button type="button" class="btn btn-outline btn-sm quick-filter-btn" data-filter="week">
                <i class="fas fa-calendar-week"></i> Tuần này
            </button>
            <button type="button" class="btn btn-outline btn-sm quick-filter-btn" data-filter="month">
                <i class="fas fa-calendar-alt"></i> Tháng này
            </button>
            <button type="button" class="btn btn-outline btn-sm quick-filter-btn" data-filter="success">
                <i class="fas fa-check-circle"></i> Thành công
            </button>
            <button type="button" class="btn btn-outline btn-sm quick-filter-btn" data-filter="pending">
                <i class="fas fa-clock"></i> Chờ xử lý
            </button>
            <button type="button" class="btn btn-outline btn-sm quick-filter-btn" data-filter="reset">
                <i class="fas fa-times"></i> Reset
            </button>
        </div>
    `;

        filterContainer.insertAdjacentHTML('beforeend', quickFiltersHtml);

        // ⚡ FIX: Bind events cho quick filter buttons
        const quickFilterButtons = filterContainer.querySelectorAll('.quick-filter-btn');
        quickFilterButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                const filterType = button.getAttribute('data-filter');
                console.log('Quick filter clicked:', filterType);

                if (filterType === 'reset') {
                    this.resetPaymentFilters();
                } else {
                    this.setQuickFilter(filterType);
                }
            });
        });

        console.log('Quick filters added with event listeners');
    }



    setQuickFilter(type) {
        const today = new Date();
        const formatDate = (date) => date.toISOString().split('T')[0];

        console.log('Setting quick filter:', type);

        const fromDateInput = document.getElementById('paymentFromDate');
        const toDateInput = document.getElementById('paymentToDate');
        const statusInput = document.getElementById('paymentStatusFilter');

        // Kiểm tra elements tồn tại
        if (!fromDateInput || !toDateInput || !statusInput) {
            console.error(' Filter inputs not found');
            return;
        }

        // Reset tất cả trước
        fromDateInput.value = '';
        toDateInput.value = '';
        statusInput.value = '';

        switch (type) {
            case 'today':
                fromDateInput.value = formatDate(today);
                toDateInput.value = formatDate(today);
                console.log(' Set filter: Today');
                break;

            case 'week':
                const startOfWeek = new Date(today);
                startOfWeek.setDate(today.getDate() - today.getDay() + 1); // Thứ 2
                fromDateInput.value = formatDate(startOfWeek);
                toDateInput.value = formatDate(today);
                console.log(' Set filter: This week');
                break;

            case 'month':
                const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
                fromDateInput.value = formatDate(startOfMonth);
                toDateInput.value = formatDate(today);
                console.log(' Set filter: This month');
                break;

            case 'success':
                statusInput.value = 'SUCCESS';
                console.log(' Set filter: Success status');
                break;

            case 'pending':
                statusInput.value = 'PENDING';
                console.log(' Set filter: Pending status');
                break;

            default:
                console.warn(' Unknown filter type:', type);
                return;
        }

        this.filterPayments();
    }



    async loadPaymentsList(page = 0, size = 10) {
        try {
            console.log(' Loading payments list...', { page, size });

            // Lấy giá trị filter
            const fromDate = document.getElementById('paymentFromDate')?.value;
            const toDate = document.getElementById('paymentToDate')?.value;
            const statusFilter = document.getElementById('paymentStatusFilter')?.value;

            console.log(' Applied filters:', { fromDate, toDate, statusFilter });

            let apiUrl;
            let params = new URLSearchParams();
            let isDateRangeFilter = false;

            // CASE 1: Filter theo khoảng thời gian (sử dụng API)
            if (fromDate && toDate) {
                apiUrl = '/admin/payments/by-date-range';
                params.append('startDate', fromDate);
                params.append('endDate', toDate);
                isDateRangeFilter = true;
                console.log(' Using date range API');
            }
            // CASE 2: Lấy tất cả với phân trang (sử dụng API)
            else {
                apiUrl = '/admin/payments/all';
                params.append('page', page.toString());
                params.append('size', size.toString());
                console.log('Using paginated API');
            }

            const fullUrl = `${apiUrl}?${params.toString()}`;
            console.log(' Making request to:', fullUrl);

            const response = await this.apiRequest(fullUrl);
            console.log('API Response:', response);

            if (response?.success) {
                let payments = response.data;
                let paginationData = null;

                // Xử lý response theo loại API
                if (response.data && response.data.content) {
                    // Phân trang response
                    payments = response.data.content;
                    paginationData = response.data;
                    console.log(' Paginated payments received:', payments.length);
                } else if (Array.isArray(response.data)) {
                    // Date range response (array trực tiếp)
                    payments = response.data;
                    console.log(' Date range payments received:', payments.length);
                }

                // Store original data để filter frontend
                this.originalPayments = payments;

                // CASE 3: Áp dụng filter trạng thái ở frontend
                const filteredPayments = this.applyStatusFilter(payments, statusFilter);
                console.log('After status filter:', filteredPayments.length);

                // Hiển thị kết quả
                this.displayPayments(filteredPayments);

                // ⚡ FIX: Chỉ ẩn pagination khi có date range filter
                if (isDateRangeFilter) {
                    this.hidePagination();
                    console.log(' Pagination hidden for date range filter');
                } else if (paginationData) {
                    this.updatePagination(paginationData);
                    console.log(' Pagination updated for regular list');
                }

                // Hiển thị summary
                this.updateFilterSummary(filteredPayments, {
                    fromDate,
                    toDate,
                    statusFilter,
                    originalCount: payments.length
                });

            } else {
                console.error(' API response not successful:', response);
                throw new Error(`API response not successful: ${response?.message || 'Unknown error'}`);
            }

        } catch (error) {
            console.error(' Failed to load payments:', error);
            this.showFilterError(error.message);
        }
    }

    showFilterError(message) {
        const tbody = document.getElementById('paymentsTableBody');
        if (tbody) {
            tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 2rem;">
                    <div class="error-state">
                        <i class="fas fa-exclamation-triangle" style="font-size: 2rem; color: #dc3545; margin-bottom: 1rem;"></i>
                        <h4 style="color: #dc3545; margin-bottom: 0.5rem;">Lỗi tải dữ liệu</h4>
                        <p style="color: #6c757d; margin-bottom: 1.5rem;">${message}</p>
                        <div>
                            <button onclick="adminApp.resetPaymentFilters()" class="btn btn-primary" style="margin-right: 0.5rem;">
                                <i class="fas fa-redo"></i> Reset filter
                            </button>
                            <button onclick="adminApp.loadPaymentsList()" class="btn btn-secondary">
                                <i class="fas fa-reload"></i> Thử lại
                            </button>
                        </div>
                    </div>
                </td>
            </tr>
        `;
        }
    }


    hidePagination() {
        const paginationContainer = document.getElementById('paymentsPagination');
        if (paginationContainer) {
            paginationContainer.style.display = 'none';
        }
    }

    applyStatusFilter(payments, statusFilter) {
        if (!statusFilter || statusFilter === '' || statusFilter === 'all') {
            return payments;
        }

        console.log(' Applying status filter:', statusFilter);

        const statusMap = {
            'SUCCESS': ['COMPLETED', 'SUCCESS'],
            'COMPLETED': ['COMPLETED', 'SUCCESS'],
            'PENDING': ['PENDING'],
            'FAILED': ['FAILED'],
            'CANCELLED': ['CANCELLED'],
            'REFUNDED': ['REFUNDED']
        };

        const allowedStatuses = statusMap[statusFilter.toUpperCase()] || [statusFilter.toUpperCase()];

        const filtered = payments.filter(payment => {
            const paymentStatus = payment.status?.toUpperCase();
            return allowedStatuses.includes(paymentStatus);
        });

        console.log(` Status filter '${statusFilter}' applied: ${payments.length} → ${filtered.length}`);
        return filtered;
    }

    filterByStatusOnly() {
        if (!this.originalPayments) {
            console.warn('No original data to filter');
            return;
        }

        const statusFilter = document.getElementById('paymentStatusFilter')?.value;
        console.log(' Filtering by status only:', statusFilter);

        const filteredPayments = this.applyStatusFilter(this.originalPayments, statusFilter);
        this.displayPayments(filteredPayments);

        // Cập nhật summary
        const fromDate = document.getElementById('paymentFromDate')?.value;
        const toDate = document.getElementById('paymentToDate')?.value;

        this.updateFilterSummary(filteredPayments, {
            fromDate,
            toDate,
            statusFilter,
            originalCount: this.originalPayments.length
        });
    }

    updateFilterSummary(filteredPayments, filters) {
        const summaryContainer = document.getElementById('filterSummary') || this.createFilterSummary();

        const { fromDate, toDate, statusFilter, originalCount } = filters;

        let summaryText = `Hiển thị ${filteredPayments.length}`;
        if (originalCount && originalCount !== filteredPayments.length) {
            summaryText += ` / ${originalCount}`;
        }
        summaryText += ` kết quả`;

        const activeFilters = [];

        // Date range filter
        if (fromDate && toDate) {
            activeFilters.push(`${this.formatDate(fromDate)} → ${this.formatDate(toDate)}`);
        } else if (fromDate) {
            activeFilters.push(`từ ${this.formatDate(fromDate)}`);
        } else if (toDate) {
            activeFilters.push(`đến ${this.formatDate(toDate)}`);
        }

        // Status filter
        if (statusFilter && statusFilter !== '') {
            const statusText = this.getPaymentStatusText(statusFilter);
            activeFilters.push(`🏷️ ${statusText}`);
        }

        // Tính toán thống kê nhanh
        const stats = this.calculateQuickStats(filteredPayments);

        summaryContainer.innerHTML = `
        <div class="filter-summary">
            <div class="filter-info">
                <span class="filter-count">
                    <i class="fas fa-list"></i>
                    ${summaryText}
                </span>
                ${activeFilters.length > 0 ? `
                    <span class="filter-conditions">
                        ${activeFilters.join(' • ')}
                    </span>
                ` : ''}
            </div>
            
            ${filteredPayments.length > 0 ? `
                <div class="filter-stats">
                    <span class="stat-item">
                        <i class="fas fa-check-circle text-success"></i>
                        ${stats.completed} thành công
                    </span>
                    <span class="stat-item">
                        <i class="fas fa-clock text-warning"></i>
                        ${stats.pending} chờ xử lý
                    </span>
                    <span class="stat-item">
                        <i class="fas fa-money-bill text-info"></i>
                        ${this.formatCurrency(stats.totalRevenue)}
                    </span>
                </div>
            ` : ''}
            
            <div class="filter-actions">
                ${activeFilters.length > 0 ? `
                    <button onclick="adminApp.resetPaymentFilters()" class="btn btn-outline btn-sm">
                        <i class="fas fa-times"></i> Xóa filter
                    </button>
                ` : ''}
                <button onclick="adminApp.exportFilteredPayments()" class="btn btn-primary btn-sm">
                    <i class="fas fa-download"></i> Xuất Excel
                </button>
            </div>
        </div>
    `;
    }

    resetPaymentFilters() {
        console.log('Resetting all filters...');

        const fromDateInput = document.getElementById('paymentFromDate');
        const toDateInput = document.getElementById('paymentToDate');
        const statusInput = document.getElementById('paymentStatusFilter');

        if (fromDateInput) fromDateInput.value = '';
        if (toDateInput) toDateInput.value = '';
        if (statusInput) statusInput.value = '';

        // Clear URL params
        window.history.replaceState({}, '', window.location.pathname);

        // Reload data từ đầu
        this.loadPaymentsList(0, 10);

        console.log(' All payment filters reset');
    }

    async exportFilteredPayments() {
        try {
            this.showFilterLoading();

            const fromDate = document.getElementById('paymentFromDate')?.value;
            const toDate = document.getElementById('paymentToDate')?.value;
            const statusFilter = document.getElementById('paymentStatusFilter')?.value;

            console.log('Exporting payments with filters:', { fromDate, toDate, statusFilter });

            // Tạo URL với parameters
            let exportUrl = '/admin/payments/export?format=excel';

            if (fromDate) exportUrl += `&startDate=${fromDate}`;
            if (toDate) exportUrl += `&endDate=${toDate}`;
            // Note: Status filter sẽ được áp dụng sau khi tải về (nếu cần)

            // Tạo request để download file
            const response = await fetch(this.apiBaseUrl + exportUrl, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // Tạo file download
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;

            // Tạo tên file với thông tin filter
            let fileName = 'payments_export';
            if (fromDate && toDate) {
                fileName += `_${fromDate}_to_${toDate}`;
            }
            if (statusFilter) {
                fileName += `_${statusFilter}`;
            }
            fileName += `.xlsx`;

            a.download = fileName;
            document.body.appendChild(a);
            a.click();

            // Cleanup
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);

            this.showSuccess(`File Excel đã được tải về: ${fileName}`);

        } catch (error) {
            console.error(' Export error:', error);
            this.showError('Có lỗi khi xuất file Excel: ' + error.message);
        } finally {
            this.hideFilterLoading();
        }
    }

    openAdvancedFilter() {
        const advancedFilterModal = `
        <div class="advanced-filter-modal">
            <h4>Bộ lọc nâng cao</h4>
            <div class="advanced-filter-content">
                <div class="filter-section">
                    <h5>Khoảng thời gian</h5>
                    <div class="date-range-picker">
                        <input type="date" id="advFromDate" value="${document.getElementById('paymentFromDate')?.value || ''}">
                        <span>đến</span>
                        <input type="date" id="advToDate" value="${document.getElementById('paymentToDate')?.value || ''}">
                    </div>
                </div>
                
                <div class="filter-section">
                    <h5>Trạng thái</h5>
                    <div class="status-checkboxes">
                        ${this.generateStatusCheckboxes()}
                    </div>
                </div>
                
                <div class="filter-section">
                    <h5>Khoảng tiền</h5>
                    <div class="amount-range">
                        <input type="number" id="minAmount" placeholder="Từ (VND)" min="0">
                        <span>đến</span>
                        <input type="number" id="maxAmount" placeholder="Đến (VND)" min="0">
                    </div>
                </div>
                
                <div class="filter-section">
                    <h5>Phương thức thanh toán</h5>
                    <div class="method-checkboxes">
                        <label><input type="checkbox" value="VNPAY" checked> VNPay</label>
                        <label><input type="checkbox" value="MOMO" checked> MoMo</label>
                        <label><input type="checkbox" value="CASH" checked> Tiền mặt</label>
                        <label><input type="checkbox" value="CREDIT_CARD" checked> Thẻ tín dụng</label>
                    </div>
                </div>
            </div>
        </div>
    `;

        this.showModal('Bộ lọc nâng cao', advancedFilterModal, `
        <button class="btn btn-secondary" onclick="adminApp.closeModal()">Hủy</button>
        <button class="btn btn-primary" onclick="adminApp.applyAdvancedFilter()">Áp dụng</button>
    `);
    }

    generateStatusCheckboxes() {
        const statuses = [
            { value: 'SUCCESS', label: 'Thành công', checked: true },
            { value: 'PENDING', label: 'Chờ xử lý', checked: true },
            { value: 'FAILED', label: 'Thất bại', checked: true },
            { value: 'CANCELLED', label: 'Đã hủy', checked: false },
            { value: 'REFUNDED', label: 'Đã hoàn tiền', checked: false }
        ];

        return statuses.map(status => `
        <label>
            <input type="checkbox" value="${status.value}" ${status.checked ? 'checked' : ''}> 
            ${status.label}
        </label>
    `).join('');
    }

// Apply advanced filter
    applyAdvancedFilter() {
        const fromDate = document.getElementById('advFromDate')?.value;
        const toDate = document.getElementById('advToDate')?.value;

        // Update main filter inputs
        if (fromDate) document.getElementById('paymentFromDate').value = fromDate;
        if (toDate) document.getElementById('paymentToDate').value = toDate;

        // Apply filter
        this.filterPayments();
        this.closeModal();

        console.log(' Advanced filter applied');
    }

// Statistics for filtered data
    displayFilterStatistics(payments) {
        if (!payments || payments.length === 0) return;

        const stats = {
            total: payments.length,
            completed: payments.filter(p => ['COMPLETED', 'SUCCESS'].includes(p.status?.toUpperCase())).length,
            pending: payments.filter(p => p.status?.toUpperCase() === 'PENDING').length,
            failed: payments.filter(p => p.status?.toUpperCase() === 'FAILED').length,
            totalAmount: payments.reduce((sum, p) => sum + (p.amount || 0), 0),
            successRate: 0
        };

        stats.successRate = stats.total > 0 ? (stats.completed / stats.total * 100).toFixed(1) : 0;

        console.log(' Filter statistics:', stats);
        return stats;
    }

// Cleanup function
    cleanupPaymentFilters() {
        console.log('🧹 Cleaning up payment filters...');

        // Remove stored data
        this.originalPayments = null;

        // Clear URL params
        const url = new URL(window.location);
        url.search = '';
        window.history.replaceState({}, '', url);

        console.log('✅ Payment filters cleaned up');
    }

    calculateQuickStats(payments) {
        const completed = payments.filter(p =>
            ['COMPLETED', 'SUCCESS'].includes(p.status?.toUpperCase())
        ).length;

        const pending = payments.filter(p =>
            p.status?.toUpperCase() === 'PENDING'
        ).length;

        const totalRevenue = payments
            .filter(p => ['COMPLETED', 'SUCCESS'].includes(p.status?.toUpperCase()))
            .reduce((sum, p) => sum + (p.amount || 0), 0);

        return { completed, pending, totalRevenue };
    }

// Hàm filter theo ngày (reload API)
    async filterByDateRange() {
        console.log(' Filtering by date range - reloading from API');
        await this.loadPaymentsList(0, 10); // Reset về trang đầu
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

        console.log('📄 Updating pagination:', { currentPage, totalPages, totalElements });


        if (totalPages <= 1 && totalElements <= pageSize) {
            paginationContainer.style.display = 'none';
            return;
        }

        paginationContainer.style.display = 'flex';

        // Xóa nội dung cũ
        paginationContainer.innerHTML = '';

        // Tạo phần hiển thị thông tin trang
        const paginationInfo = document.createElement('div');
        paginationInfo.className = 'pagination-info';
        paginationInfo.textContent = `Hiển thị ${currentPage * pageSize + 1} - ${Math.min((currentPage + 1) * pageSize, totalElements)} trong ${totalElements} kết quả`;

        const paginationControls = document.createElement('div');
        paginationControls.className = 'pagination-controls';

        // Nút Previous
        const prevBtn = document.createElement('button');
        prevBtn.className = `btn btn-outline ${currentPage === 0 ? 'disabled' : ''}`;
        prevBtn.innerHTML = '<i class="fas fa-chevron-left"></i>';
        prevBtn.disabled = currentPage === 0;
        if (!prevBtn.disabled) {
            prevBtn.addEventListener('click', async () => {
                await this.loadPaymentsList(currentPage - 1);
            });
        }
        paginationControls.appendChild(prevBtn);

        // Page numbers
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            const pageBtn = document.createElement('button');
            pageBtn.className = `btn ${i === currentPage ? 'btn-primary' : 'btn-outline'}`;
            pageBtn.textContent = i + 1;
            if (i !== currentPage) {
                pageBtn.addEventListener('click', async () => {
                    await this.loadPaymentsList(i);
                });
            }
            paginationControls.appendChild(pageBtn);
        }

        // Nút Next
        const nextBtn = document.createElement('button');
        nextBtn.className = `btn btn-outline ${currentPage === totalPages - 1 ? 'disabled' : ''}`;
        nextBtn.innerHTML = '<i class="fas fa-chevron-right"></i>';
        nextBtn.disabled = currentPage === totalPages - 1;
        if (!nextBtn.disabled) {
            nextBtn.addEventListener('click', async () => {
                await this.loadPaymentsList(currentPage + 1);
            });
        }
        paginationControls.appendChild(nextBtn);

        // Gắn vào DOM
        paginationContainer.appendChild(paginationInfo);
        paginationContainer.appendChild(paginationControls);

        console.log(' Pagination updated successfully');
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

        if (!tbody) {
            console.error('Payments table body not found');
            return;
        }

        if (!payments || payments.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 2rem; color: #666;">Không có dữ liệu thanh toán</td></tr>';
            return;
        }

        tbody.innerHTML = payments.map(payment => {
            // Xử lý các trường có thể null/undefined
            const transactionId = payment.transactionId || payment.id || 'N/A';
            const patientName = payment.patientName || payment.patient?.fullName || 'N/A';
            const doctorName = payment.doctorName || payment.doctor?.fullName || 'N/A';
            const amount = payment.amount || 0;
            const status = payment.status || 'UNKNOWN';
            const createdAt = payment.createdAt || payment.paymentDate || new Date().toISOString();

            return `
            <tr>
                <td>${transactionId}</td>
                <td>${patientName}</td>
                <td>${doctorName}</td>
                <td>${this.formatCurrency(amount)}</td>
                <td><span class="status-badge status-${this.getPaymentStatusClass(status)}">${this.getPaymentStatusText(status)}</span></td>
                <td>${this.formatDateTime(createdAt)}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-outline action-btn" onclick="adminApp.viewPaymentDetails('${transactionId}')" title="Xem chi tiết">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn btn-outline action-btn" onclick="adminApp.refreshPaymentStatus('${transactionId}')" title="Làm mới trạng thái">
                            <i class="fas fa-sync"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
        }).join('');

        console.log(`Displayed ${payments.length} payments in table`);
    }

    getPaymentStatusText(status) {
        const statusMap = {
            'COMPLETED': 'Hoàn thành',
            'SUCCESS': 'Thành công',
            'PENDING': 'Đang xử lý',
            'FAILED': 'Thất bại'
        };
        return statusMap[status] || status;
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
                    <select id="specialtyFilter">
                        <option value="">Tất cả</option>
                        <option value="Tim mạch">Tim mạch</option>
                        <option value="Thần Kinh">Thần kinh</option>
                        <option value="Nhi khoa">Nhi khoa</option>
                        <option value="Chỉnh Hình">Chấn thương chỉnh hình</option>
                        <option value="Da liễu">Da liễu</option>
                        <option value="Đa khoa">Đa khoa</option>
                        <option value="Sản phụ khoa">Sản phụ khoa</option>
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

    bindUserFilterEvents() {
        const roleFilter = document.getElementById('roleFilter');
        const statusFilter = document.getElementById('userStatusFilter');
        const searchInput = document.getElementById('userSearch');

        if (roleFilter) {
            roleFilter.addEventListener('change', () => this.filterUsers());
        }

        if (statusFilter) {
            statusFilter.addEventListener('change', () => this.filterUsers());
        }

        if (searchInput) {
            searchInput.addEventListener('input', this.debounce(() => this.filterUsers(), 300));
        }
    }


    applyUserFilters(users) {
        const roleFilter = document.getElementById('roleFilter')?.value;
        const statusFilter = document.getElementById('userStatusFilter')?.value;
        const searchTerm = document.getElementById('userSearch')?.value?.toLowerCase();

        return users.filter(user => {
            if (roleFilter && user.role !== roleFilter) return false;
            if (statusFilter && user.status !== statusFilter) return false;
            if (searchTerm) {
                const searchableText = `${patient.fullName} ${patient.phone} ${patient.address}`.toLowerCase();
                if (!searchableText.includes(searchTerm)) return false;
            }

            return true;
        });
    }

    setPatientQuickFilter(type) {
        console.log('⚡ Setting patient quick filter:', type);

        const genderFilter = document.getElementById('genderFilter');
        const ageRangeFilter = document.getElementById('ageRangeFilter');
        const statusFilter = document.getElementById('patientStatusFilter');
        const searchInput = document.getElementById('patientSearch');

        if (genderFilter) genderFilter.value = '';
        if (ageRangeFilter) ageRangeFilter.value = '';
        if (statusFilter) statusFilter.value = '';
        if (searchInput) searchInput.value = '';

        switch (type) {
            case 'new':
                if (statusFilter) {
                    statusFilter.value = 'NEW';
                }
                break;
            case 'active':
                if (statusFilter) {
                    statusFilter.value = 'ACTIVE';
                }
                break;
            case 'children':
                if (ageRangeFilter) {
                    ageRangeFilter.value = '0-18';
                }
                break;
            case 'elderly':
                if (ageRangeFilter) {
                    ageRangeFilter.value = '60+';
                }
                break;
            case 'reset':
                break;
        }

        // Trigger filter
        this.filterPatients();
    }

    getSpecialtyText(specialty) {
        // Nếu đã là tiếng Việt thì return luôn
        if (typeof specialty === 'string' && specialty.includes(' ')) {
            return specialty;
        }

        // Map từ English sang tiếng Việt (backup)
        const specialtyMap = {
            'CARDIOLOGY': 'Tim mạch',
            'NEUROLOGY': 'Thần Kinh',
            'ORTHOPEDICS': 'Chỉnh Hình',
            'PEDIATRICS': 'Nhi khoa',
            'DERMATOLOGY': 'Da liễu',
            'GENERAL': 'Đa khoa',
            'OBSTETRICS': 'Sản phụ khoa',
            'OPHTHALMOLOGY': 'Mắt',
            'ENT': 'Tai mũi họng'
        };

        return specialtyMap[specialty] || specialty;
    }

    updatePatientFilterSummary(patients) {
        const container = document.getElementById('patientFilterSummary');
        if (!container) return;

        const stats = {
            total: patients.length,
            active: patients.filter(p => p.status === 'ACTIVE').length,
            new: patients.filter(p => p.status === 'NEW').length,
            children: patients.filter(p => this.calculateAge(p.dateOfBirth) < 18).length
        };

        container.innerHTML = `
        <div class="filter-summary">
            <div class="filter-info">
                <span class="filter-count">Hiển thị ${stats.total} bệnh nhân</span>
            </div>
            <div class="filter-stats">
                <span class="stat-item">${stats.active} đang điều trị</span>
                <span class="stat-item">${stats.new} bệnh nhân mới</span>
                <span class="stat-item">${stats.children} trẻ em</span>
            </div>
        </div>
    `;
    }

    setUserQuickFilter(type) {
        const roleFilter = document.getElementById('roleFilter');
        const statusFilter = document.getElementById('userStatusFilter');
        const searchInput = document.getElementById('userSearch');

        // Reset filters
        if (roleFilter) roleFilter.value = '';
        if (statusFilter) statusFilter.value = '';
        if (searchInput) searchInput.value = '';

        switch (type) {
            case 'doctors':
                if (roleFilter) roleFilter.value = 'DOCTOR';
                break;
            case 'patients':
                if (roleFilter) roleFilter.value = 'PATIENT';
                break;
            case 'active':
                if (statusFilter) statusFilter.value = 'ACTIVE';
                break;
            case 'pending':
                if (statusFilter) statusFilter.value = 'PENDING';
                break;
            case 'reset':
                break;
        }

        this.filterUsers();
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

    viewPaymentDetails(paymentId) {
        console.log('Viewing payment details for:', paymentId);
        // TODO: Implement payment details modal
        this.showModal('Chi tiết thanh toán', `
        <div class="loading-spinner">Đang tải chi tiết thanh toán ${paymentId}...</div>
    `);
    }

    // Filter Methods
    filterUsers() {
        if (!this.originalUsers) return;

        const filteredUsers = this.applyUserFilters(this.originalUsers);
        this.displayUsers(filteredUsers);
        this.updateUserFilterSummary(filteredUsers);
    }

    updateUserFilterSummary(users) {
        const container = document.getElementById('userFilterSummary');
        if (!container) return;

        const stats = {
            total: users.length,
            active: users.filter(u => u.status === 'ACTIVE').length,
            doctors: users.filter(u => u.role === 'DOCTOR').length,
            patients: users.filter(u => u.role === 'PATIENT').length
        };

        container.innerHTML = `
        <div class="filter-summary">
            <div class="filter-info">
                <span class="filter-count">Hiển thị ${stats.total} người dùng</span>
            </div>
            <div class="filter-stats">
                <span class="stat-item">${stats.active} đang hoạt động</span>
                <span class="stat-item">${stats.doctors} bác sĩ</span>
                <span class="stat-item">${stats.patients} bệnh nhân</span>
            </div>
        </div>
    `;
    }


    filterDoctors() {
        if (!this.originalDoctors) return;

        const filteredDoctors = this.applyDoctorFilters(this.originalDoctors);
        this.displayDoctors(filteredDoctors);
        this.updateDoctorFilterSummary(filteredDoctors);
    }

    updateDoctorFilterSummary(doctors) {
        const container = document.getElementById('doctorFilterSummary');
        if (!container) return;

        const stats = {
            total: doctors.length,
            active: doctors.filter(d => d.status === 'ACTIVE').length,
            avgExperience: doctors.length > 0 ? Math.round(doctors.reduce((sum, d) => sum + (d.experience || 0), 0) / doctors.length) : 0
        };

        container.innerHTML = `
        <div class="filter-summary">
            <div class="filter-info">
                <span class="filter-count">Hiển thị ${stats.total} bác sĩ</span>
            </div>
            <div class="filter-stats">    
                <span class="stat-item"> <i class="fas fa-check-circle text-success"></i>${stats.active} đang hoạt động</span>
                <span class="stat-item"><i class="fas fa-medal text-warning"></i>TB ${stats.avgExperience} năm kinh nghiệm</span>
            </div>
        </div>
    `;
    }

    applyDoctorFilters(doctors) {
        const specialtyFilter = document.getElementById('specialtyFilter')?.value;
        const statusFilter = document.getElementById('doctorStatusFilter')?.value;
        const experienceFilter = document.getElementById('experienceFilter')?.value;
        const searchTerm = document.getElementById('doctorSearch')?.value?.toLowerCase();

        return doctors.filter(doctor => {
            if (specialtyFilter && doctor.specialty !== specialtyFilter) return false;
            if (statusFilter && doctor.status !== statusFilter) return false;

            // Experience filter
            if (experienceFilter) {
                const experience = doctor.experience || 0;
                switch (experienceFilter) {
                    case '0-2':
                        if (experience < 0 || experience > 2) return false;
                        break;
                    case '3-5':
                        if (experience < 3 || experience > 5) return false;
                        break;
                    case '6-10':
                        if (experience < 6 || experience > 10) return false;
                        break;
                    case '10+':
                        if (experience < 11) return false;
                        break;
                }
            }

            if (searchTerm) {
                const searchableText = `${doctor.fullName} ${doctor.specialty}`.toLowerCase();
                if (!searchableText.includes(searchTerm)) return false;
            }

            return true;
        });
    }

    filterPatients() {
        if (!this.originalPatients) {

            return;
        }

        const filteredPatients = this.applyPatientFilters(this.originalPatients);

        this.displayPatients(filteredPatients);
        this.updatePatientFilterSummary(filteredPatients);
    }

    hideFilterLoading() {
        const filterContainer = document.querySelector('.table-filters');
        if (filterContainer) {
            filterContainer.classList.remove('filter-loading');
        }
    }


    validateDateRange() {
        const fromDate = document.getElementById('paymentFromDate')?.value;
        const toDate = document.getElementById('paymentToDate')?.value;

        if (fromDate && toDate) {
            const from = new Date(fromDate);
            const to = new Date(toDate);

            if (from > to) {
                this.showWarning('Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc');
                return false;
            }

            // Kiểm tra khoảng thời gian không quá 1 năm
            const diffTime = Math.abs(to - from);
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

            if (diffDays > 365) {
                this.showWarning('Khoảng thời gian filter không được vượt quá 1 năm để đảm bảo hiệu năng');
                return false;
            }

            // Cảnh báo nếu khoảng thời gian quá xa
            const now = new Date();
            const oneYearAgo = new Date();
            oneYearAgo.setFullYear(now.getFullYear() - 1);

            if (to < oneYearAgo) {
                this.showInfo('Bạn đang xem dữ liệu cũ hơn 1 năm. Có thể cần thời gian tải lâu hơn.');
            }
        }

        return true;
    }

    showWarning(message) {
        console.warn('', message);
        alert(message);
    }

    showInfo(message) {
        console.info('', message);
    }


    async filterPayments() {
        try {
            const fromDate = document.getElementById('paymentFromDate')?.value;
            const toDate = document.getElementById('paymentToDate')?.value;
            const statusFilter = document.getElementById('paymentStatusFilter')?.value;

            console.log(' Filter triggered:', { fromDate, toDate, statusFilter });

            if ((fromDate || toDate) && !this.validateDateRange()) {
                return; // Dừng nếu date range không hợp lệ
            }
            await this.loadPaymentsList(0, 10);
            this.updateFilterUrlParams();

        } catch (error) {
            console.error(' Error filtering payments:', error);
            this.showError('Có lỗi khi lọc dữ liệu thanh toán: ' + error.message);
        }
    }

    bindPaymentFilterEvents() {
        console.log(' Binding payment filter events...');

        const fromDateInput = document.getElementById('paymentFromDate');
        const toDateInput = document.getElementById('paymentToDate');
        const statusSelect = document.getElementById('paymentStatusFilter');

        // Xóa event listeners cũ nếu có
        if (fromDateInput) {
            fromDateInput.removeEventListener('change', this.handleDateChange);
            this.handleDateChange = this.debounce(async () => {
                console.log(' From date changed:', fromDateInput.value);
                if (this.validateDateRange()) {
                    await this.filterPayments();
                }
            }, 300);
            fromDateInput.addEventListener('change', this.handleDateChange);
        }

        if (toDateInput) {
            toDateInput.removeEventListener('change', this.handleToDateChange);
            this.handleToDateChange = this.debounce(async () => {
                console.log(' To date changed:', toDateInput.value);
                if (this.validateDateRange()) {
                    await this.filterPayments();
                }
            }, 300);
            toDateInput.addEventListener('change', this.handleToDateChange);
        }

        if (statusSelect) {
            statusSelect.removeEventListener('change', this.handleStatusChange);
            this.handleStatusChange = () => {
                console.log(' Status filter changed:', statusSelect.value);
                this.filterByStatusOnly();
                this.updateFilterUrlParams();
            };
            statusSelect.addEventListener('change', this.handleStatusChange);
        }

        console.log(' Payment filter events bound successfully');
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
        }).format(amount || 0);
    }

    formatDate(dateString) {
        if (!dateString) return 'N/A';
        try {
            const date = new Date(dateString);
            return new Intl.DateTimeFormat('vi-VN').format(date);
        } catch (error) {
            return dateString;
        }
    }

    showFilterLoading() {
        const filterContainer = document.querySelector('.table-filters');
        if (filterContainer) {
            filterContainer.classList.add('filter-loading');
        }
    }

    formatDateTime(dateString) {
        if (!dateString) return 'N/A';
        try {
            const date = new Date(dateString);
            return new Intl.DateTimeFormat('vi-VN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            }).format(date);
        } catch (error) {
            return dateString;
        }
    }

    getRoleText(role) {
        const roleMap = {
            'ADMIN': 'Admin',
            'DOCTOR': 'Bác sĩ',
            'PATIENT': 'Bệnh nhân'
        };
        return roleMap[role] || role;
    }




    getStatusClass(status) {
        const statusMap = {
            'ACTIVE': 'active',
            'INACTIVE': 'inactive',
            'PENDING': 'pending',
            'NEW': 'new'
        };
        return statusMap[status] || 'inactive';
    }

    getStatusText(status) {
        const statusMap = {
            'ACTIVE': 'Hoạt động',
            'INACTIVE': 'Vô hiệu hóa',
            'PENDING': 'Chờ xác thực',
            'NEW': 'Mới'
        };
        return statusMap[status] || status;
    }




    getRoleClass(role) {
        const roleMap = {
            'ADMIN': 'admin',
            'DOCTOR': 'doctor',
            'PATIENT': 'patient'
        };
        return roleMap[role] || 'patient';
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
        const statusMap = {
            'COMPLETED': 'success',
            'SUCCESS': 'success',
            'PENDING': 'warning',
            'FAILED': 'danger'
        };
        return statusMap[status] || 'secondary';
    }

    calculateAge(dateOfBirth) {
        if (!dateOfBirth) return 0;

        try {
            const currentYear = new Date().getFullYear();

            // Nếu chỉ có năm (string hoặc number)
            if (typeof dateOfBirth === 'string' && dateOfBirth.length === 4) {
                return currentYear - parseInt(dateOfBirth);
            }

            // Nếu có ngày đầy đủ, lấy năm từ đó
            const birthDate = new Date(dateOfBirth);
            if (isNaN(birthDate.getTime())) {
                console.warn('Invalid date:', dateOfBirth);
                return 0;
            }

            const birthYear = birthDate.getFullYear();
            const age = currentYear - birthYear;

            return age >= 0 ? age : 0;
        } catch (error) {
            console.error('Error calculating age:', error);
            return 0;
        }
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
        if (tbody) {
            const colCount = tbody.closest('table')?.querySelector('thead tr')?.children.length || 8;
            tbody.innerHTML = `
            <tr>
                <td colspan="${colCount}" class="loading">
                    <div class="loading-spinner">Đang tải dữ liệu...</div>
                </td>
            </tr>
        `;
        }
    }

    showTableError(tableBodyId, message) {
        const tbody = document.getElementById(tableBodyId);
        if (tbody) {
            const colCount = tbody.closest('table')?.querySelector('thead tr')?.children.length || 8;
            tbody.innerHTML = `
            <tr>
                <td colspan="${colCount}" style="text-align: center; padding: 2rem;">
                    <div class="error-state">
                        <h4 style="color: #dc3545; margin-bottom: 0.5rem;">Lỗi tải dữ liệu</h4>
                        <p style="color: #6c757d; margin-bottom: 1.5rem;">${message}</p>
                        <button onclick="location.reload()" class="btn btn-primary">Thử lại</button>
                    </div>
                </td>
            </tr>
        `;
        }
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
