const AppointmentManagement = () => {
    const [appointments, setAppointments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filterStatus, setFilterStatus] = useState('all');

    useEffect(() => {
        const fetchAppointments = async () => {
            try {
                // Mock data since there's no admin endpoint for all appointments
                setAppointments([
                    {
                        id: 1,
                        patientName: "Nguyễn Văn A",
                        doctorName: "BS. Trần Thị B",
                        date: "2024-01-15",
                        time: "09:00",
                        status: "PENDING",
                        appointmentType: "VIDEO_CALL"
                    },
                    {
                        id: 2,
                        patientName: "Lê Thị C",
                        doctorName: "BS. Phạm Văn D",
                        date: "2024-01-15",
                        time: "10:30",
                        status: "CONFIRMED",
                        appointmentType: "IN_PERSON"
                    },
                    {
                        id: 3,
                        patientName: "Hoàng Minh E",
                        doctorName: "BS. Nguyễn Thị F",
                        date: "2024-01-14",
                        time: "14:00",
                        status: "COMPLETED",
                        appointmentType: "VIDEO_CALL"
                    },
                    {
                        id: 4,
                        patientName: "Trần Văn G",
                        doctorName: "BS. Lê Thị H",
                        date: "2024-01-12",
                        time: "11:15",
                        status: "CANCELLED",
                        appointmentType: "IN_PERSON"
                    }
                ]);
            } catch (error) {
                console.error('Error fetching appointments:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchAppointments();
    }, []);

    const getStatusColor = (status) => {
        const colors = {
            'PENDING': 'bg-yellow-100 text-yellow-800',
            'CONFIRMED': 'bg-blue-100 text-blue-800',
            'COMPLETED': 'bg-green-100 text-green-800',
            'CANCELLED': 'bg-red-100 text-red-800'
        };
        return colors[status] || 'bg-gray-100 text-gray-800';
    };

    const getStatusText = (status) => {
        const texts = {
            'PENDING': 'Chờ xác nhận',
            'CONFIRMED': 'Đã xác nhận',
            'COMPLETED': 'Hoàn thành',
            'CANCELLED': 'Đã hủy'
        };
        return texts[status] || status;
    };

    const filteredAppointments = appointments.filter(appointment =>
        filterStatus === 'all' || appointment.status === filterStatus
    );

    const exportAppointments = (format) => {
        const data = filteredAppointments.map(appointment => ({
            'ID': appointment.id,
            'Bệnh nhân': appointment.patientName,
            'Bác sĩ': appointment.doctorName,
            'Ngày': formatDate(appointment.date),
            'Giờ': appointment.time,
            'Loại hình': appointment.appointmentType === 'VIDEO_CALL' ? 'Video call' : 'Tại phòng khám',
            'Trạng thái': getStatusText(appointment.status)
        }));

        if (format === 'pdf') {
            exportUtils.exportToPDF('Báo cáo cuộc hẹn',
                data.map(item => Object.entries(item).map(([k,v]) => `${k}: ${v}`).join(', '))
            );
        } else {
            exportUtils.exportToExcel('Báo cáo cuộc hẹn', data);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div className="space-y-6 fade-in">
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold">Quản lý cuộc hẹn</h2>
                <div className="flex items-center space-x-4">
                    <select
                        value={filterStatus}
                        onChange={(e) => setFilterStatus(e.target.value)}
                        className="border rounded-lg px-3 py-2"
                    >
                        <option value="all">Tất cả</option>
                        <option value="PENDING">Chờ xác nhận</option>
                        <option value="CONFIRMED">Đã xác nhận</option>
                        <option value="COMPLETED">Hoàn thành</option>
                        <option value="CANCELLED">Đã hủy</option>
                    </select>
                    <div className="space-x-2">
                        <button
                            onClick={() => exportAppointments('pdf')}
                            className="bg-red-600 text-white px-3 py-1 rounded text-sm hover:bg-red-700"
                        >
                            📄 PDF
                        </button>
                        <button
                            onClick={() => exportAppointments('excel')}
                            className="bg-green-600 text-white px-3 py-1 rounded text-sm hover:bg-green-700"
                        >
                            📊 Excel
                        </button>
                    </div>
                </div>
            </div>

            <div className="bg-white rounded-xl shadow-lg overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Bệnh nhân</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Bác sĩ</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Ngày giờ</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Loại hình</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Trạng thái</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                        {filteredAppointments.map((appointment) => (
                            <tr key={appointment.id} className="hover:bg-gray-50">
                                <td className="px-6 py-4 font-medium">{appointment.patientName}</td>
                                <td className="px-6 py-4">{appointment.doctorName}</td>
                                <td className="px-6 py-4">
                                    <div>
                                        <p>{formatDate(appointment.date)}</p>
                                        <p className="text-sm text-gray-500">{appointment.time}</p>
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                        <span className={`px-2 py-1 text-xs rounded-full ${
                                            appointment.appointmentType === 'VIDEO_CALL'
                                                ? 'bg-purple-100 text-purple-800'
                                                : 'bg-green-100 text-green-800'
                                        }`}>
                                            {appointment.appointmentType === 'VIDEO_CALL' ? 'Video call' : 'Tại phòng khám'}
                                        </span>
                                </td>
                                <td className="px-6 py-4">
                                        <span className={`px-2 py-1 text-xs rounded-full ${getStatusColor(appointment.status)}`}>
                                            {getStatusText(appointment.status)}
                                        </span>
                                </td>
                                <td className="px-6 py-4 text-sm space-x-2">
                                    <button className="text-blue-600 hover:text-blue-900">Xem</button>
                                    {appointment.status !== 'CANCELLED' && appointment.status !== 'COMPLETED' && (
                                        <button className="text-red-600 hover:text-red-900">Hủy</button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};
