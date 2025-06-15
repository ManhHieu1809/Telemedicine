// Login Component
const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            const response = await api.login(email, password);

            if (response.token) {
                localStorage.setItem('jwtToken', response.token);
                localStorage.setItem('userId', response.id);
                localStorage.setItem('userRole', response.role);

                // Check if user is admin
                if (response.role === 'ADMIN') {
                    window.location.reload();
                } else {
                    setError('Bạn không có quyền truy cập trang quản trị');
                    localStorage.removeItem('jwtToken');
                    localStorage.removeItem('userId');
                    localStorage.removeItem('userRole');
                }
            }
        } catch (err) {
            console.error('Login error:', err);
            setError(err.response?.data?.message || 'Email hoặc mật khẩu không đúng');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
            <div className="max-w-md w-full bg-white rounded-xl shadow-xl p-8">
                <div className="text-center mb-8">
                    <h1 className="text-3xl font-bold text-gray-900">🏥</h1>
                    <h2 className="text-2xl font-bold text-gray-900 mt-2">Admin Panel</h2>
                    <p className="text-gray-600 mt-2">Đăng nhập để quản lý hệ thống</p>
                </div>

                {error && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                        {error}
                    </div>
                )}

                <form onSubmit={handleLogin} className="space-y-6">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Email
                        </label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                            placeholder="admin@example.com"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Mật khẩu
                        </label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                            placeholder="••••••••"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed font-medium"
                    >
                        {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
                    </button>
                </form>

                <div className="mt-8 text-center text-sm text-gray-600">
                    <p>Hướng dẫn tạo tài khoản Admin:</p>
                    <p className="mt-2 text-xs bg-gray-100 p-3 rounded">
                        1. Đăng ký tài khoản thường qua /api/auth/register<br/>
                        2. Thay đổi role trong database thành 'ADMIN'<br/>
                        3. Hoặc liên hệ quản trị viên hệ thống
                    </p>
                </div>
            </div>
        </div>
    );
};

// Main Admin Dashboard Component
const AdminDashboard = () => {
    const [currentUser, setCurrentUser] = useState(null);
    const [activeTab, setActiveTab] = useState('dashboard');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const checkAuth = async () => {
            const token = localStorage.getItem('jwtToken');
            if (!token) {
                window.location.href = '/login.html';
                return;
            }

            try {
                const user = await api.get('/users/profile');
                if (user.data.role !== 'ADMIN') {
                    alert('Bạn không có quyền truy cập trang này');
                    window.location.href = '/';
                    return;
                }
                setCurrentUser(user.data);
            } catch (error) {
                console.error('Auth error:', error);
                localStorage.removeItem('jwtToken');
                window.location.href = '/login.html';
            } finally {
                setLoading(false);
            }
        };

        checkAuth();
    }, []);

    const logout = () => {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('userId');
        localStorage.removeItem('userRole');
        window.location.href = '/login.html';
    };

    const tabs = [
        { id: 'dashboard', name: 'Tổng quan', icon: '📊' },
        { id: 'users', name: 'Người dùng', icon: '👥' },
        { id: 'appointments', name: 'Cuộc hẹn', icon: '📅' },
        { id: 'reviews', name: 'Đánh giá', icon: '⭐' },
        { id: 'settings', name: 'Cài đặt', icon: '⚙️' },
        { id: 'monitoring', name: 'Giám sát', icon: '🔍' }
    ];

    const renderContent = () => {
        switch (activeTab) {
            case 'dashboard':
                return <Dashboard />;
            case 'users':
                return <UserManagement />;
            case 'appointments':
                return <AppointmentManagement />;
            case 'reviews':
                return <ReviewManagement />;
            case 'settings':
                return <SystemSettings />;
            case 'monitoring':
                return <SystemMonitoring />;
            default:
                return <Dashboard />;
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div className="min-h-screen bg-gray-100">
            {/* Header */}
            <header className="bg-white shadow-sm border-b">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center h-16">
                        <div className="flex items-center">
                            <h1 className="text-xl font-bold text-gray-900">
                                🏥 Telemedicine Admin
                            </h1>
                        </div>

                        <div className="flex items-center space-x-4">
                            <div className="text-sm text-gray-600">
                                Xin chào, <span className="font-medium">{currentUser?.fullName || currentUser?.username}</span>
                            </div>
                            <button
                                onClick={logout}
                                className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 text-sm"
                            >
                                Đăng xuất
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="flex space-x-8">
                    {/* Sidebar */}
                    <div className="w-64 flex-shrink-0">
                        <nav className="bg-white rounded-xl shadow-lg p-4">
                            <div className="space-y-2">
                                {tabs.map((tab) => (
                                    <button
                                        key={tab.id}
                                        onClick={() => setActiveTab(tab.id)}
                                        className={`w-full flex items-center space-x-3 px-4 py-3 rounded-lg text-left transition-colors ${
                                            activeTab === tab.id
                                                ? 'bg-blue-100 text-blue-700 font-medium'
                                                : 'text-gray-600 hover:bg-gray-50'
                                        }`}
                                    >
                                        <span className="text-lg">{tab.icon}</span>
                                        <span>{tab.name}</span>
                                    </button>
                                ))}
                            </div>
                        </nav>
                    </div>

                    {/* Main Content */}
                    <div className="flex-1">
                        {renderContent()}
                    </div>
                </div>
            </div>
        </div>
    );
};

// App Router
const App = () => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const checkAuth = async () => {
            const token = localStorage.getItem('jwtToken');
            const role = localStorage.getItem('userRole');

            if (token && role === 'ADMIN') {
                try {
                    // Verify token is still valid
                    const response = await api.get('/users/profile');
                    if (response.data && response.data.role === 'ADMIN') {
                        setIsAuthenticated(true);
                    } else {
                        // Role mismatch, clear storage
                        localStorage.removeItem('jwtToken');
                        localStorage.removeItem('userId');
                        localStorage.removeItem('userRole');
                    }
                } catch (error) {
                    console.error('Token validation failed:', error);
                    // Token invalid, clear storage
                    localStorage.removeItem('jwtToken');
                    localStorage.removeItem('userId');
                    localStorage.removeItem('userRole');
                }
            }

            setLoading(false);
        };

        checkAuth();
    }, []);

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-100 flex items-center justify-center">
                <LoadingSpinner />
            </div>
        );
    }

    return isAuthenticated ? <AdminDashboard /> : <LoginPage />;
};

// Render the app
ReactDOM.render(<App />, document.getElementById('root'));