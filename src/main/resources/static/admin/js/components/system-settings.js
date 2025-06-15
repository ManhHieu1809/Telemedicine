const SystemSettings = () => {
    const [settings, setSettings] = useState({
        siteName: 'Telemedicine System',
        allowRegistration: true,
        requireEmailVerification: false,
        maxAppointmentsPerDay: 10,
        appointmentDuration: 30,
        notifications: {
            emailNotifications: true,
            smsNotifications: false,
            pushNotifications: true
        },
        maintenance: {
            enabled: false,
            message: 'Hệ thống đang bảo trì'
        }
    });

    const [loading, setLoading] = useState(false);
    const [saved, setSaved] = useState(false);

    const handleSaveSettings = async () => {
        setLoading(true);
        try {
            // await api.post('/admin/settings', settings);
            setSaved(true);
            setTimeout(() => setSaved(false), 3000);
            alert('Cài đặt đã được lưu thành công');
        } catch (error) {
            alert('Lỗi khi lưu cài đặt');
        } finally {
            setLoading(false);
        }
    };

    const handleSettingChange = (path, value) => {
        const keys = path.split('.');
        setSettings(prev => {
            const newSettings = { ...prev };
            let current = newSettings;

            for (let i = 0; i < keys.length - 1; i++) {
                current[keys[i]] = { ...current[keys[i]] };
                current = current[keys[i]];
            }

            current[keys[keys.length - 1]] = value;
            return newSettings;
        });
    };

    return (
        <div className="space-y-6 fade-in">
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold">Cấu hình hệ thống</h2>
                {saved && (
                    <div className="bg-green-100 text-green-800 px-4 py-2 rounded-lg">
                        ✅ Đã lưu thành công
                    </div>
                )}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* General Settings */}
                <div className="bg-white rounded-xl shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                        ⚙️ Cài đặt chung
                    </h3>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Tên hệ thống
                            </label>
                            <input
                                type="text"
                                value={settings.siteName}
                                onChange={(e) => handleSettingChange('siteName', e.target.value)}
                                className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
                            />
                        </div>

                        <div className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                id="allowRegistration"
                                checked={settings.allowRegistration}
                                onChange={(e) => handleSettingChange('allowRegistration', e.target.checked)}
                                className="rounded text-blue-600 focus:ring-blue-500"
                            />
                            <label htmlFor="allowRegistration" className="text-sm font-medium text-gray-700">
                                Cho phép đăng ký tài khoản mới
                            </label>
                        </div>

                        <div className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                id="requireEmailVerification"
                                checked={settings.requireEmailVerification}
                                onChange={(e) => handleSettingChange('requireEmailVerification', e.target.checked)}
                                className="rounded text-blue-600 focus:ring-blue-500"
                            />
                            <label htmlFor="requireEmailVerification" className="text-sm font-medium text-gray-700">
                                Yêu cầu xác thực email
                            </label>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Số cuộc hẹn tối đa mỗi ngày
                            </label>
                            <input
                                type="number"
                                value={settings.maxAppointmentsPerDay}
                                onChange={(e) => handleSettingChange('maxAppointmentsPerDay', parseInt(e.target.value))}
                                className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
                                min="1"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Thời gian mỗi cuộc hẹn (phút)
                            </label>
                            <select
                                value={settings.appointmentDuration}
                                onChange={(e) => handleSettingChange('appointmentDuration', parseInt(e.target.value))}
                                className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
                            >
                                <option value={15}>15 phút</option>
                                <option value={30}>30 phút</option>
                                <option value={45}>45 phút</option>
                                <option value={60}>60 phút</option>
                            </select>
                        </div>
                    </div>
                </div>

                {/* Notification Settings */}
                <div className="bg-white rounded-xl shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                        🔔 Cài đặt thông báo
                    </h3>
                    <div className="space-y-4">
                        <div className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                id="emailNotifications"
                                checked={settings.notifications.emailNotifications}
                                onChange={(e) => handleSettingChange('notifications.emailNotifications', e.target.checked)}
                                className="rounded text-blue-600 focus:ring-blue-500"
                            />
                            <label htmlFor="emailNotifications" className="text-sm font-medium text-gray-700">
                                📧 Thông báo qua Email
                            </label>
                        </div>

                        <div className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                id="smsNotifications"
                                checked={settings.notifications.smsNotifications}
                                onChange={(e) => handleSettingChange('notifications.smsNotifications', e.target.checked)}
                                className="rounded text-blue-600 focus:ring-blue-500"
                            />
                            <label htmlFor="smsNotifications" className="text-sm font-medium text-gray-700">
                                📱 Thông báo qua SMS
                            </label>
                        </div>

                        <div className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                id="pushNotifications"
                                checked={settings.notifications.pushNotifications}
                                onChange={(e) => handleSettingChange('notifications.pushNotifications', e.target.checked)}
                                className="rounded text-blue-600 focus:ring-blue-500"
                            />
                            <label htmlFor="pushNotifications" className="text-sm font-medium text-gray-700">
                                🔔 Push Notifications
                            </label>
                        </div>

                        <div className="p-4 bg-blue-50 rounded-lg">
                            <p className="text-sm text-blue-800">
                                <strong>Lưu ý:</strong> Thay đổi cài đặt thông báo sẽ ảnh hưởng đến tất cả người dùng trong hệ thống.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Maintenance Mode */}
                <div className="bg-white rounded-xl shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                        🔧 Chế độ bảo trì
                    </h3>
                    <div className="space-y-4">
                        <div className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                id="maintenanceEnabled"
                                checked={settings.maintenance.enabled}
                                onChange={(e) => handleSettingChange('maintenance.enabled', e.target.checked)}
                                className="rounded text-red-600 focus:ring-red-500"
                            />
                            <label htmlFor="maintenanceEnabled" className="text-sm font-medium text-gray-700">
                                Bật chế độ bảo trì
                            </label>
                        </div>

                        {settings.maintenance.enabled && (
                            <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                                <p className="text-red-800 text-sm font-medium">
                                    ⚠️ Hệ thống sẽ chuyển sang chế độ bảo trì
                                </p>
                            </div>
                        )}

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Thông báo bảo trì
                            </label>
                            <textarea
                                value={settings.maintenance.message}
                                onChange={(e) => handleSettingChange('maintenance.message', e.target.value)}
                                className="w-full border rounded-lg px-3 py-2 h-20 focus:ring-2 focus:ring-blue-500"
                                placeholder="Thông báo hiển thị khi hệ thống bảo trì"
                            />
                        </div>
                    </div>
                </div>

                {/* System Actions */}
                <div className="bg-white rounded-xl shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                        🛠️ Thao tác hệ thống
                    </h3>
                    <div className="space-y-3">
                        <button
                            onClick={() => {
                                if (window.confirm('Bạn có chắc chắn muốn xóa cache?')) {
                                    alert('Cache đã được xóa thành công');
                                }
                            }}
                            className="w-full bg-yellow-600 text-white py-3 rounded-lg hover:bg-yellow-700 transition-colors flex items-center justify-center space-x-2"
                        >
                            <span>🗑️</span>
                            <span>Xóa Cache hệ thống</span>
                        </button>

                        <button
                            onClick={() => {
                                if (window.confirm('Bạn có chắc chắn muốn backup dữ liệu?')) {
                                    alert('Backup đang được thực hiện...');
                                }
                            }}
                            className="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center space-x-2"
                        >
                            <span>💾</span>
                            <span>Backup dữ liệu</span>
                        </button>

                        <button
                            onClick={() => {
                                if (window.confirm('Bạn có chắc chắn muốn khởi động lại hệ thống?')) {
                                    alert('Hệ thống sẽ khởi động lại trong 30 giây');
                                }
                            }}
                            className="w-full bg-red-600 text-white py-3 rounded-lg hover:bg-red-700 transition-colors flex items-center justify-center space-x-2"
                        >
                            <span>🔄</span>
                            <span>Khởi động lại hệ thống</span>
                        </button>

                        <button
                            onClick={() => {
                                const configData = JSON.stringify(settings, null, 2);
                                const blob = new Blob([configData], { type: 'application/json' });
                                const url = URL.createObjectURL(blob);
                                const a = document.createElement('a');
                                a.href = url;
                                a.download = 'system-config.json';
                                a.click();
                                URL.revokeObjectURL(url);
                            }}
                            className="w-full bg-green-600 text-white py-3 rounded-lg hover:bg-green-700 transition-colors flex items-center justify-center space-x-2"
                        >
                            <span>📤</span>
                            <span>Xuất cấu hình</span>
                        </button>
                    </div>
                </div>

                {/* Security Settings */}
                <div className="bg-white rounded-xl shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                        🔒 Cài đặt bảo mật
                    </h3>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Thời gian timeout session (phút)
                            </label>
                            <select className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500">
                                <option value={30}>30 phút</option>
                                <option value={60}>1 giờ</option>
                                <option value={120}>2 giờ</option>
                                <option value={480}>8 giờ</option>
                            </select>
                        </div>

                        <div className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                id="require2FA"
                                className="rounded text-blue-600 focus:ring-blue-500"
                            />
                            <label htmlFor="require2FA" className="text-sm font-medium text-gray-700">
                                Bắt buộc xác thực 2 yếu tố (2FA)
                            </label>
                        </div>

                        <div className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                id="blockSuspiciousLogin"
                                defaultChecked
                                className="rounded text-blue-600 focus:ring-blue-500"
                            />
                            <label htmlFor="blockSuspiciousLogin" className="text-sm font-medium text-gray-700">
                                Chặn đăng nhập đáng ngờ
                            </label>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Số lần đăng nhập sai tối đa
                            </label>
                            <input
                                type="number"
                                defaultValue={5}
                                min="3"
                                max="10"
                                className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                    </div>
                </div>

                {/* Performance Settings */}
                <div className="bg-white rounded-xl shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4 flex items-center">
                        ⚡ Cài đặt hiệu suất
                    </h3>
                    <div className="space-y-4">
                        <div className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                id="enableCaching"
                                defaultChecked
                                className="rounded text-blue-600 focus:ring-blue-500"
                            />
                            <label htmlFor="enableCaching" className="text-sm font-medium text-gray-700">
                                Bật cache hệ thống
                            </label>
                        </div>

                        <div className="flex items-center space-x-3">
                            <input
                                type="checkbox"
                                id="enableCompression"
                                defaultChecked
                                className="rounded text-blue-600 focus:ring-blue-500"
                            />
                            <label htmlFor="enableCompression" className="text-sm font-medium text-gray-700">
                                Nén dữ liệu API responses
                            </label>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Giới hạn kích thước file upload (MB)
                            </label>
                            <select className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500">
                                <option value={5}>5 MB</option>
                                <option value={10}>10 MB</option>
                                <option value={25}>25 MB</option>
                                <option value={50}>50 MB</option>
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Số record tối đa mỗi trang
                            </label>
                            <select className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500">
                                <option value={20}>20</option>
                                <option value={50}>50</option>
                                <option value={100}>100</option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>

            <div className="flex justify-end space-x-4">
                <button
                    onClick={() => setSettings({
                        siteName: 'Telemedicine System',
                        allowRegistration: true,
                        requireEmailVerification: false,
                        maxAppointmentsPerDay: 10,
                        appointmentDuration: 30,
                        notifications: {
                            emailNotifications: true,
                            smsNotifications: false,
                            pushNotifications: true
                        },
                        maintenance: {
                            enabled: false,
                            message: 'Hệ thống đang bảo trì'
                        }
                    })}
                    className="bg-gray-500 text-white px-6 py-2 rounded-lg hover:bg-gray-600"
                >
                    🔄 Khôi phục mặc định
                </button>
                <button
                    onClick={handleSaveSettings}
                    disabled={loading}
                    className="bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
                >
                    {loading ? (
                        <>
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                            <span>Đang lưu...</span>
                        </>
                    ) : (
                        <>
                            <span>💾</span>
                            <span>Lưu tất cả cài đặt</span>
                        </>
                    )}
                </button>
            </div>
        </div>
    );
};