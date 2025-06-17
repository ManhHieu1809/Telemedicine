// js/components/Medical.js
const Medical = () => {
    const [selectedPatient, setSelectedPatient] = useState(null);

    const patients = [
        {
            id: 1,
            name: 'Sarah Johnson',
            age: 28,
            condition: 'Hypertension',
            lastVisit: '2024-01-15',
            status: 'stable',
            avatar: 'https://images.unsplash.com/photo-1494790108755-2616b9fc6168?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80'
        },
        {
            id: 2,
            name: 'Michael Chen',
            age: 45,
            condition: 'Diabetes Type 2',
            lastVisit: '2024-01-14',
            status: 'monitoring',
            avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80'
        },
        {
            id: 3,
            name: 'Emily Davis',
            age: 32,
            condition: 'Asthma',
            lastVisit: '2024-01-13',
            status: 'critical',
            avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80'
        }
    ];

    const medicalStats = [
        { label: 'Total Patients', value: '1,247', change: '+12%', color: 'bg-blue-500' },
        { label: 'Active Cases', value: '156', change: '+8%', color: 'bg-green-500' },
        { label: 'Critical Cases', value: '23', change: '-15%', color: 'bg-red-500' },
        { label: 'Recovered', value: '1,068', change: '+25%', color: 'bg-purple-500' }
    ];

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-3xl font-bold text-gray-800">Medical Dashboard</h1>
                <div className="flex space-x-4">
                    <button className="osler-btn px-6 py-3 rounded-xl">
                        <i className="fas fa-plus mr-2"></i>
                        Add Patient
                    </button>
                    <button className="bg-white border border-gray-300 px-6 py-3 rounded-xl text-gray-700 hover:bg-gray-50">
                        <i className="fas fa-filter mr-2"></i>
                        Filter
                    </button>
                </div>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                {medicalStats.map((stat, index) => (
                    <div key={index} className="glass-card rounded-2xl p-6">
                        <div className="flex items-center justify-between mb-4">
                            <div className={`w-12 h-12 ${stat.color} rounded-xl flex items-center justify-center`}>
                                <i className="fas fa-chart-line text-white"></i>
                            </div>
                            <span className="text-green-500 text-sm font-medium">{stat.change}</span>
                        </div>
                        <h3 className="text-gray-600 text-sm font-medium mb-2">{stat.label}</h3>
                        <p className="text-3xl font-bold text-gray-800">{stat.value}</p>
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Patient List */}
                <div className="lg:col-span-2 glass-card rounded-3xl p-6">
                    <div className="flex items-center justify-between mb-6">
                        <h3 className="text-xl font-semibold text-gray-800">Recent Patients</h3>
                        <div className="flex space-x-2">
                            <button className="pill-badge bg-green-100 text-green-600">Active</button>
                            <button className="pill-badge bg-gray-100 text-gray-600">All</button>
                            <button className="pill-badge bg-red-100 text-red-600">Critical</button>
                        </div>
                    </div>

                    <div className="space-y-4">
                        {patients.map((patient) => (
                            <div
                                key={patient.id}
                                onClick={() => setSelectedPatient(patient)}
                                className="flex items-center space-x-4 p-4 rounded-2xl hover:bg-gray-50 cursor-pointer transition-colors"
                            >
                                <img
                                    src={patient.avatar}
                                    alt={patient.name}
                                    className="w-12 h-12 rounded-full object-cover"
                                />
                                <div className="flex-1">
                                    <h4 className="font-semibold text-gray-800">{patient.name}</h4>
                                    <p className="text-sm text-gray-500">Age: {patient.age} • {patient.condition}</p>
                                    <p className="text-xs text-gray-400">Last visit: {patient.lastVisit}</p>
                                </div>
                                <div className="text-right">
                                    <span className={`pill-badge ${
                                        patient.status === 'stable' ? 'bg-green-100 text-green-600' :
                                            patient.status === 'monitoring' ? 'bg-yellow-100 text-yellow-600' :
                                                'bg-red-100 text-red-600'
                                    }`}>
                                        {patient.status}
                                    </span>
                                </div>
                                <button className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center">
                                    <i className="fas fa-chevron-right text-gray-400 text-sm"></i>
                                </button>
                            </div>
                        ))}
                    </div>

                    {/* Load More */}
                    <div className="mt-6 text-center">
                        <button className="text-green-600 font-medium hover:text-green-700">
                            Load More Patients
                        </button>
                    </div>
                </div>

                {/* Medical Analytics */}
                <div className="space-y-6">
                    {/* Quick Actions */}
                    <div className="glass-card rounded-3xl p-6">
                        <h3 className="text-lg font-semibold text-gray-800 mb-4">Quick Actions</h3>
                        <div className="space-y-3">
                            <button className="w-full flex items-center space-x-3 p-3 bg-blue-50 rounded-xl hover:bg-blue-100 transition-colors">
                                <div className="w-10 h-10 bg-blue-500 rounded-lg flex items-center justify-center">
                                    <i className="fas fa-calendar-plus text-white"></i>
                                </div>
                                <span className="font-medium text-gray-800">Schedule Appointment</span>
                            </button>

                            <button className="w-full flex items-center space-x-3 p-3 bg-green-50 rounded-xl hover:bg-green-100 transition-colors">
                                <div className="w-10 h-10 bg-green-500 rounded-lg flex items-center justify-center">
                                    <i className="fas fa-prescription-bottle-alt text-white"></i>
                                </div>
                                <span className="font-medium text-gray-800">Prescribe Medication</span>
                            </button>

                            <button className="w-full flex items-center space-x-3 p-3 bg-purple-50 rounded-xl hover:bg-purple-100 transition-colors">
                                <div className="w-10 h-10 bg-purple-500 rounded-lg flex items-center justify-center">
                                    <i className="fas fa-file-medical text-white"></i>
                                </div>
                                <span className="font-medium text-gray-800">View Medical Records</span>
                            </button>
                        </div>
                    </div>

                    {/* Department Status */}
                    <div className="glass-card rounded-3xl p-6">
                        <h3 className="text-lg font-semibold text-gray-800 mb-4">Department Status</h3>
                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <div className="flex items-center space-x-3">
                                    <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                                    <span className="text-sm text-gray-600">Cardiology</span>
                                </div>
                                <span className="text-sm font-medium text-gray-800">85%</span>
                            </div>

                            <div className="flex items-center justify-between">
                                <div className="flex items-center space-x-3">
                                    <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                                    <span className="text-sm text-gray-600">Neurology</span>
                                </div>
                                <span className="text-sm font-medium text-gray-800">72%</span>
                            </div>

                            <div className="flex items-center justify-between">
                                <div className="flex items-center space-x-3">
                                    <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
                                    <span className="text-sm text-gray-600">Emergency</span>
                                </div>
                                <span className="text-sm font-medium text-gray-800">95%</span>
                            </div>

                            <div className="flex items-center justify-between">
                                <div className="flex items-center space-x-3">
                                    <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                                    <span className="text-sm text-gray-600">Surgery</span>
                                </div>
                                <span className="text-sm font-medium text-gray-800">68%</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Patient Details Modal */}
            {selectedPatient && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="glass-card rounded-3xl p-8 max-w-2xl w-full max-h-[80vh] overflow-y-auto">
                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-2xl font-bold text-gray-800">Patient Details</h2>
                            <button
                                onClick={() => setSelectedPatient(null)}
                                className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center hover:bg-gray-200"
                            >
                                <i className="fas fa-times text-gray-600"></i>
                            </button>
                        </div>

                        <div className="flex items-center space-x-6 mb-8">
                            <img
                                src={selectedPatient.avatar}
                                alt={selectedPatient.name}
                                className="w-20 h-20 rounded-full object-cover"
                            />
                            <div>
                                <h3 className="text-xl font-bold text-gray-800">{selectedPatient.name}</h3>
                                <p className="text-gray-600">Age: {selectedPatient.age}</p>
                                <p className="text-gray-600">Condition: {selectedPatient.condition}</p>
                                <span className={`pill-badge mt-2 ${
                                    selectedPatient.status === 'stable' ? 'bg-green-100 text-green-600' :
                                        selectedPatient.status === 'monitoring' ? 'bg-yellow-100 text-yellow-600' :
                                            'bg-red-100 text-red-600'
                                }`}>
                                    {selectedPatient.status}
                                </span>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div className="bg-gray-50 rounded-2xl p-6">
                                <h4 className="font-semibold text-gray-800 mb-4">Vital Signs</h4>
                                <div className="space-y-3">
                                    <div className="flex justify-between">
                                        <span className="text-gray-600">Blood Pressure</span>
                                        <span className="font-medium">120/80 mmHg</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-gray-600">Heart Rate</span>
                                        <span className="font-medium">75 bpm</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-gray-600">Temperature</span>
                                        <span className="font-medium">98.6°F</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-gray-600">Oxygen Sat</span>
                                        <span className="font-medium">98%</span>
                                    </div>
                                </div>
                            </div>

                            <div className="bg-gray-50 rounded-2xl p-6">
                                <h4 className="font-semibold text-gray-800 mb-4">Recent Activity</h4>
                                <div className="space-y-3">
                                    <div className="text-sm">
                                        <span className="text-gray-600">Last Visit:</span>
                                        <span className="font-medium ml-2">{selectedPatient.lastVisit}</span>
                                    </div>
                                    <div className="text-sm">
                                        <span className="text-gray-600">Next Appointment:</span>
                                        <span className="font-medium ml-2">2024-01-22</span>
                                    </div>
                                    <div className="text-sm">
                                        <span className="text-gray-600">Assigned Doctor:</span>
                                        <span className="font-medium ml-2">Dr. White</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="mt-8 flex space-x-4">
                            <button className="osler-btn px-6 py-3 rounded-xl flex-1">
                                Update Records
                            </button>
                            <button className="bg-gray-200 text-gray-700 px-6 py-3 rounded-xl hover:bg-gray-300">
                                Send Message
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};