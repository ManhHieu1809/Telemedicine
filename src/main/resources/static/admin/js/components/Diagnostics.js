// js/components/Diagnostics.js
const Diagnostics = () => {
    const [selectedDate, setSelectedDate] = useState(12);

    // Navigation tabs
    const tabs = [
        { id: 'diagnostics', label: 'Diagnostics', active: true },
        { id: 'medical', label: 'Medical' },
        { id: 'ehr', label: 'EHR' },
        { id: 'ai-analysis', label: 'AI Analysis' }
    ];

    // Calendar data
    const calendarDays = [
        { day: 'Mon', date: 11 },
        { day: 'Tue', date: 12, active: true },
        { day: 'Wed', date: 13 },
        { day: 'Thu', date: 14 },
        { day: 'Fri', date: 15 },
        { day: 'Sat', date: 16 },
        { day: 'Sun', date: 17 },
        { day: 'Mon', date: 18 },
        { day: 'Tue', date: 19 },
        { day: 'Wed', date: 20 }
    ];

    const BodyDiagram = () => (
        <div className="relative w-full h-80 flex items-center justify-center">
            {/* Human Body Silhouette */}
            <div className="relative">
                {/* Head */}
                <div className="w-16 h-20 bg-gray-300 rounded-full mx-auto mb-2"></div>

                {/* Body */}
                <div className="w-20 h-32 bg-gray-300 rounded-t-full mx-auto mb-2 relative">
                    {/* Heart indicator */}
                    <div className="absolute top-6 left-1/2 transform -translate-x-1/2 w-3 h-3 bg-green-500 rounded-full"></div>
                </div>

                {/* Arms */}
                <div className="absolute top-20 -left-6 w-12 h-4 bg-gray-300 rounded-full transform -rotate-12"></div>
                <div className="absolute top-20 -right-6 w-12 h-4 bg-gray-300 rounded-full transform rotate-12"></div>

                {/* Legs */}
                <div className="flex justify-center space-x-2">
                    <div className="w-6 h-24 bg-gray-300 rounded-full"></div>
                    <div className="w-6 h-24 bg-gray-300 rounded-full"></div>
                </div>

                {/* Health indicators */}
                <div className="absolute top-16 left-8 w-3 h-3 bg-green-500 rounded-full"></div>
                <div className="absolute bottom-32 left-1/2 transform -translate-x-1/2 w-3 h-3 bg-green-500 rounded-full"></div>

                {/* Central health monitor */}
                <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-12 h-12 bg-gray-800 rounded-full flex items-center justify-center">
                    <i className="fas fa-heartbeat text-white"></i>
                </div>
            </div>

            {/* Health metrics around body */}
            <div className="absolute top-4 right-4 text-xs text-gray-600">100</div>
            <div className="absolute top-1/3 left-4 text-xs text-gray-600">90</div>
            <div className="absolute top-2/3 left-4 text-xs text-gray-600">80</div>
            <div className="absolute bottom-4 left-4 text-xs text-gray-600">60</div>
        </div>
    );

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Navigation Tabs */}
            <div className="flex space-x-1 bg-gray-200 rounded-full p-1 w-fit">
                {tabs.map((tab) => (
                    <button
                        key={tab.id}
                        className={`px-6 py-2 rounded-full text-sm font-medium transition-all ${
                            tab.active
                                ? 'osler-green text-white shadow-lg'
                                : 'text-gray-600 hover:text-gray-800'
                        }`}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Left Column - Health History */}
                <div className="lg:col-span-1 space-y-6">
                    {/* Health History Card */}
                    <div className="glass-card rounded-3xl p-6">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="text-lg font-semibold text-gray-800">Health History</h3>
                            <button className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center">
                                <i className="fas fa-ellipsis-h text-gray-400"></i>
                            </button>
                        </div>

                        <BodyDiagram />

                        {/* Health Indicators */}
                        <div className="mt-6 space-y-3">
                            <div className="flex items-center space-x-3">
                                <div className="w-3 h-3 bg-gray-800 rounded-full"></div>
                                <span className="text-sm text-gray-600">Liver Test</span>
                            </div>
                            <div className="flex items-center space-x-3">
                                <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                                <span className="text-sm text-gray-600">Heart Anatomy</span>
                            </div>
                            <div className="flex items-center space-x-3">
                                <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
                                <span className="text-sm text-gray-600">Lung System</span>
                            </div>
                        </div>
                    </div>

                    {/* Body Diagnostics History */}
                    <div className="glass-card rounded-3xl p-6">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="text-lg font-semibold text-gray-800">Body Diagnostics History</h3>
                            <button className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center">
                                <i className="fas fa-ellipsis-h text-gray-400"></i>
                            </button>
                        </div>

                        <div className="flex items-center justify-between">
                            <div className="flex space-x-4">
                                <div className="body-part w-12 h-12 bg-red-400 rounded-lg flex items-center justify-center">
                                    <i className="fas fa-heart text-white"></i>
                                </div>
                                <div className="body-part w-12 h-12 bg-red-400 rounded-lg flex items-center justify-center">
                                    <i className="fas fa-brain text-white"></i>
                                </div>
                                <div className="body-part w-12 h-12 bg-red-400 rounded-lg flex items-center justify-center">
                                    <i className="fas fa-lungs text-white"></i>
                                </div>
                            </div>
                            <div className="text-right">
                                <div className="text-2xl font-bold text-gray-800">12</div>
                                <div className="text-sm text-gray-500">Total</div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right Column - Schedule and Charts */}
                <div className="lg:col-span-2 space-y-6">
                    {/* AI Telehealth Schedule */}
                    <div className="schedule-card rounded-3xl p-6 text-white">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="text-lg font-semibold">Your AI Telehealth Schedule</h3>
                            <div className="flex space-x-2">
                                <button className="w-8 h-8 bg-white bg-opacity-20 rounded-full flex items-center justify-center">
                                    <i className="fas fa-chevron-left text-white text-sm"></i>
                                </button>
                                <button className="w-8 h-8 bg-white bg-opacity-20 rounded-full flex items-center justify-center">
                                    <i className="fas fa-chevron-right text-white text-sm"></i>
                                </button>
                            </div>
                        </div>

                        {/* Calendar */}
                        <div className="grid grid-cols-10 gap-2 mb-6">
                            {calendarDays.map((day, index) => (
                                <div
                                    key={index}
                                    className={`text-center p-3 rounded-xl cursor-pointer transition-all ${
                                        day.active
                                            ? 'osler-green text-white'
                                            : 'bg-white bg-opacity-10 hover:bg-opacity-20'
                                    }`}
                                >
                                    <div className="text-xs opacity-70">{day.day}</div>
                                    <div className="text-lg font-semibold">{day.date}</div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Schedules and Osler Score */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        {/* Your Schedules */}
                        <div className="glass-card rounded-3xl p-6">
                            <div className="flex items-center justify-between mb-6">
                                <h3 className="text-lg font-semibold text-gray-800">Your Schedules</h3>
                                <div className="flex items-center space-x-2">
                                    <span className="pill-badge bg-green-100 text-green-600">● Upcoming</span>
                                    <span className="pill-badge bg-red-100 text-red-600">● Cancelled</span>
                                    <select className="pill-badge bg-gray-100 text-gray-600 border-0">
                                        <option>Weekly</option>
                                    </select>
                                </div>
                            </div>

                            {/* Schedule Items */}
                            <div className="space-y-4">
                                <div className="dark-card rounded-2xl p-4 text-white">
                                    <div className="flex items-center space-x-3">
                                        <div className="w-8 h-8 osler-green rounded-lg flex items-center justify-center">
                                            <i className="fas fa-pills text-white text-sm"></i>
                                        </div>
                                        <div className="flex-1">
                                            <h4 className="font-semibold">Psylociblin X</h4>
                                            <p className="text-sm opacity-70">750mg • 24 Pills</p>
                                            <p className="text-xs opacity-50 mt-1">⏰ Before Eating</p>
                                        </div>
                                        <button className="w-6 h-6 bg-white bg-opacity-20 rounded-full flex items-center justify-center">
                                            <i className="fas fa-chevron-right text-white text-xs"></i>
                                        </button>
                                    </div>
                                </div>

                                <div className="bg-gray-50 rounded-2xl p-4">
                                    <div className="flex items-center space-x-3">
                                        <img
                                            src="https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?ixlib=rb-4.0.3&auto=format&fit=crop&w=100&q=80"
                                            alt="Doctor"
                                            className="w-12 h-12 rounded-full object-cover"
                                        />
                                        <div className="flex-1">
                                            <h4 className="font-semibold text-gray-800">General Checkup Dr. White</h4>
                                            <div className="flex items-center space-x-4 mt-1">
                                                <span className="text-sm text-gray-500">❤️ Cardiologist</span>
                                                <span className="text-sm text-gray-500">📍 1.2km</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Timeline */}
                            <div className="mt-6 flex items-center justify-between text-xs text-gray-400">
                                <span>10:00 AM</span>
                                <span>11:00 AM</span>
                                <span>12:00 AM</span>
                                <span>01:00 PM</span>
                                <span>02:00 PM</span>
                            </div>
                        </div>

                        {/* Osler Score */}
                        <div className="glass-card rounded-3xl p-6">
                            <div className="flex items-center justify-between mb-6">
                                <h3 className="text-lg font-semibold text-gray-800">Osler Score</h3>
                                <div className="flex items-center space-x-2">
                                    <select className="pill-badge bg-gray-100 text-gray-600 border-0">
                                        <option>Yearly</option>
                                    </select>
                                    <span className="pill-badge bg-red-100 text-red-600">⚠️ No Anomalies</span>
                                </div>
                            </div>

                            <div className="text-center mb-6">
                                <div className="text-4xl font-bold text-gray-800">87.52%</div>
                            </div>

                            {/* Chart Area */}
                            <div className="chart-container relative">
                                <svg className="w-full h-full" viewBox="0 0 300 200">
                                    <defs>
                                        <linearGradient id="scoreGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                                            <stop offset="0%" style={{stopColor:'#7CB342', stopOpacity:0.3}} />
                                            <stop offset="100%" style={{stopColor:'#7CB342', stopOpacity:0}} />
                                        </linearGradient>
                                    </defs>

                                    {/* Grid lines */}
                                    <g stroke="#e5e7eb" strokeWidth="1" opacity="0.5">
                                        <line x1="0" y1="40" x2="300" y2="40" />
                                        <line x1="0" y1="80" x2="300" y2="80" />
                                        <line x1="0" y1="120" x2="300" y2="120" />
                                        <line x1="0" y1="160" x2="300" y2="160" />
                                    </g>

                                    {/* Score line */}
                                    <path
                                        d="M 30 140 Q 80 100 120 80 Q 160 60 200 70 Q 240 80 270 60"
                                        stroke="#7CB342"
                                        strokeWidth="3"
                                        fill="none"
                                        strokeLinecap="round"
                                    />

                                    {/* Area under curve */}
                                    <path
                                        d="M 30 140 Q 80 100 120 80 Q 160 60 200 70 Q 240 80 270 60 L 270 200 L 30 200 Z"
                                        fill="url(#scoreGradient)"
                                    />

                                    {/* Data points */}
                                    <circle cx="120" cy="80" r="4" fill="#7CB342" />
                                    <circle cx="200" cy="70" r="4" fill="#7CB342" />
                                    <circle cx="270" cy="60" r="4" fill="#7CB342" />

                                    {/* Score markers */}
                                    <circle cx="120" cy="80" r="8" fill="white" stroke="#7CB342" strokeWidth="2" />
                                    <text x="125" y="75" fontSize="10" fill="#7CB342" fontWeight="bold">74</text>

                                    <circle cx="200" cy="70" r="8" fill="white" stroke="#7CB342" strokeWidth="2" />
                                    <text x="205" y="65" fontSize="10" fill="#7CB342" fontWeight="bold">82</text>

                                    <circle cx="270" cy="60" r="8" fill="white" stroke="#7CB342" strokeWidth="2" />
                                    <text x="275" y="55" fontSize="10" fill="#7CB342" fontWeight="bold">95</text>
                                </svg>

                                {/* Y-axis labels */}
                                <div className="absolute left-0 top-0 h-full flex flex-col justify-between text-xs text-gray-400 py-2">
                                    <span>100</span>
                                    <span>90</span>
                                    <span>80</span>
                                    <span>70</span>
                                    <span>60</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Go Pro Card */}
                    <div className="osler-green rounded-3xl p-6 text-white relative overflow-hidden">
                        <div className="relative z-10">
                            <h3 className="text-2xl font-bold mb-2">Go Pro, Now!</h3>
                            <p className="opacity-90 mb-6">Unlock premium features and advanced AI diagnostics</p>
                            <button className="bg-white text-green-600 px-6 py-3 rounded-full font-semibold hover:bg-gray-100 transition-colors">
                                Go Pro!
                            </button>
                        </div>

                        {/* Background decoration */}
                        <div className="absolute top-0 right-0 w-32 h-32 bg-white bg-opacity-10 rounded-full -translate-y-8 translate-x-8"></div>
                        <div className="absolute bottom-0 right-0 w-24 h-24 bg-white bg-opacity-5 rounded-full translate-y-4 translate-x-4"></div>
                    </div>
                </div>
            </div>

            {/* Heart Rate Widget */}
            <div className="fixed bottom-6 left-32 glass-card rounded-2xl p-4 w-64 shadow-lg">
                <div className="flex items-center space-x-4">
                    <div className="heart-rate-circle">
                        <i className="fas fa-heartbeat text-white text-lg"></i>
                    </div>
                    <div>
                        <h4 className="font-semibold text-gray-800">Heart Rate</h4>
                        <p className="text-2xl font-bold text-gray-800">75.2<span className="text-sm font-normal">bpm</span></p>
                        <p className="text-xs text-gray-500">Last Updated: 24.14.2025</p>
                    </div>
                    <button className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center ml-auto">
                        <i className="fas fa-chevron-down text-gray-400 text-sm"></i>
                    </button>
                </div>
            </div>
        </div>
    );
};