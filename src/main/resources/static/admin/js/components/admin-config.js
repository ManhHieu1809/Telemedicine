// admin-config.js - Cấu hình cho Admin Panel

const AdminConfig = {
    // API Configuration
    api: {
        baseUrl: 'http://localhost:8080/api',
        timeout: 30000,
        endpoints: {
            // Authentication
            login: '/auth/login',
            register: '/auth/register',
            createDoctor: '/auth/create-doctor',

            // User Management
            profile: '/users/profile',
            allDoctors: '/users/doctors',
            doctorsBySpecialty: '/users/doctors/specialty',
            topDoctors: '/users/top-doctors',

            // Admin Management
            systemReport: '/admin/reports',
            userActivities: '/admin/user-activities',
            allPatients: '/admin/users/patients',
            updateDoctor: '/admin/users/doctors',
            deleteDoctor: '/admin/users/doctors',
            updatePatient: '/admin/users/patients',
            deletePatient: '/admin/users/patients',
            deleteReview: '/admin/reviews',
            deleteChatMessage: '/admin/chat-messages',

            // Payment Management
            paymentDashboard: '/admin/payments/dashboard',
            paymentReports: '/admin/payments/reports',
            monthlyRevenue: '/admin/payments/revenue/monthly',
            topPatients: '/admin/payments/top-patients',
            topDoctors: '/admin/payments/top-doctors',
            dailyStats: '/admin/payments/daily-stats',
            methodStats: '/admin/payments/method-stats',
            exportPayments: '/admin/payments/export'
        }
    },

    // Table Configurations
    tables: {
        users: {
            columns: [
                { key: 'id', title: 'ID', type: 'number' },
                { key: 'username', title: 'Tên đăng nhập', type: 'string' },
                { key: 'email', title: 'Email', type: 'string' },
                {
                    key: 'roles',
                    title: 'Vai trò',
                    type: 'string',
                    render: (value) => `<span class="status-badge status-${AdminConfig.getRoleClass(value)}">${value}</span>`
                },
                {
                    key: 'createdAt',
                    title: 'Ngày tạo',
                    type: 'date',
                    render: (value) => AdminConfig.formatDateTime(value)
                }
            ],
            actions: [
                { icon: 'edit', text: '', handler: 'adminApp.editUser', type: 'outline' },
                { icon: 'trash', text: '', handler: 'adminApp.deleteUser', type: 'danger' }
            ],
            pageSize: 10,
            searchable: true,
            sortable: true
        },

        patients: {
            columns: [
                { key: 'id', title: 'ID', type: 'number' },
                { key: 'fullName', title: 'Họ tên', type: 'string' },
                {
                    key: 'dateOfBirth',
                    title: 'Ngày sinh',
                    type: 'date',
                    render: (value) => AdminConfig.formatDate(value)
                },
                {
                    key: 'gender',
                    title: 'Giới tính',
                    type: 'string',
                    render: (value) => AdminConfig.getGenderText(value)
                },
                { key: 'phone', title: 'Điện thoại', type: 'string' }
            ],
            actions: [
                { icon: 'edit', text: '', handler: 'adminApp.editPatient', type: 'outline' },
                { icon: 'trash', text: '', handler: 'adminApp.deletePatient', type: 'danger' }
            ],
            pageSize: 10,
            searchable: true,
            sortable: true
        },

        payments: {
            columns: [
                { key: 'transactionId', title: 'Mã GD', type: 'string' },
                { key: 'patientName', title: 'Bệnh nhân', type: 'string' },
                { key: 'doctorName', title: 'Bác sĩ', type: 'string' },
                {
                    key: 'amount',
                    title: 'Số tiền',
                    type: 'number',
                    render: (value) => AdminConfig.formatCurrency(value)
                },
                {
                    key: 'status',
                    title: 'Trạng thái',
                    type: 'string',
                    render: (value) => `<span class="status-badge status-${AdminConfig.getPaymentStatusClass(value)}">${value}</span>`
                },
                {
                    key: 'createdAt',
                    title: 'Ngày',
                    type: 'date',
                    render: (value) => AdminConfig.formatDateTime(value)
                }
            ],
            actions: [
                { icon: 'eye', text: '', handler: 'adminApp.viewPaymentDetails', type: 'outline' }
            ],
            pageSize: 15,
            searchable: true,
            sortable: true
        }
    },

    // Form Configurations
    forms: {
        addUser: {
            fields: [
                {
                    name: 'username',
                    label: 'Tên đăng nhập',
                    type: 'text',
                    required: true,
                    placeholder: 'Nhập tên đăng nhập'
                },
                {
                    name: 'email',
                    label: 'Email',
                    type: 'email',
                    required: true,
                    placeholder: 'Nhập địa chỉ email'
                },
                {
                    name: 'password',
                    label: 'Mật khẩu',
                    type: 'password',
                    required: true,
                    placeholder: 'Nhập mật khẩu'
                },
                {
                    name: 'role',
                    label: 'Vai trò',
                    type: 'select',
                    required: true,
                    options: [
                        { value: 'ADMIN', label: 'Quản trị viên' },
                        { value: 'DOCTOR', label: 'Bác sĩ' },
                        { value: 'PATIENT', label: 'Bệnh nhân' }
                    ]
                }
            ]
        },

        addDoctor: {
            fields: [
                {
                    name: 'username',
                    label: 'Tên đăng nhập',
                    type: 'text',
                    required: true,
                    placeholder: 'Nhập tên đăng nhập'
                },
                {
                    name: 'email',
                    label: 'Email',
                    type: 'email',
                    required: true,
                    placeholder: 'Nhập địa chỉ email'
                },
                {
                    name: 'password',
                    label: 'Mật khẩu',
                    type: 'password',
                    required: true,
                    placeholder: 'Nhập mật khẩu'
                },
                {
                    name: 'fullName',
                    label: 'Họ và tên',
                    type: 'text',
                    required: true,
                    placeholder: 'Nhập họ và tên đầy đủ'
                },
                {
                    name: 'specialty',
                    label: 'Chuyên khoa',
                    type: 'select',
                    required: true,
                    options: [
                        { value: 'Nội khoa', label: 'Nội khoa' },
                        { value: 'Ngoại khoa', label: 'Ngoại khoa' },
                        { value: 'Sản phụ khoa', label: 'Sản phụ khoa' },
                        { value: 'Nhi khoa', label: 'Nhi khoa' },
                        { value: 'Tim mạch', label: 'Tim mạch' },
                        { value: 'Da liễu', label: 'Da liễu' },
                        { value: 'Mắt', label: 'Nhãn khoa' },
                        { value: 'Tai mũi họng', label: 'Tai mũi họng' },
                        { value: 'Thần kinh', label: 'Thần kinh' },
                        { value: 'Xương khớp', label: 'Xương khớp' }
                    ]
                },
                {
                    name: 'experience',
                    label: 'Kinh nghiệm (năm)',
                    type: 'number',
                    required: true,
                    min: 0,
                    placeholder: 'Nhập số năm kinh nghiệm'
                },
                {
                    name: 'phone',
                    label: 'Số điện thoại',
                    type: 'tel',
                    required: true,
                    placeholder: 'Nhập số điện thoại'
                },
                {
                    name: 'address',
                    label: 'Địa chỉ',
                    type: 'textarea',
                    rows: 3,
                    placeholder: 'Nhập địa chỉ'
                }
            ]
        },

        addPatient: {
            fields: [
                {
                    name: 'username',
                    label: 'Tên đăng nhập',
                    type: 'text',
                    required: true,
                    placeholder: 'Nhập tên đăng nhập'
                },
                {
                    name: 'email',
                    label: 'Email',
                    type: 'email',
                    required: true,
                    placeholder: 'Nhập địa chỉ email'
                },
                {
                    name: 'password',
                    label: 'Mật khẩu',
                    type: 'password',
                    required: true,
                    placeholder: 'Nhập mật khẩu'
                },
                {
                    name: 'fullName',
                    label: 'Họ và tên',
                    type: 'text',
                    required: true,
                    placeholder: 'Nhập họ và tên đầy đủ'
                },
                {
                    name: 'dateOfBirth',
                    label: 'Ngày sinh',
                    type: 'date',
                    required: true
                },
                {
                    name: 'gender',
                    label: 'Giới tính',
                    type: 'select',
                    required: true,
                    options: [
                        { value: 'MALE', label: 'Nam' },
                        { value: 'FEMALE', label: 'Nữ' },
                        { value: 'OTHER', label: 'Khác' }
                    ]
                },
                {
                    name: 'phone',
                    label: 'Số điện thoại',
                    type: 'tel',
                    required: true,
                    placeholder: 'Nhập số điện thoại'
                },
                {
                    name: 'address',
                    label: 'Địa chỉ',
                    type: 'textarea',
                    rows: 3,
                    placeholder: 'Nhập địa chỉ'
                }
            ]
        }
    },

    // Chart Configurations
    charts: {
        userActivity: {
            type: 'line',
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        },

        revenue: {
            type: 'bar',
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return AdminConfig.formatCurrency(value);
                            }
                        }
                    }
                }
            }
        },

        paymentMethods: {
            type: 'pie',
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        }
    },

    // Filter Options
    filters: {
        roles: [
            { value: '', label: 'Tất cả vai trò' },
            { value: 'ADMIN', label: 'Quản trị viên' },
            { value: 'DOCTOR', label: 'Bác sĩ' },
            { value: 'PATIENT', label: 'Bệnh nhân' }
        ],

        specialties: [
            { value: '', label: 'Tất cả chuyên khoa' },
            { value: 'Nội khoa', label: 'Nội khoa' },
            { value: 'Ngoại khoa', label: 'Ngoại khoa' },
            { value: 'Sản phụ khoa', label: 'Sản phụ khoa' },
            { value: 'Nhi khoa', label: 'Nhi khoa' },
            { value: 'Tim mạch', label: 'Tim mạch' },
            { value: 'Da liễu', label: 'Da liễu' },
            { value: 'Mắt', label: 'Nhãn khoa' },
            { value: 'Tai mũi họng', label: 'Tai mũi họng' },
            { value: 'Thần kinh', label: 'Thần kinh' },
            { value: 'Xương khớp', label: 'Xương khớp' }
        ],

        genders: [
            { value: '', label: 'Tất cả giới tính' },
            { value: 'MALE', label: 'Nam' },
            { value: 'FEMALE', label: 'Nữ' },
            { value: 'OTHER', label: 'Khác' }
        ],

        paymentStatuses: [
            { value: '', label: 'Tất cả trạng thái' },
            { value: 'SUCCESS', label: 'Thành công' },
            { value: 'PENDING', label: 'Đang xử lý' },
            { value: 'FAILED', label: 'Thất bại' },
            { value: 'REFUNDED', label: 'Đã hoàn tiền' }
        ]
    },

    // Settings
    settings: {
        pagination: {
            defaultPageSize: 10,
            pageSizeOptions: [5, 10, 20, 50, 100]
        },

        dateFormats: {
            date: 'dd/MM/yyyy',
            datetime: 'dd/MM/yyyy HH:mm',
            time: 'HH:mm'
        },

        currency: {
            locale: 'vi-VN',
            currency: 'VND'
        },

        theme: {
            primaryColor: '#667eea',
            secondaryColor: '#764ba2',
            successColor: '#10b981',
            warningColor: '#f59e0b',
            errorColor: '#ef4444'
        }
    },

    // Validation Rules
    validation: {
        username: {
            minLength: 3,
            maxLength: 50,
            pattern: /^[a-zA-Z0-9_]+$/,
            message: 'Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới'
        },

        email: {
            pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
            message: 'Email không hợp lệ'
        },

        password: {
            minLength: 6,
            pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/,
            message: 'Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ hoa, chữ thường và số'
        },

        phone: {
            pattern: /^[0-9]{10,11}$/,
            message: 'Số điện thoại phải có 10-11 chữ số'
        }
    },

    // Utility Functions
    formatCurrency(amount) {
        if (amount == null || amount === '') return '0₫';
        return new Intl.NumberFormat(this.settings.currency.locale, {
            style: 'currency',
            currency: this.settings.currency.currency
        }).format(amount);
    },

    formatDate(dateString) {
        if (!dateString) return '-';
        try {
            return new Date(dateString).toLocaleDateString('vi-VN');
        } catch (error) {
            return '-';
        }
    },

    formatDateTime(dateString) {
        if (!dateString) return '-';
        try {
            return new Date(dateString).toLocaleString('vi-VN');
        } catch (error) {
            return '-';
        }
    },

    formatTime(dateString) {
        if (!dateString) return '-';
        try {
            return new Date(dateString).toLocaleTimeString('vi-VN', {
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (error) {
            return '-';
        }
    },

    getRoleClass(role) {
        const classes = {
            'ADMIN': 'error',
            'DOCTOR': 'info',
            'PATIENT': 'success'
        };
        return classes[role] || 'info';
    },

    getGenderText(gender) {
        const texts = {
            'MALE': 'Nam',
            'FEMALE': 'Nữ',
            'OTHER': 'Khác'
        };
        return texts[gender] || gender;
    },

    getPaymentStatusClass(status) {
        const classes = {
            'SUCCESS': 'success',
            'PENDING': 'warning',
            'FAILED': 'error',
            'REFUNDED': 'info'
        };
        return classes[status] || 'info';
    },

    getSpecialtyIcon(specialty) {
        const icons = {
            'Nội khoa': 'fas fa-stethoscope',
            'Ngoại khoa': 'fas fa-cut',
            'Sản phụ khoa': 'fas fa-baby',
            'Nhi khoa': 'fas fa-child',
            'Tim mạch': 'fas fa-heartbeat',
            'Da liễu': 'fas fa-hand-paper',
            'Mắt': 'fas fa-eye',
            'Tai mũi họng': 'fas fa-head-side-mask',
            'Thần kinh': 'fas fa-brain',
            'Xương khớp': 'fas fa-bone'
        };
        return icons[specialty] || 'fas fa-user-md';
    },

    // Notification configurations
    notifications: {
        position: 'top-right',
        duration: 5000,
        maxVisible: 5
    },

    // Export configurations
    export: {
        formats: ['xlsx', 'csv', 'pdf'],
        defaultFormat: 'xlsx',
        filename: {
            users: 'danh-sach-nguoi-dung',
            doctors: 'danh-sach-bac-si',
            patients: 'danh-sach-benh-nhan',
            payments: 'bao-cao-thanh-toan'
        }
    },

    // Security settings
    security: {
        sessionTimeout: 3600000, // 1 hour in milliseconds
        maxLoginAttempts: 5,
        lockoutDuration: 300000, // 5 minutes in milliseconds
        tokenRefreshThreshold: 300000, // 5 minutes before expiry
        csrfProtection: true
    },

    // Feature flags
    features: {
        realTimeUpdates: true,
        exportFunctionality: true,
        bulkOperations: true,
        advancedFilters: true,
        notifications: true,
        darkMode: false,
        multiLanguage: false
    }
};

// Make AdminConfig globally available
if (typeof window !== 'undefined') {
    window.AdminConfig = AdminConfig;
}

// Export for Node.js environments
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AdminConfig;
}
