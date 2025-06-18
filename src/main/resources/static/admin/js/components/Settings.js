// js/components/Settings.js
const Settings = () => {
    const [activeTab, setActiveTab] = useState('general');
    const [settings, setSettings] = useState({
        notifications: {
            email: true,
            push: true,
            sms: false,
            desktop: true
        },
        privacy: {
            analytics: true,
            marketing: false,
            dataSharing: false
        },
        ai: {
            autoAnalysis: true,
            smartSuggestions: true,
            riskAssessment: true,
            learningMode: false
        },
        system: {
            darkMode: false,
            language: 'en',
            timezone: 'UTC+7',
            dateFormat: 'DD/MM/YYYY'
        }
    });

    const settingsTabs = [
        { id: 'general', label: 'General', icon: 'fas fa-cog' },
        { id: 'notifications', label: 'Notifications', icon: 'fas fa-bell' },
        { id: 'privacy', label: 'Privacy', icon: 'fas fa-shield-alt' },
        { id: 'ai', label: 'AI Settings', icon: 'fas fa-brain' },
        { id: 'security', label: 'Security', icon: 'fas fa-lock' },
        { id: 'billing', label: 'Billing', icon: 'fas fa-credit-card' }
    ];

    const handleToggle = (category, setting) => {
        setSettings(prev => ({
            ...prev,
            [category]: {
                ...prev[category],
                [setting]: !prev[category][setting]
            }
        }));
    };

    const handleSelectChange = (category, setting, value) => {
        setSettings(prev => ({
            ...prev,
            [category]: {
                ...prev[category],
                [setting]: value
            }
        }));
    };

    const renderGeneralSettings = () => (
        <div className="space-y-6">
            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">Profile Information</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Full Name</label>
                        <input
                            type="text"
                            defaultValue="Bocchi Rock"
                            className="w-full bg-gray-100 border-0 rounded-xl py-3 px-4 text-gray-700 focus:outline-none focus:ring-2 focus:ring-green-500"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Email</label>
                        <input
                            type="email"
                            defaultValue="bocchi@osler.ai"
                            className="w-full bg-gray-100 border-0 rounded-xl py-3 px-4 text-gray-700 focus:outline-none focus:ring-2 focus:ring-green-500"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Role</label>
                        <select className="w-full bg-gray-100 border-0 rounded-xl py-3 px-4 text-gray-700 focus:outline-none focus:ring-2 focus:ring-green-500">
                            <option>Administrator</option>
                            <option>Doctor</option>
                            <option>Nurse</option>
                            <option>Technician</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Department</label>
                        <select className="w-full bg-gray-100 border-0 rounded-xl py-3 px-4 text-gray-700 focus:outline-none focus:ring-2 focus:ring-green-500">
                            <option>IT Administration</option>
                            <option>Cardiology</option>
                            <option>Neurology</option>
                            <option>Emergency</option>
                        </select>
                    </div>
                </div>
            </div>

            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">System Preferences</h3>
                <div className="space-y-4">
                    <div className="flex items-center justify-between">
                        <div>
                            <h4 className="font-medium text-gray-800">Dark Mode</h4>
                            <p className="text-sm text-gray-600">Switch to dark theme for better night viewing</p>
                        </div>
                        <button
                            onClick={() => handleToggle('system', 'darkMode')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.system.darkMode ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.system.darkMode ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>

                    <div className="flex items-center justify-between">
                        <div>
                            <h4 className="font-medium text-gray-800">Language</h4>
                            <p className="text-sm text-gray-600">Choose your preferred language</p>
                        </div>
                        <select
                            value={settings.system.language}
                            onChange={(e) => handleSelectChange('system', 'language', e.target.value)}
                            className="bg-gray-100 border-0 rounded-xl py-2 px-3 text-gray-700 focus:outline-none focus:ring-2 focus:ring-green-500"
                        >
                            <option value="en">English</option>
                            <option value="vi">Tiếng Việt</option>
                            <option value="ja">日本語</option>
                            <option value="ko">한국어</option>
                        </select>
                    </div>

                    <div className="flex items-center justify-between">
                        <div>
                            <h4 className="font-medium text-gray-800">Timezone</h4>
                            <p className="text-sm text-gray-600">Set your local timezone</p>
                        </div>
                        <select
                            value={settings.system.timezone}
                            onChange={(e) => handleSelectChange('system', 'timezone', e.target.value)}
                            className="bg-gray-100 border-0 rounded-xl py-2 px-3 text-gray-700 focus:outline-none focus:ring-2 focus:ring-green-500"
                        >
                            <option value="UTC+7">UTC+7 (Vietnam)</option>
                            <option value="UTC+9">UTC+9 (Japan)</option>
                            <option value="UTC-5">UTC-5 (EST)</option>
                            <option value="UTC-8">UTC-8 (PST)</option>
                        </select>
                    </div>
                </div>
            </div>
        </div>
    );

    const renderNotificationSettings = () => (
        <div className="space-y-6">
            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">Notification Preferences</h3>
                <div className="space-y-6">
                    <div className="flex items-center justify-between py-4 border-b border-gray-100">
                        <div>
                            <h4 className="font-medium text-gray-800">Email Notifications</h4>
                            <p className="text-sm text-gray-600">Receive notifications via email</p>
                        </div>
                        <button
                            onClick={() => handleToggle('notifications', 'email')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.notifications.email ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.notifications.email ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>

                    <div className="flex items-center justify-between py-4 border-b border-gray-100">
                        <div>
                            <h4 className="font-medium text-gray-800">Push Notifications</h4>
                            <p className="text-sm text-gray-600">Receive push notifications on your device</p>
                        </div>
                        <button
                            onClick={() => handleToggle('notifications', 'push')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.notifications.push ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.notifications.push ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>

                    <div className="flex items-center justify-between py-4 border-b border-gray-100">
                        <div>
                            <h4 className="font-medium text-gray-800">SMS Notifications</h4>
                            <p className="text-sm text-gray-600">Receive important alerts via SMS</p>
                        </div>
                        <button
                            onClick={() => handleToggle('notifications', 'sms')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.notifications.sms ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.notifications.sms ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>

                    <div className="flex items-center justify-between py-4">
                        <div>
                            <h4 className="font-medium text-gray-800">Desktop Notifications</h4>
                            <p className="text-sm text-gray-600">Show notifications on your desktop</p>
                        </div>
                        <button
                            onClick={() => handleToggle('notifications', 'desktop')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.notifications.desktop ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.notifications.desktop ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>
                </div>
            </div>

            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">Notification Types</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-3">
                        <label className="flex items-center space-x-3">
                            <input type="checkbox" defaultChecked className="w-4 h-4 text-green-500 rounded focus:ring-green-500" />
                            <span className="text-gray-700">Patient appointments</span>
                        </label>
                        <label className="flex items-center space-x-3">
                            <input type="checkbox" defaultChecked className="w-4 h-4 text-green-500 rounded focus:ring-green-500" />
                            <span className="text-gray-700">Critical alerts</span>
                        </label>
                        <label className="flex items-center space-x-3">
                            <input type="checkbox" defaultChecked className="w-4 h-4 text-green-500 rounded focus:ring-green-500" />
                            <span className="text-gray-700">AI analysis complete</span>
                        </label>
                        <label className="flex items-center space-x-3">
                            <input type="checkbox" className="w-4 h-4 text-green-500 rounded focus:ring-green-500" />
                            <span className="text-gray-700">System maintenance</span>
                        </label>
                    </div>
                    <div className="space-y-3">
                        <label className="flex items-center space-x-3">
                            <input type="checkbox" defaultChecked className="w-4 h-4 text-green-500 rounded focus:ring-green-500" />
                            <span className="text-gray-700">Lab results</span>
                        </label>
                        <label className="flex items-center space-x-3">
                            <input type="checkbox" className="w-4 h-4 text-green-500 rounded focus:ring-green-500" />
                            <span className="text-gray-700">Marketing updates</span>
                        </label>
                        <label className="flex items-center space-x-3">
                            <input type="checkbox" defaultChecked className="w-4 h-4 text-green-500 rounded focus:ring-green-500" />
                            <span className="text-gray-700">Security alerts</span>
                        </label>
                        <label className="flex items-center space-x-3">
                            <input type="checkbox" className="w-4 h-4 text-green-500 rounded focus:ring-green-500" />
                            <span className="text-gray-700">Feature updates</span>
                        </label>
                    </div>
                </div>
            </div>
        </div>
    );

    const renderPrivacySettings = () => (
        <div className="space-y-6">
            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">Privacy Controls</h3>
                <div className="space-y-6">
                    <div className="flex items-center justify-between py-4 border-b border-gray-100">
                        <div>
                            <h4 className="font-medium text-gray-800">Analytics & Performance</h4>
                            <p className="text-sm text-gray-600">Help improve Osler by sharing usage analytics</p>
                        </div>
                        <button
                            onClick={() => handleToggle('privacy', 'analytics')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.privacy.analytics ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.privacy.analytics ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>

                    <div className="flex items-center justify-between py-4 border-b border-gray-100">
                        <div>
                            <h4 className="font-medium text-gray-800">Marketing Communications</h4>
                            <p className="text-sm text-gray-600">Receive promotional content and product updates</p>
                        </div>
                        <button
                            onClick={() => handleToggle('privacy', 'marketing')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.privacy.marketing ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.privacy.marketing ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>

                    <div className="flex items-center justify-between py-4">
                        <div>
                            <h4 className="font-medium text-gray-800">Data Sharing</h4>
                            <p className="text-sm text-gray-600">Share anonymized data for research purposes</p>
                        </div>
                        <button
                            onClick={() => handleToggle('privacy', 'dataSharing')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.privacy.dataSharing ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.privacy.dataSharing ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>
                </div>
            </div>

            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">Data Management</h3>
                <div className="space-y-4">
                    <button className="w-full flex items-center justify-between p-4 bg-blue-50 rounded-xl hover:bg-blue-100 transition-colors">
                        <div className="flex items-center space-x-3">
                            <div className="w-10 h-10 bg-blue-500 rounded-lg flex items-center justify-center">
                                <i className="fas fa-download text-white"></i>
                            </div>
                            <div className="text-left">
                                <h4 className="font-medium text-gray-800">Download Your Data</h4>
                                <p className="text-sm text-gray-600">Export all your personal data</p>
                            </div>
                        </div>
                        <i className="fas fa-chevron-right text-gray-400"></i>
                    </button>

                    <button className="w-full flex items-center justify-between p-4 bg-red-50 rounded-xl hover:bg-red-100 transition-colors">
                        <div className="flex items-center space-x-3">
                            <div className="w-10 h-10 bg-red-500 rounded-lg flex items-center justify-center">
                                <i className="fas fa-trash text-white"></i>
                            </div>
                            <div className="text-left">
                                <h4 className="font-medium text-gray-800">Delete Account</h4>
                                <p className="text-sm text-gray-600">Permanently delete your account and data</p>
                            </div>
                        </div>
                        <i className="fas fa-chevron-right text-gray-400"></i>
                    </button>
                </div>
            </div>
        </div>
    );

    const renderAISettings = () => (
        <div className="space-y-6">
            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">AI Preferences</h3>
                <div className="space-y-6">
                    <div className="flex items-center justify-between py-4 border-b border-gray-100">
                        <div>
                            <h4 className="font-medium text-gray-800">Automatic Analysis</h4>
                            <p className="text-sm text-gray-600">Enable AI to automatically analyze patient data</p>
                        </div>
                        <button
                            onClick={() => handleToggle('ai', 'autoAnalysis')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.ai.autoAnalysis ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.ai.autoAnalysis ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>

                    <div className="flex items-center justify-between py-4 border-b border-gray-100">
                        <div>
                            <h4 className="font-medium text-gray-800">Smart Suggestions</h4>
                            <p className="text-sm text-gray-600">Get AI-powered treatment and diagnosis suggestions</p>
                        </div>
                        <button
                            onClick={() => handleToggle('ai', 'smartSuggestions')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.ai.smartSuggestions ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.ai.smartSuggestions ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>

                    <div className="flex items-center justify-between py-4 border-b border-gray-100">
                        <div>
                            <h4 className="font-medium text-gray-800">Risk Assessment</h4>
                            <p className="text-sm text-gray-600">Enable AI risk assessment for patients</p>
                        </div>
                        <button
                            onClick={() => handleToggle('ai', 'riskAssessment')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.ai.riskAssessment ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.ai.riskAssessment ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>

                    <div className="flex items-center justify-between py-4">
                        <div>
                            <h4 className="font-medium text-gray-800">Learning Mode</h4>
                            <p className="text-sm text-gray-600">Allow AI to learn from your decisions (Beta)</p>
                        </div>
                        <button
                            onClick={() => handleToggle('ai', 'learningMode')}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                                settings.ai.learningMode ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                        >
                            <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                    settings.ai.learningMode ? 'translate-x-6' : 'translate-x-1'
                                }`}
                            />
                        </button>
                    </div>
                </div>
            </div>

            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">AI Model Information</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="bg-gray-50 rounded-xl p-4">
                        <h4 className="font-medium text-gray-800 mb-2">Current Model</h4>
                        <p className="text-2xl font-bold text-green-600 mb-1">Osler AI v2.1.4</p>
                        <p className="text-sm text-gray-600">Last updated: 2 hours ago</p>
                    </div>
                    <div className="bg-gray-50 rounded-xl p-4">
                        <h4 className="font-medium text-gray-800 mb-2">Accuracy Rate</h4>
                        <p className="text-2xl font-bold text-blue-600 mb-1">94.2%</p>
                        <p className="text-sm text-gray-600">Based on 10,000+ cases</p>
                    </div>
                </div>
            </div>
        </div>
    );

    const renderSecuritySettings = () => (
        <div className="space-y-6">
            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">Security Settings</h3>
                <div className="space-y-4">
                    <button className="w-full flex items-center justify-between p-4 bg-blue-50 rounded-xl hover:bg-blue-100 transition-colors">
                        <div className="flex items-center space-x-3">
                            <div className="w-10 h-10 bg-blue-500 rounded-lg flex items-center justify-center">
                                <i className="fas fa-key text-white"></i>
                            </div>
                            <div className="text-left">
                                <h4 className="font-medium text-gray-800">Change Password</h4>
                                <p className="text-sm text-gray-600">Update your account password</p>
                            </div>
                        </div>
                        <i className="fas fa-chevron-right text-gray-400"></i>
                    </button>

                    <button className="w-full flex items-center justify-between p-4 bg-green-50 rounded-xl hover:bg-green-100 transition-colors">
                        <div className="flex items-center space-x-3">
                            <div className="w-10 h-10 bg-green-500 rounded-lg flex items-center justify-center">
                                <i className="fas fa-shield-alt text-white"></i>
                            </div>
                            <div className="text-left">
                                <h4 className="font-medium text-gray-800">Two-Factor Authentication</h4>
                                <p className="text-sm text-gray-600">Add an extra layer of security</p>
                            </div>
                        </div>
                        <span className="pill-badge bg-green-100 text-green-600">Enabled</span>
                    </button>

                    <button className="w-full flex items-center justify-between p-4 bg-purple-50 rounded-xl hover:bg-purple-100 transition-colors">
                        <div className="flex items-center space-x-3">
                            <div className="w-10 h-10 bg-purple-500 rounded-lg flex items-center justify-center">
                                <i className="fas fa-mobile-alt text-white"></i>
                            </div>
                            <div className="text-left">
                                <h4 className="font-medium text-gray-800">Trusted Devices</h4>
                                <p className="text-sm text-gray-600">Manage your trusted devices</p>
                            </div>
                        </div>
                        <i className="fas fa-chevron-right text-gray-400"></i>
                    </button>

                    <button className="w-full flex items-center justify-between p-4 bg-orange-50 rounded-xl hover:bg-orange-100 transition-colors">
                        <div className="flex items-center space-x-3">
                            <div className="w-10 h-10 bg-orange-500 rounded-lg flex items-center justify-center">
                                <i className="fas fa-history text-white"></i>
                            </div>
                            <div className="text-left">
                                <h4 className="font-medium text-gray-800">Login History</h4>
                                <p className="text-sm text-gray-600">View recent login activity</p>
                            </div>
                        </div>
                        <i className="fas fa-chevron-right text-gray-400"></i>
                    </button>
                </div>
            </div>

            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">Session Management</h3>
                <div className="space-y-4">
                    <div className="flex items-center justify-between py-3">
                        <div>
                            <h4 className="font-medium text-gray-800">Auto-logout</h4>
                            <p className="text-sm text-gray-600">Automatically logout after inactivity</p>
                        </div>
                        <select className="bg-gray-100 border-0 rounded-xl py-2 px-3 text-gray-700 focus:outline-none focus:ring-2 focus:ring-green-500">
                            <option>15 minutes</option>
                            <option>30 minutes</option>
                            <option>1 hour</option>
                            <option>Never</option>
                        </select>
                    </div>
                </div>
            </div>
        </div>
    );

    const renderBillingSettings = () => (
        <div className="space-y-6">
            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">Current Plan</h3>
                <div className="bg-gradient-to-r from-green-50 to-blue-50 rounded-2xl p-6">
                    <div className="flex items-center justify-between mb-4">
                        <div>
                            <h4 className="text-xl font-bold text-gray-800">Osler AI Pro</h4>
                            <p className="text-gray-600">Advanced AI diagnostics and unlimited patients</p>
                        </div>
                        <span className="pill-badge bg-green-100 text-green-600">Active</span>
                    </div>
                    <div className="flex items-center space-x-6">
                        <div>
                            <p className="text-3xl font-bold text-gray-800">$199<span className="text-lg font-normal text-gray-600">/month</span></p>
                        </div>
                        <div>
                            <p className="text-sm text-gray-600">Next billing: January 25, 2024</p>
                        </div>
                    </div>
                </div>
            </div>

            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">Payment Methods</h3>
                <div className="space-y-4">
                    <div className="flex items-center justify-between p-4 border border-gray-200 rounded-xl">
                        <div className="flex items-center space-x-3">
                            <div className="w-12 h-8 bg-blue-600 rounded flex items-center justify-center">
                                <i className="fab fa-cc-visa text-white"></i>
                            </div>
                            <div>
                                <h4 className="font-medium text-gray-800">**** **** **** 1234</h4>
                                <p className="text-sm text-gray-600">Expires 12/25</p>
                            </div>
                        </div>
                        <span className="pill-badge bg-green-100 text-green-600">Primary</span>
                    </div>

                    <button className="w-full flex items-center justify-center space-x-2 p-4 border-2 border-dashed border-gray-300 rounded-xl hover:border-green-500 hover:bg-green-50 transition-colors">
                        <i className="fas fa-plus text-gray-400"></i>
                        <span className="text-gray-600">Add Payment Method</span>
                    </button>
                </div>
            </div>

            <div className="glass-card rounded-2xl p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-6">Usage Statistics</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="text-center">
                        <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3">
                            <i className="fas fa-users text-blue-600 text-xl"></i>
                        </div>
                        <h4 className="text-2xl font-bold text-gray-800">2,847</h4>
                        <p className="text-sm text-gray-600">Active Patients</p>
                        <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
                            <div className="bg-blue-500 h-2 rounded-full" style={{ width: '68%' }}></div>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">68% of limit</p>
                    </div>

                    <div className="text-center">
                        <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-3">
                            <i className="fas fa-brain text-purple-600 text-xl"></i>
                        </div>
                        <h4 className="text-2xl font-bold text-gray-800">15,432</h4>
                        <p className="text-sm text-gray-600">AI Analyses</p>
                        <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
                            <div className="bg-purple-500 h-2 rounded-full" style={{ width: '45%' }}></div>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">45% of limit</p>
                    </div>

                    <div className="text-center">
                        <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
                            <i className="fas fa-database text-green-600 text-xl"></i>
                        </div>
                        <h4 className="text-2xl font-bold text-gray-800">245 GB</h4>
                        <p className="text-sm text-gray-600">Storage Used</p>
                        <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
                            <div className="bg-green-500 h-2 rounded-full" style={{ width: '82%' }}></div>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">82% of 300 GB</p>
                    </div>
                </div>
            </div>
        </div>
    );

    const renderContent = () => {
        switch (activeTab) {
            case 'general': return renderGeneralSettings();
            case 'notifications': return renderNotificationSettings();
            case 'privacy': return renderPrivacySettings();
            case 'ai': return renderAISettings();
            case 'security': return renderSecuritySettings();
            case 'billing': return renderBillingSettings();
            default: return renderGeneralSettings();
        }
    };

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-3xl font-bold text-gray-800">Settings</h1>
                <button className="osler-btn px-6 py-3 rounded-xl">
                    <i className="fas fa-save mr-2"></i>
                    Save Changes
                </button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                {/* Settings Navigation */}
                <div className="lg:col-span-1">
                    <div className="glass-card rounded-2xl p-4 sticky top-6">
                        <nav className="space-y-2">
                            {settingsTabs.map((tab) => (
                                <button
                                    key={tab.id}
                                    onClick={() => setActiveTab(tab.id)}
                                    className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl transition-all ${
                                        activeTab === tab.id
                                            ? 'osler-green text-white shadow-lg'
                                            : 'text-gray-600 hover:bg-gray-100'
                                    }`}
                                >
                                    <i className={`${tab.icon} text-lg`}></i>
                                    <span className="font-medium">{tab.label}</span>
                                </button>
                            ))}
                        </nav>
                    </div>
                </div>

                {/* Settings Content */}
                <div className="lg:col-span-3">
                    {renderContent()}
                </div>
            </div>
        </div>
    );
};