const UserManagement = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedUser, setSelectedUser] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const fetchUsers = async () => {
        try {
            setLoading(true);
            const doctors = await api.get('/users/doctors');
            setUsers(doctors.data || []);
        } catch (error) {
            console.error('Error fetching users:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const handleDeleteUser = async (userId, userType) => {
        if (!window.confirm('Bạn có chắc chắn muốn xóa người dùng này?')) return;

        try {
            if (userType === 'doctor') {
                await api.delete(`/admin/users/doctors/${userId}`);
            } else {
                await api.delete(`/admin/users/patients/${userId}`);
            }
            fetchUsers();
        } catch (error) {
            alert('Lỗi khi xóa người dùng');
        }
    };

    const exportUsers = (format) => {
        const userData = users.map(user => ({
            'ID': user.id,
            'Họ tên': user.fullName,
            'Email': user.email,
            'Chuyên khoa': user.specialty,
            'Kinh nghiệm': `${user.experience} năm`,
            'Số điện thoại': user.phone,
            'Địa chỉ': user.address
        }));

        if (format === 'pdf') {
            exportUtils.exportToPDF('Danh sách người dùng',
                userData.map(item => Object.entries(item).map(([k,v]) => `${k}: ${v}`).join(', '))
            );
        } else {
            exportUtils.exportToExcel('Danh sách người dùng', userData);
        }
    };

    const UserForm = ({ user, onSave, onCancel }) => {
        const [formData, setFormData] = useState(user || {
            username: '',
            email: '',
            fullName: '',
            phone: '',
            address: '',
            specialty: '',
            experience: 0
        });

        const handleSubmit = async (e) => {
            e.preventDefault();
            try {
                if (user?.id) {
                    await api.put(`/admin/users/doctors/${user.id}`, formData);
                } else {
                    await api.post('/admin/users/doctors', formData);
                }
                onSave();
            } catch (error) {
                alert('Lỗi khi lưu thông tin');
            }
        };

        return (
            <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                    <input
                        type="text"
                        placeholder="Tên đăng nhập"
                        value={formData.username}
                        onChange={(e) => setFormData({...formData, username: e.target.value})}
                        className="border rounded-lg px-3 py-2"
                        required
                    />
                    <input
                        type="email"
                        placeholder="Email"
                        value={formData.email}
                        onChange={(e) => setFormData({...formData, email: e.target.value})}
                        className="border rounded-lg px-3 py-2"
                        required
                    />
                </div>

                <input
                    type="text"
                    placeholder="Họ và tên"
                    value={formData.fullName}
                    onChange={(e) => setFormData({...formData, fullName: e.target.value})}
                    className="w-full border rounded-lg px-3 py-2"
                    required
                />

                <div className="grid grid-cols-2 gap-4">
                    <input
                        type="text"
                        placeholder="Số điện thoại"
                        value={formData.phone}
                        onChange={(e) => setFormData({...formData, phone: e.target.value})}
                        className="border rounded-lg px-3 py-2"
                        required
                    />
                    <input
                        type="text"
                        placeholder="Chuyên khoa"
                        value={formData.specialty}
                        onChange={(e) => setFormData({...formData, specialty: e.target.value})}
                        className="border rounded-lg px-3 py-2"
                        required
                    />
                </div>

                <textarea
                    placeholder="Địa chỉ"
                    value={formData.address}
                    onChange={(e) => setFormData({...formData, address: e.target.value})}
                    className="w-full border rounded-lg px-3 py-2 h-20"
                />

                <input
                    type="number"
                    placeholder="Kinh nghiệm (năm)"
                    value={formData.experience}
                    onChange={(e) => setFormData({...formData, experience: parseInt(e.target.value)})}
                    className="w-full border rounded-lg px-3 py-2"
                    min="0"
                />

                <div className="flex gap-3 pt-4">
                    <button
                        type="submit"
                        className="flex-1 bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700"
                    >
                        Lưu
                    </button>
                    <button
                        type="button"
                        onClick={onCancel}
                        className="flex-1 bg-gray-300 text-gray-700 py-2 rounded-lg hover:bg-gray-400"
                    >
                        Hủy
                    </button>
                </div>
            </form>
        );
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div className="space-y-6 fade-in">
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold">Quản lý người dùng</h2>
                <div className="flex items-center space-x-4">
                    <button
                        onClick={() => {
                            setSelectedUser(null);
                            setIsModalOpen(true);
                        }}
                        className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
                    >
                        Thêm bác sĩ mới
                    </button>
                    <div className="space-x-2">
                        <button
                            onClick={() => exportUsers('pdf')}
                            className="bg-red-600 text-white px-3 py-1 rounded text-sm hover:bg-red-700"
                        >
                            📄 PDF
                        </button>
                        <button
                            onClick={() => exportUsers('excel')}
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
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Họ tên</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Chuyên khoa</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Kinh nghiệm</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                        {users.map((user) => (
                            <tr key={user.id} className="hover:bg-gray-50">
                                <td className="px-6 py-4">
                                    <div className="flex items-center">
                                        <img
                                            src={user.avatarUrl || '/api/placeholder/40/40'}
                                            alt=""
                                            className="w-10 h-10 rounded-full mr-3"
                                        />
                                        <div>
                                            <p className="font-medium">{user.fullName}</p>
                                            <p className="text-sm text-gray-500">@{user.username || 'N/A'}</p>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-6 py-4 text-sm text-gray-900">{user.email}</td>
                                <td className="px-6 py-4 text-sm text-gray-900">{user.specialty}</td>
                                <td className="px-6 py-4 text-sm text-gray-900">{user.experience} năm</td>
                                <td className="px-6 py-4 text-sm space-x-2">
                                    <button
                                        onClick={() => {
                                            setSelectedUser(user);
                                            setIsModalOpen(true);
                                        }}
                                        className="text-blue-600 hover:text-blue-900"
                                    >
                                        Sửa
                                    </button>
                                    <button
                                        onClick={() => handleDeleteUser(user.id, 'doctor')}
                                        className="text-red-600 hover:text-red-900"
                                    >
                                        Xóa
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </div>

            <Modal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                title={selectedUser ? "Chỉnh sửa bác sĩ" : "Thêm bác sĩ mới"}
            >
                <UserForm
                    user={selectedUser}
                    onSave={() => {
                        setIsModalOpen(false);
                        fetchUsers();
                    }}
                    onCancel={() => setIsModalOpen(false)}
                />
            </Modal>
        </div>
    );
};