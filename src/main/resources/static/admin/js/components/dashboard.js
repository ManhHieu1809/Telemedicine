const Dashboard = () => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [chartData, setChartData] = useState(null);
    const { notifications, isConnected } = useWebSocket();

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const report = await api.get('/admin/reports');
                const activities = await api.get('/admin/user-activities');

                const statsData = {
                    ...report,
                    totalUsers: report.doctorRatings?.length || 0,
                    recentActivities: activities.slice(0, 5)
                };

                setStats(statsData);

                // Prepare chart data
                const monthlyData = generateMonthlyData();
                setChartData({
                    appointments: {
                        labels: monthlyData.labels,
                        datasets: [{
                            label: 'Cuộc hẹn',
                            data: monthlyData.appointments,
                            borderColor: 'rgb(59, 130, 246)',
                            backgroundColor: 'rgba(59, 130, 246, 0.1)',
                            tension: 0.4
                        }]
                    },
                    revenue: {
                        labels: monthlyData.labels,
                        datasets: [{
                            label: 'Doanh thu (VND)',
                            data: monthlyData.revenue,
                            backgroundColor: 'rgba(34, 197, 94, 0.8)',
                            borderColor: 'rgb(34, 197, 94)',
                            borderWidth: 1
                        }]
                    },
                    userGrowth: {
                        labels: ['Bác sĩ', 'Bệnh nhân', 'Admin'],
                        datasets: [{
                            data: [report.doctorRatings?.length || 0, 150, 5],
                            backgroundColor: [
                                'rgba(168, 85, 247, 0.8)',
                                'rgba(59, 130, 246, 0.8)',
                                'rgba(239, 68, 68, 0.8)'
                            ]
                        }]
                    }
                });
            } catch (error) {
                console.error('Error fetching stats:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchStats();

        // Listen for real-time stats updates
        const handleStatsUpdate = (event) => {
            setStats(prev => ({ ...prev, ...event.detail }));
        };

        window.addEventListener('statsUpdate', handleStatsUpdate);
        return () => window.removeEventListener('statsUpdate', handleStatsUpdate);
    }, []);

    const generateMonthlyData = () => {
        const months = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6'];
        return {
            labels: months,
            appointments: [45, 67, 89, 123, 156, 189],
            revenue: [15000000, 23000000, 34000000, 45000000, 56000000, 67000000]
        };
    };

    const exportReport = (format) => {
        const reportData = [
            `Báo cáo tổng quan hệ thống - ${new Date().toLocaleDateString('vi-VN')}`,
            '',
            `Tổng số cuộc hẹn: ${stats?.totalAppointments || 0}`,
            `Tổng doanh thu: ${(stats?.totalRevenue || 0).toLocaleString('vi-VN')} VND`,
            `Số lượng bác sĩ: ${stats?.doctorRatings?.length || 0}`,
            `Đánh giá trung bình: ${stats?.doctorRatings?.length > 0 ?
                (stats.doctorRatings.reduce((sum, dr) => sum + dr.averageRating, 0) / stats.doctorRatings.length).toFixed(1) : '0'}`,
            '',
            'Top 5 bác sĩ có đánh giá cao:',
            ...((stats?.doctorRatings || []).slice(0, 5).map((dr, i) =>
                `${i + 1}. ${dr.fullName} - ${dr.averageRating.toFixed(1)} sao (${dr.totalReviews} đánh giá)`
            ))
        ];

        if (format === 'pdf') {
            exportUtils.exportToPDF('Báo cáo tổng quan hệ thống', reportData);
        } else {
            exportUtils.exportToExcel('Báo cáo tổng quan hệ thống',
                reportData.map(item => ({ 'Thông tin': item }))
            );
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div className="space-y-6 fade-in">
            {/* Header with real-time status and export buttons */}
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold">Dashboard Tổng quan</h2>
                <div className="flex items-center space-x-4">
                    <div className={`flex items-center space-x-2 px-3 py-1 rounded-full text-sm ${
                        isConnected ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    }`}>
                        <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
                        <span>{isConnected ? 'Trực tuyến' : 'Mất kết nối'}</span>
                    </div>
                    <div className="space-x-2">
                        <button
                            onClick={() => exportReport('pdf')}
                            className="bg-red-600 text-white px-3 py-1 rounded text-sm hover:bg-red-700"
                        >
                            📄 PDF
                        </button>
                        <button
                            onClick={() => exportReport('excel')}
                            className="bg-green-600 text-white px-3 py-1 rounded text-sm hover:bg-green-700"
                        >
                            📊 Excel
                        </button>
                    </div>
                </div>
            </div>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard
                    icon={<div className="w-6 h-6 bg-blue-600 rounded"></div>}
                    title="Tổng số cuộc hẹn"
                    value={stats?.totalAppointments || 0}
                    description="Tất cả cuộc hẹn"
                    color="blue"
                />
                <StatCard
                    icon={<div className="w-6 h-6 bg-green-600 rounded"></div>}
                    title="Doanh thu"
                    value={`${(stats?.totalRevenue || 0).toLocaleString('vi-VN')} VND`}
                    description="Tổng doanh thu"
                    color="green"
                />
                <StatCard
                    icon={<div className="w-6 h-6 bg-purple-600 rounded"></div>}
                    title="Bác sĩ"
                    value={stats?.doctorRatings?.length || 0}
                    description="Tổng số bác sĩ"
                    color="purple"
                />
                <StatCard
                    icon={<div className="w-6 h-6 bg-yellow-600 rounded"></div>}
                    title="Đánh giá TB"
                    value={stats?.doctorRatings?.length > 0 ?
                        (stats.doctorRatings.reduce((sum, dr) => sum + dr.averageRating, 0) / stats.doctorRatings.length).toFixed(1) : '0'
                    }
                    description="Đánh giá trung bình"
                    color="yellow"
                />
            </div>

            {/* Charts Section */}
            {chartData && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <div className="bg-white rounded-xl shadow-lg p-6">
                        <h3 className="text-lg font-semibold mb-4">Xu hướng cuộc hẹn</h3>
                        <div className="h-64">
                            <ChartComponent
                                type="line"
                                data={chartData.appointments}
                                className="w-full h-full"
                            />
                        </div>
                    </div>

                    <div className="bg-white rounded-xl shadow-lg p-6">
                        <h3 className="text-lg font-semibold mb-4">Doanh thu theo tháng</h3>
                        <div className="h-64">
                            <ChartComponent
                                type="bar"
                                data={chartData.revenue}
                                className="w-full h-full"
                            />
                        </div>
                    </div>

                    <div className="bg-white rounded-xl shadow-lg p-6">
                        <h3 className="text-lg font-semibold mb-4">Phân bố người dùng</h3>
                        <div className="h-64">
                            <ChartComponent
                                type="doughnut"
                                data={chartData.userGrowth}
                                className="w-full h-full"
                            />
                        </div>
                    </div>

                    <div className="bg-white rounded-xl shadow-lg p-6">
                        <h3 className="text-lg font-semibold mb-4">Thông báo trực tiếp</h3>
                        <div className="space-y-2 max-h-64 overflow-y-auto">
                            {notifications.length === 0 ? (
                                <p className="text-gray-500 text-sm">Chưa có thông báo mới</p>
                            ) : (
                                notifications.slice(0, 10).map((notif, index) => (
                                    <div key={index} className="p-2 bg-blue-50 rounded text-sm">
                                        <p className="text-blue-800">{notif.message}</p>
                                        <p className="text-blue-600 text-xs">{formatDateTime(notif.timestamp)}</p>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Existing content */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="bg-white rounded-xl shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4">Bác sĩ có đánh giá cao</h3>
                    <div className="space-y-3">
                        {stats?.doctorRatings?.slice(0, 5).map((doctor, index) => (
                            <div key={doctor.doctorId} className="flex justify-between items-center p-3 bg-gray-50 rounded-lg">
                                <div>
                                    <p className="font-medium">{doctor.fullName}</p>
                                    <p className="text-sm text-gray-600">{doctor.totalReviews} đánh giá</p>
                                </div>
                                <div className="flex items-center">
                                    <span className="text-yellow-500">⭐</span>
                                    <span className="ml-1 font-medium">{doctor.averageRating.toFixed(1)}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="bg-white rounded-xl shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4">Hoạt động gần đây</h3>
                    <div className="space-y-3">
                        {stats?.recentActivities?.map((activity, index) => (
                            <div key={index} className="border-l-2 border-blue-200 pl-4 py-2">
                                <p className="text-sm font-medium">{activity.username}</p>
                                <p className="text-xs text-gray-600">{activity.description}</p>
                                <p className="text-xs text-gray-400">{formatDateTime(activity.timestamp)}</p>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};