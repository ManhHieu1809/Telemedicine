// js/components/Dashboard.js
const Dashboard = () => {
    const [timeFilter, setTimeFilter] = useState('today');
    const [chartType, setChartType] = useState('line');

    const dashboardStats = [
        {
            label: 'Active Patients',
            value: '2,847',
            change: '+12%',
            trend: 'up',
            color: 'bg-blue-500',
            icon: 'fas fa-users'
        },
        {
            label: 'Appointments Today',
            value: '156',
            change: '+8%',
            trend: 'up',
            color: 'bg-green-500',
            icon: 'fas fa-calendar-check'
        },
        {
            label: 'AI Diagnostics',
            value: '1,234',
            change: '+25%',
            trend: 'up',
            color: 'bg-purple-500',
            icon: 'fas fa-brain'
        },
        {
            label: 'System Health',
            value: '98.7%',
            change: '+0.2%',
            trend: 'up',
            color: 'bg-orange-500',
            icon: 'fas fa-heartbeat'
        }
    ];

    const recentActivities = [
        {
            id: 1,
            type: 'appointment',
            title: 'New Appointment Scheduled',
            description: 'Sarah Johnson scheduled with Dr. Smith',
            time: '2 minutes ago',
            icon: 'fas fa-calendar-plus',
            color: 'bg-blue-100 text-blue-600'
        },
        {
            id: 2,
            type: 'diagnosis',
            title: 'AI Diagnosis Completed',
            description: 'Heart analysis for Michael Chen completed',
            time: '5 minutes ago',
            icon: 'fas fa-brain',
            color: 'bg-purple-100 text-purple-600'
        },
        {
            id: 3,
            type: 'alert',
            title: 'Critical Alert',
            description: 'High blood pressure detected for Emily Davis',
            time: '10 minutes ago',
            icon: 'fas fa-exclamation-triangle',
            color: 'bg-red-100 text-red-600'
        },
        {
            id: 4,
            type: 'prescription',
            title: 'Prescription Updated',
            description: 'New medication prescribed for James Wilson',
            time: '15 minutes ago',
            icon: 'fas fa-pills',
            color: 'bg-green-100 text-green-600'
        },
        {
            id: 5,
            type: 'report',
            title: 'Lab Results Available',
            description: 'Blood test results ready for review',
            time: '30 minutes ago',
            icon: 'fas fa-file-medical',
            color: 'bg-yellow-100 text-yellow-600'
        }
    ];

    const upcomingAppointments = [
        {
            id: 1,
            patient: 'Sarah Johnson',
            doctor: 'Dr. Smith',
            time: '09:00 AM',
            type: 'Consultation',
            status: 'confirmed',
            avatar: 'https://images.unsplash.com/photo-1494790108755-2616b9fc6168?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80'
        },
        {
            id: 2,
            patient: 'Michael Chen',
            doctor: 'Dr. Johnson',
            time: '10:30 AM',
            type: 'Follow-up',
            status: 'pending',
            avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80'
        },
        {
            id: 3,
            patient: 'Emily Davis',
            doctor: 'Dr. Brown',
            time: '02:00 PM',
            type: 'Emergency',
            status: 'urgent',
            avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80'
        }
    ];

    const systemMetrics = [
        { label: 'Server Uptime', value: '99.98%', status: 'excellent' },
        { label: 'Response Time', value: '0.2s', status: 'good' },
        { label: 'Database Load', value: '45%', status: 'normal' },
        { label: 'Storage Used', value: '67%', status: 'warning' }
    ];

    const getStatusColor = (status) => {
        switch (status) {
            case 'confirmed': return 'bg-green-100 text-green-600';
            case 'pending': return 'bg-yellow-100 text-yellow-600';
            case 'urgent': return 'bg-red-100 text-red-600';
            default: return 'bg-gray-100 text-gray-600';
        }
    };

    const getMetricStatusColor = (status) => {
        switch (status) {
            case 'excellent': return 'text-green-600';
            case 'good': return 'text-blue-600';
            case 'normal': return 'text-gray-600';
            case 'warning': return 'text-orange-600';
            case 'critical': return 'text-red-600';
            default: return 'text-gray-600';
        }
    };

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Osler AI Dashboard</h1>
                    <p className="text-gray-600 mt-2">Welcome to your intelligent healthcare management system</p>
                </div>
                <div className="flex space-x-4">
                    <select
                        value={timeFilter}
                        onChange={(e) => setTimeFilter(e.target.value)}
                        className="bg-white border border-gray-300 rounded-xl px-4 py-2 text-gray-700"
                    >
                        <option value="today">Today</option>
                        <option value="week">This Week</option>
                        <option value="month">This Month</option>
                        <option value="year">This Year</option>
                    </select>
                    <button className="osler-btn px-6 py-2 rounded-xl">
                        <i className="fas fa-download mr-2"></i>
                        Export Report
                    </button>
                </div>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {dashboardStats.map((stat, index) => (
                    <div key={index} className="glass-card rounded-2xl p-6 hover:shadow-lg transition-all duration-300">
                        <div className="flex items-center justify-between mb-4">
                            <div className={`w-12 h-12 ${stat.color} rounded-xl flex items-center justify-center`}>
                                <i className={`${stat.icon} text-white text-xl`}></i>
                            </div>
                            <div className={`text-sm font-medium ${
                                stat.trend === 'up' ? 'text-green-500' : 'text-red-500'
                            }`}>
                                <i className={`fas fa-arrow-${stat.trend} mr-1`}></i>
                                {stat.change}
                            </div>
                        </div>
                        <h3 className="text-gray-600 text-sm font-medium mb-2">{stat.label}</h3>
                        <p className="text-3xl font-bold text-gray-800">{stat.value}</p>
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Main Chart Area */}
                <div className="lg:col-span-2 space-y-6">
                    {/* Analytics Chart */}
                    <div className="glass-card rounded-3xl p-6">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="text-xl font-semibold text-gray-800">Patient Analytics</h3>
                            <div className="flex space-x-2">
                                <button
                                    onClick={() => setChartType('line')}
                                    className={`pill-badge ${chartType === 'line' ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-600'}`}
                                >
                                    Line
                                </button>
                                <button
                                    onClick={() => setChartType('bar')}
                                    className={`pill-badge ${chartType === 'bar' ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-600'}`}
                                >
                                    Bar
                                </button>
                                <button className="pill-badge bg-blue-100 text-blue-600">Daily</button>
                            </div>
                        </div>

                        {/* Chart Container */}
                        <div className="chart-container">
                            <svg className="w-full h-full" viewBox="0 0 600 300">
                                <defs>
                                    <linearGradient id="chartGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                                        <stop offset="0%" style={{stopColor:'#7CB342', stopOpacity:0.3}} />
                                        <stop offset="100%" style={{stopColor:'#7CB342', stopOpacity:0}} />
                                    </linearGradient>
                                </defs>

                                {/* Grid lines */}
                                <g stroke="#e5e7eb" strokeWidth="1" opacity="0.5">
                                    <line x1="0" y1="60" x2="600" y2="60" />
                                    <line x1="0" y1="120" x2="600" y2="120" />
                                    <line x1="0" y1="180" x2="600" y2="180" />
                                    <line x1="0" y1="240" x2="600" y2="240" />
                                </g>

                                {/* Chart Line */}
                                <path
                                    d="M 50 200 Q 120 150 180 130 Q 240 110 300 120 Q 360 140 420 100 Q 480 80 550 90"
                                    stroke="#7CB342"
                                    strokeWidth="4"
                                    fill="none"
                                    strokeLinecap="round"
                                />

                                {/* Area under curve */}
                                <path
                                    d="M 50 200 Q 120 150 180 130 Q 240 110 300 120 Q 360 140 420 100 Q 480 80 550 90 L 550 300 L 50 300 Z"
                                    fill="url(#chartGradient)"
                                />

                                {/* Data points */}
                                <circle cx="180" cy="130" r="6" fill="#7CB342" />
                                <circle cx="300" cy="120" r="6" fill="#7CB342" />
                                <circle cx="420" cy="100" r="6" fill="#7CB342" />
                                <circle cx="550" cy="90" r="6" fill="#7CB342" />

                                {/* Data labels */}
                                <text x="180" y="125" fontSize="12" fill="#7CB342" fontWeight="bold" textAnchor="middle">847</text>
                                <text x="300" y="115" fontSize="12" fill="#7CB342" fontWeight="bold" textAnchor="middle">952</text>
                                <text x="420" y="95" fontSize="12" fill="#7CB342" fontWeight="bold" textAnchor="middle">1,234</text>
                                <text x="550" y="85" fontSize="12" fill="#7CB342" fontWeight="bold" textAnchor="middle">1,456</text>
                            </svg>

                            {/* X-axis labels */}
                            <div className="flex justify-between text-xs text-gray-400 mt-4 px-12">
                                <span>Jan</span>
                                <span>Feb</span>
                                <span>Mar</span>
                                <span>Apr</span>
                                <span>May</span>
                                <span>Jun</span>
                            </div>
                        </div>
                    </div>

                    {/* Recent Activities */}
                    <div className="glass-card rounded-3xl p-6">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="text-xl font-semibold text-gray-800">Recent Activities</h3>
                            <button className="text-green-600 text-sm font-medium hover:text-green-700">
                                View All
                            </button>
                        </div>

                        <div className="space-y-4">
                            {recentActivities.map((activity) => (
                                <div key={activity.id} className="flex items-start space-x-4 p-4 rounded-2xl hover:bg-gray-50 transition-colors">
                                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${activity.color}`}>
                                        <i className={`${activity.icon}`}></i>
                                    </div>
                                    <div className="flex-1">
                                        <h4 className="font-semibold text-gray-800 mb-1">{activity.title}</h4>
                                        <p className="text-gray-600 text-sm mb-2">{activity.description}</p>
                                        <span className="text-xs text-gray-400">{activity.time}</span>
                                    </div>
                                    <button className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center hover:bg-gray-200">
                                        <i className="fas fa-chevron-right text-gray-400 text-sm"></i>
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Right Sidebar */}
                <div className="space-y-6">
                    {/* Upcoming Appointments */}
                    <div className="glass-card rounded-3xl p-6">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="text-lg font-semibold text-gray-800">Today's Appointments</h3>
                            <span className="pill-badge bg-blue-100 text-blue-600">{upcomingAppointments.length}</span>
                        </div>

                        <div className="space-y-4">
                            {upcomingAppointments.map((appointment) => (
                                <div key={appointment.id} className="flex items-center space-x-3 p-3 rounded-2xl hover:bg-gray-50 cursor-pointer transition-colors">
                                    <img
                                        src={appointment.avatar}
                                        alt={appointment.patient}
                                        className="w-12 h-12 rounded-full object-cover"
                                    />
                                    <div className="flex-1">
                                        <h4 className="font-semibold text-gray-800 text-sm">{appointment.patient}</h4>
                                        <p className="text-gray-600 text-xs">{appointment.doctor}</p>
                                        <div className="flex items-center space-x-2 mt-1">
                                            <span className="text-xs text-gray-500">{appointment.time}</span>
                                            <span className={`pill-badge text-xs ${getStatusColor(appointment.status)}`}>
                                                {appointment.type}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <button className="w-full mt-4 osler-btn py-3 rounded-xl text-sm">
                            <i className="fas fa-calendar-plus mr-2"></i>
                            Schedule New Appointment
                        </button>
                    </div>

                    {/* System Health */}
                    <div className="glass-card rounded-3xl p-6">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="text-lg font-semibold text-gray-800">System Health</h3>
                            <div className="flex items-center space-x-2">
                                <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                                <span className="text-sm text-green-600">All Systems Operational</span>
                            </div>
                        </div>

                        <div className="space-y-4">
                            {systemMetrics.map((metric, index) => (
                                <div key={index} className="flex items-center justify-between py-2">
                                    <span className="text-gray-600 text-sm">{metric.label}</span>
                                    <span className={`font-semibold text-sm ${getMetricStatusColor(metric.status)}`}>
                                        {metric.value}
                                    </span>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* AI Insights */}
                    <div className="glass-card rounded-3xl p-6">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="text-lg font-semibold text-gray-800">AI Insights</h3>
                            <button className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center">
                                <i className="fas fa-brain text-gray-400"></i>
                            </button>
                        </div>

                        <div className="text-center py-4">
                            <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <i className="fas fa-lightbulb text-purple-600 text-2xl"></i>
                            </div>
                            <h4 className="font-semibold text-gray-800 mb-2">Pattern Detected</h4>
                            <p className="text-gray-600 text-sm mb-4">High correlation between weather patterns and respiratory symptoms</p>
                            <button className="text-purple-600 text-sm font-medium hover:text-purple-700">
                                View Details
                            </button>
                        </div>
                    </div>

                    {/* Quick Actions */}
                    <div className="glass-card rounded-3xl p-6">
                        <h3 className="text-lg font-semibold text-gray-800 mb-4">Quick Actions</h3>
                        <div className="grid grid-cols-2 gap-3">
                            <button className="flex flex-col items-center p-4 bg-blue-50 rounded-xl hover:bg-blue-100 transition-colors">
                                <div className="w-10 h-10 bg-blue-500 rounded-lg flex items-center justify-center mb-2">
                                    <i className="fas fa-user-plus text-white"></i>
                                </div>
                                <span className="text-xs font-medium text-gray-800">Add Patient</span>
                            </button>

                            <button className="flex flex-col items-center p-4 bg-green-50 rounded-xl hover:bg-green-100 transition-colors">
                                <div className="w-10 h-10 bg-green-500 rounded-lg flex items-center justify-center mb-2">
                                    <i className="fas fa-calendar text-white"></i>
                                </div>
                                <span className="text-xs font-medium text-gray-800">Schedule</span>
                            </button>

                            <button className="flex flex-col items-center p-4 bg-purple-50 rounded-xl hover:bg-purple-100 transition-colors">
                                <div className="w-10 h-10 bg-purple-500 rounded-lg flex items-center justify-center mb-2">
                                    <i className="fas fa-brain text-white"></i>
                                </div>
                                <span className="text-xs font-medium text-gray-800">AI Analysis</span>
                            </button>

                            <button className="flex flex-col items-center p-4 bg-orange-50 rounded-xl hover:bg-orange-100 transition-colors">
                                <div className="w-10 h-10 bg-orange-500 rounded-lg flex items-center justify-center mb-2">
                                    <i className="fas fa-file-alt text-white"></i>
                                </div>
                                <span className="text-xs font-medium text-gray-800">Reports</span>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};