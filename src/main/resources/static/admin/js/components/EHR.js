// js/components/EHR.js
const EHR = () => {
    const [selectedRecord, setSelectedRecord] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterStatus, setFilterStatus] = useState('all');

    const healthRecords = [
        {
            id: 1,
            patientName: 'Sarah Johnson',
            recordType: 'Lab Results',
            date: '2024-01-15',
            status: 'completed',
            priority: 'normal',
            doctor: 'Dr. Smith',
            summary: 'Blood test results - All values within normal range'
        },
        {
            id: 2,
            patientName: 'Michael Chen',
            recordType: 'Imaging',
            date: '2024-01-14',
            status: 'pending',
            priority: 'high',
            doctor: 'Dr. Johnson',
            summary: 'MRI Brain scan - Awaiting radiologist review'
        },
        {
            id: 3,
            patientName: 'Emily Davis',
            recordType: 'Prescription',
            date: '2024-01-13',
            status: 'active',
            priority: 'urgent',
            doctor: 'Dr. Brown',
            summary: 'New medication prescribed for chronic condition'
        },
        {
            id: 4,
            patientName: 'James Wilson',
            recordType: 'Consultation',
            date: '2024-01-12',
            status: 'completed',
            priority: 'normal',
            doctor: 'Dr. Davis',
            summary: 'Follow-up consultation - Patient showing improvement'
        }
    ];

    const getStatusColor = (status) => {
        switch (status) {
            case 'completed': return 'bg-green-100 text-green-600';
            case 'pending': return 'bg-yellow-100 text-yellow-600';
            case 'active': return 'bg-blue-100 text-blue-600';
            default: return 'bg-gray-100 text-gray-600';
        }
    };

    const getPriorityColor = (priority) => {
        switch (priority) {
            case 'urgent': return 'bg-red-100 text-red-600';
            case 'high': return 'bg-orange-100 text-orange-600';
            case 'normal': return 'bg-gray-100 text-gray-600';
            default: return 'bg-gray-100 text-gray-600';
        }
    };

    const filteredRecords = healthRecords.filter(record => {
        const matchesSearch = record.patientName.toLowerCase().includes(searchTerm.toLowerCase()) ||
            record.recordType.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesFilter = filterStatus === 'all' || record.status === filterStatus;
        return matchesSearch && matchesFilter;
    });

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-3xl font-bold text-gray-800">Electronic Health Records</h1>
                <div className="flex space-x-4">
                    <button className="osler-btn px-6 py-3 rounded-xl">
                        <i className="fas fa-plus mr-2"></i>
                        New Record
                    </button>
                    <button className="bg-white border border-gray-300 px-6 py-3 rounded-xl text-gray-700 hover:bg-gray-50">
                        <i className="fas fa-download mr-2"></i>
                        Export
                    </button>
                </div>
            </div>

            {/* Search and Filter Bar */}
            <div className="glass-card rounded-2xl p-6">
                <div className="flex flex-col md:flex-row md:items-center space-y-4 md:space-y-0 md:space-x-6">
                    <div className="flex-1">
                        <div className="relative">
                            <input
                                type="text"
                                placeholder="Search patient name or record type..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="w-full bg-gray-100 border-0 rounded-xl py-3 px-4 pr-12 text-gray-700 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-green-500"
                            />
                            <i className="fas fa-search absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-400"></i>
                        </div>
                    </div>

                    <div className="flex space-x-4">
                        <select
                            value={filterStatus}
                            onChange={(e) => setFilterStatus(e.target.value)}
                            className="bg-gray-100 border-0 rounded-xl py-3 px-4 text-gray-700 focus:outline-none focus:ring-2 focus:ring-green-500"
                        >
                            <option value="all">All Status</option>
                            <option value="completed">Completed</option>
                            <option value="pending">Pending</option>
                            <option value="active">Active</option>
                        </select>

                        <button className="bg-gray-100 px-4 py-3 rounded-xl text-gray-700 hover:bg-gray-200">
                            <i className="fas fa-filter"></i>
                        </button>
                    </div>
                </div>
            </div>

            {/* Records Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
                {filteredRecords.map((record) => (
                    <div
                        key={record.id}
                        onClick={() => setSelectedRecord(record)}
                        className="glass-card rounded-2xl p-6 cursor-pointer hover:shadow-lg transition-all duration-300"
                    >
                        <div className="flex items-start justify-between mb-4">
                            <div className="flex space-x-2">
                                <span className={`pill-badge ${getStatusColor(record.status)}`}>
                                    {record.status}
                                </span>
                                <span className={`pill-badge ${getPriorityColor(record.priority)}`}>
                                    {record.priority}
                                </span>
                            </div>
                            <button className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center hover:bg-gray-200">
                                <i className="fas fa-chevron-right text-gray-400 text-sm"></i>
                            </button>
                        </div>

                        <h3 className="text-lg font-semibold text-gray-800 mb-2">{record.patientName}</h3>
                        <p className="text-gray-600 text-sm mb-3">{record.recordType}</p>
                        <p className="text-gray-500 text-sm mb-4 line-clamp-2">{record.summary}</p>

                        <div className="flex items-center justify-between">
                            <div className="text-xs text-gray-400">
                                <i className="fas fa-calendar mr-1"></i>
                                {record.date}
                            </div>
                            <div className="text-xs text-gray-400">
                                <i className="fas fa-user-md mr-1"></i>
                                {record.doctor}
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Quick Stats */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div className="glass-card rounded-2xl p-6 text-center">
                    <div className="w-12 h-12 bg-blue-500 rounded-xl flex items-center justify-center mx-auto mb-4">
                        <i className="fas fa-file-medical text-white"></i>
                    </div>
                    <h3 className="text-2xl font-bold text-gray-800">2,547</h3>
                    <p className="text-gray-600 text-sm">Total Records</p>
                </div>

                <div className="glass-card rounded-2xl p-6 text-center">
                    <div className="w-12 h-12 bg-green-500 rounded-xl flex items-center justify-center mx-auto mb-4">
                        <i className="fas fa-check-circle text-white"></i>
                    </div>
                    <h3 className="text-2xl font-bold text-gray-800">1,834</h3>
                    <p className="text-gray-600 text-sm">Completed</p>
                </div>

                <div className="glass-card rounded-2xl p-6 text-center">
                    <div className="w-12 h-12 bg-yellow-500 rounded-xl flex items-center justify-center mx-auto mb-4">
                        <i className="fas fa-clock text-white"></i>
                    </div>
                    <h3 className="text-2xl font-bold text-gray-800">156</h3>
                    <p className="text-gray-600 text-sm">Pending Review</p>
                </div>

                <div className="glass-card rounded-2xl p-6 text-center">
                    <div className="w-12 h-12 bg-red-500 rounded-xl flex items-center justify-center mx-auto mb-4">
                        <i className="fas fa-exclamation-triangle text-white"></i>
                    </div>
                    <h3 className="text-2xl font-bold text-gray-800">23</h3>
                    <p className="text-gray-600 text-sm">Urgent Cases</p>
                </div>
            </div>

            {/* Record Details Modal */}
            {selectedRecord && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="glass-card rounded-3xl p-8 max-w-4xl w-full max-h-[90vh] overflow-y-auto">
                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-2xl font-bold text-gray-800">Health Record Details</h2>
                            <button
                                onClick={() => setSelectedRecord(null)}
                                className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center hover:bg-gray-200"
                            >
                                <i className="fas fa-times text-gray-600"></i>
                            </button>
                        </div>

                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                            {/* Record Information */}
                            <div className="lg:col-span-2 space-y-6">
                                <div className="bg-gray-50 rounded-2xl p-6">
                                    <h3 className="text-lg font-semibold text-gray-800 mb-4">Record Information</h3>
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <label className="text-sm text-gray-600">Patient Name</label>
                                            <p className="font-medium text-gray-800">{selectedRecord.patientName}</p>
                                        </div>
                                        <div>
                                            <label className="text-sm text-gray-600">Record Type</label>
                                            <p className="font-medium text-gray-800">{selectedRecord.recordType}</p>
                                        </div>
                                        <div>
                                            <label className="text-sm text-gray-600">Date Created</label>
                                            <p className="font-medium text-gray-800">{selectedRecord.date}</p>
                                        </div>
                                        <div>
                                            <label className="text-sm text-gray-600">Attending Doctor</label>
                                            <p className="font-medium text-gray-800">{selectedRecord.doctor}</p>
                                        </div>
                                    </div>
                                </div>

                                <div className="bg-gray-50 rounded-2xl p-6">
                                    <h3 className="text-lg font-semibold text-gray-800 mb-4">Summary</h3>
                                    <p className="text-gray-700 leading-relaxed">{selectedRecord.summary}</p>
                                </div>

                                <div className="bg-gray-50 rounded-2xl p-6">
                                    <h3 className="text-lg font-semibold text-gray-800 mb-4">Additional Notes</h3>
                                    <div className="space-y-3">
                                        <div className="border-l-4 border-blue-500 pl-4">
                                            <p className="text-sm text-gray-600">Treatment Plan</p>
                                            <p className="text-gray-800">Continue current medication regimen and schedule follow-up in 2 weeks.</p>
                                        </div>
                                        <div className="border-l-4 border-green-500 pl-4">
                                            <p className="text-sm text-gray-600">Progress Notes</p>
                                            <p className="text-gray-800">Patient showing positive response to treatment. Symptoms have improved significantly.</p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Status and Actions */}
                            <div className="space-y-6">
                                <div className="bg-gray-50 rounded-2xl p-6">
                                    <h3 className="text-lg font-semibold text-gray-800 mb-4">Status</h3>
                                    <div className="space-y-4">
                                        <div className="flex items-center justify-between">
                                            <span className="text-gray-600">Current Status</span>
                                            <span className={`pill-badge ${getStatusColor(selectedRecord.status)}`}>
                                                {selectedRecord.status}
                                            </span>
                                        </div>
                                        <div className="flex items-center justify-between">
                                            <span className="text-gray-600">Priority</span>
                                            <span className={`pill-badge ${getPriorityColor(selectedRecord.priority)}`}>
                                                {selectedRecord.priority}
                                            </span>
                                        </div>
                                        <div className="flex items-center justify-between">
                                            <span className="text-gray-600">Last Updated</span>
                                            <span className="text-gray-800 text-sm">{selectedRecord.date}</span>
                                        </div>
                                    </div>
                                </div>

                                <div className="bg-gray-50 rounded-2xl p-6">
                                    <h3 className="text-lg font-semibold text-gray-800 mb-4">Quick Actions</h3>
                                    <div className="space-y-3">
                                        <button className="w-full osler-btn py-3 rounded-xl">
                                            <i className="fas fa-edit mr-2"></i>
                                            Edit Record
                                        </button>
                                        <button className="w-full bg-blue-100 text-blue-600 py-3 rounded-xl hover:bg-blue-200">
                                            <i className="fas fa-share mr-2"></i>
                                            Share Record
                                        </button>
                                        <button className="w-full bg-gray-100 text-gray-600 py-3 rounded-xl hover:bg-gray-200">
                                            <i className="fas fa-download mr-2"></i>
                                            Download PDF
                                        </button>
                                        <button className="w-full bg-red-100 text-red-600 py-3 rounded-xl hover:bg-red-200">
                                            <i className="fas fa-archive mr-2"></i>
                                            Archive Record
                                        </button>
                                    </div>
                                </div>

                                <div className="bg-gray-50 rounded-2xl p-6">
                                    <h3 className="text-lg font-semibold text-gray-800 mb-4">Related Records</h3>
                                    <div className="space-y-3">
                                        <div className="flex items-center space-x-3 p-3 bg-white rounded-xl">
                                            <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                                                <i className="fas fa-file-alt text-blue-600 text-sm"></i>
                                            </div>
                                            <div className="flex-1">
                                                <p className="text-sm font-medium text-gray-800">Previous Lab Results</p>
                                                <p className="text-xs text-gray-500">Jan 10, 2024</p>
                                            </div>
                                        </div>
                                        <div className="flex items-center space-x-3 p-3 bg-white rounded-xl">
                                            <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                                                <i className="fas fa-pills text-green-600 text-sm"></i>
                                            </div>
                                            <div className="flex-1">
                                                <p className="text-sm font-medium text-gray-800">Medication History</p>
                                                <p className="text-xs text-gray-500">Jan 08, 2024</p>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};