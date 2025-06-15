const SystemMonitoring = () => {
    const [activities, setActivities] = useState([]);
    const [systemHealth, setSystemHealth] = useState(null);
    const [loading, setLoading] = useState(true);
    const [realTimeData, setRealTimeData] = useState({
        activeUsers: 0,
        ongoingAppointments: 0,
        serverLoad: 0,
        memoryUsage: 0
    });
    const { notifications, isConnected } = useWebSocket();

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [activitiesData] = await Promise.all([
                    api.get('/admin/user-activities')
                ]);

                setActivities(activitiesData);

                // Mock system health data
                setSystemHealth({
                    status: 'healthy',
                    uptime: '15 ngày 4 giờ 23 phút',
                    lastRestart: '2024-01-01T00:00:00',
                    version: '1.0.0',
                    database: 'connected',
                    cache: 'active',
                    storage: '78% used'
                });

                // Simulate real-time data updates
                const interval = setInterval(() => {
                    setRealTimeData(prev => ({
                        activeUsers: Math.floor(Math.random() * 50) + 10,
                        ongoingAppointments: Math.floor(Math.random() * 20) + 5,
                        serverLoad: Math.floor(Math.random() * 30) + 40,
                        memoryUsage: Math.floor(Math.random() * 20) + 60
                    }));
                }, 5000);

                return () => clearInterval(interval);
            } catch (error) {
                console.error('Error fetching monitoring data:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    const exportLogs = (format) => {
        const logData = activities.map(activity => ({
            'Thời gian': formatDateTime(activity.timestamp),
            'Người dùng': activity.username,
            'Loại hoạt động': activity.activityType,
            'Mô tả': activity.description
        }));

        if (format === 'pdf') {
            exportUtils.exportToPDF('Nhật ký hệ thống',
                logData.map(item => Object.entries(item).map(([k,v]) => `${k}: ${v}`).join(', '))
            );
        } else {
            exportUtils.exportToExcel('Nhật ký hệ thống', logData);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div className="space-y-6 fade-in">
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold">Giám sát hệ thống</h2>
                <div className="flex items-center space-x-4">
                    <div className={`flex items-center space-x-2 px-3 py-1 rounded-full text-sm ${
                        isConnected ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    }`}>
                        <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`}></div>
                        <span>{isConnected ? 'Kết nối trực tiếp' : 'Mất kết nối'}</span>
                    </div>
                    <div className="space-x-2">
                        <button
                            onClick={() => exportLogs('pdf')}
                            className="bg-red-600 text-white px-3 py-1 rounded text-sm hover:bg-red-700"
                        >
                            📄 Xuất PDF
                        </button>
                        <button
                            onClick={() => exportLogs('excel')}
                            className="bg-green-600 text-white px-3 py-1 rounded text-sm hover:bg-green-700"
                        >
                            📊 Xuất Excel
                        </button>
                    </div>
                </div>
            </div>

            {/* Real-time metrics */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard
                    icon={<div className="w-6 h-6 bg-green-600 rounded animate-pulse"></div>}
                    title="Người dùng trực tuyến"
                    value={realTimeData.activeUsers}
                    description="Đang hoạt động"
                    color="green"
                />
                <StatCard
                    icon={<div className="w-6 h-6 bg-blue-600 rounded"></div>}
                    title="Cuộc hẹn đang diễn ra"
                    value={realTimeData.ongoingAppointments}
                    description="Hiện tại"
                    color="blue"
                />
                <StatCard
                    icon={<div className="w-6 h-6 bg-yellow-600 rounded"></div>}
                    title="Tải server"
                    value={`${realTimeData.serverLoad}%`}
                    description="CPU usage"
                    color="yellow"
                />
                <StatCard
                    icon={<div className="w-6 h-6 bg-purple-600 rounded"></div>}
                    title="Bộ nhớ"
                    value={`${realTimeData.memoryUsage}%`}
                    description="RAM usage"
                    color="purple"
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* System Health */}
                <div className="bg-white rounded-xl shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4">Tình trạng hệ thống</h3>
                    {systemHealth && (
                        <div className="space-y-4">
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Trạng thái:</span>
                                <span className={`px-2 py-1 rounded-full text-xs ${
                                    systemHealth.status === 'healthy' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                                }`}>
                                    {systemHealth.status === 'healthy' ? '🟢 Hoạt động tốt' : '🔴 Có vấn đề'}
                                </span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Uptime:</span>
                                <span className="font-medium">{systemHealth.uptime}</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Phiên bản:</span>
                                <span className="font-medium">{systemHealth.version}</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Database:</span>
                                <span className="text-green-600">🟢 {systemHealth.database}</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Cache:</span>
                                <span className="text-green-600">🟢 {systemHealth.cache}</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Lưu trữ:</span>
                                <span className="text-yellow-600">⚠️ {systemHealth.storage}</span>
                            </div>
                        </div>
                    )}
                </div>

                {/* Real-time Notifications */}
                <div className="bg-white rounded-xl shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4">Thông báo trực tiếp</h3>
                    <div className="space-y-3 max-h-80 overflow-y-auto">
                        {notifications.length === 0 ? (
                            <div className="text-center py-8 text-gray-500">
                                <div className="text-4xl mb-2">🔔</div>
                                <p>Chưa có thông báo mới</p>
                            </div>
                        ) : (
                            notifications.map((notif, index) => (
                                <div key={index} className="p-3 bg-gradient-to-r from-blue-50 to-blue-100 rounded-lg border-l-4 border-blue-500">
                                    <div className="flex items-start justify-between">
                                        <div className="flex-1">
                                            <p className="text-sm font-medium text-blue-900">{notif.message}</p>
                                            <p className="text-xs text-blue-600 mt-1">{formatDateTime(notif.timestamp)}</p>
                                        </div>
                                        <span className="text-lg">📢</span>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>

            {/* Activity Logs */}
            <div className="bg-white rounded-xl shadow-lg p-6">
                <h3 className="text-lg font-semibold mb-4">Nhật ký hoạt động</h3>
                <div className="space-y-3 max-h-96 overflow-y-auto">
                    {activities.map((activity, index) => (
                        <div key={index} className="flex items-start space-x-4 p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                            <div className="flex-shrink-0 w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                                <span className="text-blue-600 text-sm font-medium">
                                    {activity.username?.charAt(0).toUpperCase()}
                                </span>
                            </div>
                            <div className="flex-1 min-w-0">
                                <div className="flex items-center justify-between">
                                    <p className="text-sm font-medium text-gray-900">
                                        {activity.username}
                                    </p>
                                    <p className="text-xs text-gray-500">
                                        {formatDateTime(activity.timestamp)}
                                    </p>
                                </div>
                                <p className="text-sm text-gray-600 mt-1">
                                    <span className={`px-2 py-1 text-xs rounded-full mr-2 ${
                                        activity.activityType === 'LOGIN' ? 'bg-green-100 text-green-800' :
                                            activity.activityType === 'REGISTER' ? 'bg-blue-100 text-blue-800' :
                                                activity.activityType === 'UPDATE_PROFILE' ? 'bg-yellow-100 text-yellow-800' :
                                                    'bg-gray-100 text-gray-800'
                                    }`}>
                                        {activity.activityType}
                                    </span>
                                    {activity.description}
                                </p>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* System Performance Chart */}
            <div className="bg-white rounded-xl shadow-lg p-6">
                <h3 className="text-lg font-semibold mb-4">Hiệu suất hệ thống (24h qua)</h3>
                <div className="h-64">
                    <ChartComponent
                        type="line"
                        data={{
                            labels: Array.from({length: 24}, (_, i) => `${i}:00`),
                            datasets: [
                                {
                                    label: 'CPU (%)',
                                    data: Array.from({length: 24}, () => Math.floor(Math.random() * 30) + 40),
                                    borderColor: 'rgb(239, 68, 68)',
                                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                                    tension: 0.4
                                },
                                {
                                    label: 'Memory (%)',
                                    data: Array.from({length: 24}, () => Math.floor(Math.random() * 20) + 60),
                                    borderColor: 'rgb(59, 130, 246)',
                                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                                    tension: 0.4
                                },
                                {
                                    label: 'Active Users',
                                    data: Array.from({length: 24}, () => Math.floor(Math.random() * 40) + 10),
                                    borderColor: 'rgb(34, 197, 94)',
                                    backgroundColor: 'rgba(34, 197, 94, 0.1)',
                                    tension: 0.4
                                }
                            ]
                        }}
                        options={{
                            scales: {
                                y: {
                                    beginAtZero: true,
                                    max: 100
                                }
                            },
                            plugins: {
                                legend: {
                                    position: 'top',
                                }
                            }
                        }}
                        className="w-full h-full"
                    />
                </div>
            </div>
        </div>
    );
};