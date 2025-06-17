// js/components/Sidebar.js
const Sidebar = ({ activeTab, setActiveTab }) => {
    const menuItems = [
        { id: 'dashboard', icon: 'fas fa-home', label: 'Dashboard' },
        { id: 'diagnostics', icon: 'fas fa-chart-line', label: 'Diagnostics', active: true },
        { id: 'medical', icon: 'fas fa-user-md', label: 'Medical' },
        { id: 'ehr', icon: 'fas fa-clipboard-list', label: 'EHR' },
        { id: 'ai-analysis', icon: 'fas fa-brain', label: 'AI Analysis' },
        { id: 'reports', icon: 'fas fa-file-alt', label: 'Reports' },
        { id: 'patients', icon: 'fas fa-users', label: 'Patients' },
        { id: 'settings', icon: 'fas fa-cog', label: 'Settings' },
        { id: 'logout', icon: 'fas fa-sign-out-alt', label: 'Logout' }
    ];

    return (
        <div className="w-20 osler-sidebar flex flex-col items-center py-6 shadow-xl">
            {/* Logo */}
            <div className="w-12 h-12 bg-white rounded-xl flex items-center justify-center mb-8 shadow-lg">
                <div className="w-8 h-8 bg-green-500 rounded-lg flex items-center justify-center">
                    <i className="fas fa-plus text-white text-lg"></i>
                </div>
            </div>

            {/* Navigation Icons */}
            <nav className="flex-1 flex flex-col space-y-4">
                {menuItems.map((item) => (
                    <button
                        key={item.id}
                        onClick={() => setActiveTab(item.id)}
                        className={`w-12 h-12 flex items-center justify-center rounded-xl transition-all duration-300 sidebar-icon ${
                            activeTab === item.id
                                ? 'nav-active shadow-lg'
                                : 'hover:bg-white hover:bg-opacity-10'
                        }`}
                        title={item.label}
                    >
                        <i className={`${item.icon} text-white text-lg`}></i>
                    </button>
                ))}
            </nav>
        </div>
    );
};