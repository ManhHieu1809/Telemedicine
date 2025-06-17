// js/components/AIAnalysis.js
const AIAnalysis = () => {
    const [selectedAnalysis, setSelectedAnalysis] = useState('health-trends');
    const [isAnalyzing, setIsAnalyzing] = useState(false);

    const analysisTypes = [
        { id: 'health-trends', name: 'Health Trends', icon: 'fas fa-chart-line', description: 'Analyze patient health patterns over time' },
        { id: 'risk-assessment', name: 'Risk Assessment', icon: 'fas fa-exclamation-triangle', description: 'Evaluate patient risk factors' },
        { id: 'drug-interactions', name: 'Drug Interactions', icon: 'fas fa-pills', description: 'Check medication compatibility' },
        { id: 'diagnostic-support', name: 'Diagnostic Support', icon: 'fas fa-brain', description: 'AI-powered diagnostic assistance' }
    ];

    const aiInsights = [
        {
            id: 1,
            type: 'Risk Alert',
            priority: 'high',
            patient: 'Sarah Johnson',
            message: 'High blood pressure trend detected. Recommend immediate consultation.',
            confidence: 87,
            date: '2024-01-15'
        },
        {
            id: 2,
            type: 'Treatment Suggestion',
            priority: 'medium',
            patient: 'Michael Chen',
            message: 'Alternative medication recommended based on patient history.',
            confidence: 73,
            date: '2024-01-14'
        },
        {
            id: 3,
            type: 'Pattern Recognition',
            priority: 'low',
            patient: 'Emily Davis',
            message: 'Improvement pattern detected in respiratory function.',
            confidence: 91,
            date: '2024-01-13'
        }
    ];

    const handleRunAnalysis = () => {
        setIsAnalyzing(true);
        setTimeout(() => {
            setIsAnalyzing(false);
        }, 3000);
    };

    const getIntensityColor = (confidence) => {
        if (confidence >= 80) return 'bg-green-500';
        if (confidence >= 60) return 'bg-yellow-500';
        return 'bg-red-500';
    };

    const getPriorityColor = (priority) => {
        switch (priority) {
            case 'high': return 'bg-red-100 text-red-600 border-red-200';
            case 'medium': return 'bg-yellow-100 text-yellow-600 border-yellow-200';
            case 'low': return 'bg-green-100 text-green-600 border-green-200';
            default: return 'bg-gray-100 text-gray-600 border-gray-200';
        }
    };

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">AI Analysis Dashboard</h1>
                    <p className="text-gray-600 mt-2">Powered by advanced machine learning algorithms</p>
                </div>
                <div className="flex space-x-4">
                    <button
                        onClick={handleRunAnalysis}
                        disabled={isAnalyzing}
                        className="osler-btn px-6 py-3 rounded-xl disabled:opacity-50"
                    >
                        {isAnalyzing ? (
                            <>
                                <i className="fas fa-spinner fa-spin mr-2"></i>
                                Analyzing...
                            </>
                        ) : (
                            <>
                                <i className="fas fa-play mr-2"></i>
                                Run Analysis
                            </>
                        )}
                    </button>
                    <button className="bg-white border border-gray-300 px-6 py-3 rounded-xl text-gray-700 hover:bg-gray-50">
                        <i className="fas fa-cog mr-2"></i>
                        Settings
                    </button>
                </div>
            </div>

            {/* Analysis Types */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                {analysisTypes.map((type) => (
                    <div
                        key={type.id}
                        onClick={() => setSelectedAnalysis(type.id)}
                        className={`glass-card rounded-2xl p-6 cursor-pointer transition-all duration-300 hover:shadow-lg ${
                            selectedAnalysis === type.id ? 'ring-2 ring-green-500 bg-green-50' : ''
                        }`}
                    >
                        <div className="text-center">
                            <div className={`w-16 h-16 rounded-2xl flex items-center justify-center mx-auto mb-4 ${
                                selectedAnalysis === type.id ? 'osler-green text-white' : 'bg-gray-100 text-gray-600'
                            }`}>
                                <i className={`${type.icon} text-2xl`}></i>
                            </div>
                            <h3 className="font-semibold text-gray-800 mb-2">{type.name}</h3>
                            <p className="text-sm text-gray-600">{type.description}</p>
                        </div>
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Main Analysis Area */}
                <div className="lg:col-span-2 space-y-6">
                    {/* AI Insights */}
                    <div className="glass-card rounded-3xl p-6">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="text-xl font-semibold text-gray-800">AI Insights & Recommendations</h3>
                            <div className="flex space-x-2">
                                <button className="pill-badge bg-blue-100 text-blue-600">All</button>
                                <button className="pill-badge bg-red-100 text-red-600">High Priority</button>
                                <button className="pill-badge bg-gray-100 text-gray-600">Recent</button>
                            </div>
                        </div>

                        <div className="space-y-4">
                            {aiInsights.map((insight) => (
                                <div key={insight.id} className="border border-gray-200 rounded-2xl p-4 hover:bg-gray-50 transition-colors">
                                    <div className="flex items-start space-x-4">
                                        <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                                            insight.priority === 'high' ? 'bg-red-100' :
                                                insight.priority === 'medium' ? 'bg-yellow-100' : 'bg-green-100'
                                        }`}>
                                            <i className={`fas ${
                                                insight.type === 'Risk Alert' ? 'fa-exclamation-triangle text-red-600' :
                                                    insight.type === 'Treatment Suggestion' ? 'fa-lightbulb text-yellow-600' :
                                                        'fa-chart-line text-green-600'
                                            }`}></i>
                                        </div>

                                        <div className="flex-1">
                                            <div className="flex items-center space-x-3 mb-2">
                                                <span className={`pill-badge border ${getPriorityColor(insight.priority)}`}>
                                                    {insight.type}
                                                </span>
                                                <span className="text-sm text-gray-500">{insight.patient}</span>
                                            </div>
                                            <p className="text-gray-800 mb-3">{insight.message}</p>
                                            <div className="flex items-center justify-between">
                                                <div className="flex items-center space-x-4">
                                                    <div className="flex items-center space-x-2">
                                                        <span className="text-xs text-gray-500">Confidence:</span>
                                                        <div className="w-16 h-2 bg-gray-200 rounded-full">
                                                            <div
                                                                className={`h-2 rounded-full ${getIntensityColor(insight.confidence)}`}
                                                                style={{ width: `${insight.confidence}%` }}
                                                            ></div>
                                                        </div>
                                                        <span className="text-xs font-medium text-gray-700">{insight.confidence}%</span>
                                                    </div>
                                                </div>
                                                <span className="text-xs text-gray-400">{insight.date}</span>
                                            </div>
                                        </div>

                                        <button className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center hover:bg-gray-200">
                                            <i className="fas fa-chevron-right text-gray-400 text-sm"></i>
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Analysis Results */}
                    <div className="glass-card rounded-3xl p-6">
                        <h3 className="text-xl font-semibold text-gray-800 mb-6">Real-time Analysis Results</h3>

                        {isAnalyzing ? (
                            <div className="text-center py-12">
                                <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 rounded-full mb-4">
                                    <i className="fas fa-brain text-green-600 text-2xl fa-pulse"></i>
                                </div>
                                <h4 className="text-lg font-semibold text-gray-800 mb-2">AI Processing...</h4>
                                <p className="text-gray-600 mb-4">Analyzing patient data and generating insights</p>
                                <div className="w-64 h-2 bg-gray-200 rounded-full mx-auto">
                                    <div className="h-2 bg-green-500 rounded-full animate-pulse" style={{ width: '60%' }}></div>
                                </div>
                            </div>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div className="bg-gray-50 rounded-2xl p-6">
                                    <h4 className="font-semibold text-gray-800 mb-4">Pattern Detection</h4>
                                    <div className="space-y-3">
                                        <div className="flex justify-between">
                                            <span className="text-gray-600">Anomalies Found</span>
                                            <span className="font-medium text-red-600">3</span>
                                        </div>
                                        <div className="flex justify-between">
                                            <span className="text-gray-600">Patterns Identified</span>
                                            <span className="font-medium text-green-600">15</span>
                                        </div>
                                        <div className="flex justify-between">
                                            <span className="text-gray-600">Risk Factors</span>
                                            <span className="font-medium text-yellow-600">7</span>
                                        </div>
                                    </div>
                                </div>

                                <div className="bg-gray-50 rounded-2xl p-6">
                                    <h4 className="font-semibold text-gray-800 mb-4">Model Performance</h4>
                                    <div className="space-y-3">
                                        <div className="flex justify-between">
                                            <span className="text-gray-600">Accuracy</span>
                                            <span className="font-medium text-green-600">94.2%</span>
                                        </div>
                                        <div className="flex justify-between">
                                            <span className="text-gray-600">Processing Time</span>
                                            <span className="font-medium text-blue-600">2.3s</span>
                                        </div>
                                        <div className="flex justify-between">
                                            <span className="text-gray-600">Data Points</span>
                                            <span className="font-medium text-purple-600">1,247</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                {/* Sidebar */}
                <div className="space-y-6">
                    {/* AI Status */}
                    <div className="glass-card rounded-3xl p-6">
                        <h3 className="text-lg font-semibold text-gray-800 mb-4">AI System Status</h3>
                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <span className="text-gray-600">Model Version</span>
                                <span className="pill-badge bg-green-100 text-green-600">v2.1.4</span>
                            </div>
                            <div className="flex items-center justify-between">
                                <span className="text-gray-600">Status</span>
                                <div className="flex items-center space-x-2">
                                    <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                                    <span className="text-sm text-green-600">Active</span>
                                </div>
                            </div>
                            <div className="flex items-center justify-between">
                                <span className="text-gray-600">Last Update</span>
                                <span className="text-sm text-gray-500">2h ago</span>
                            </div>
                            <div className="flex items-center justify-between">
                                <span className="text-gray-600">Processing Queue</span>
                                <span className="text-sm text-gray-800">12 items</span>
                            </div>
                        </div>
                    </div>

                    {/* Quick Actions */}
                    <div className="glass-card rounded-3xl p-6">
                        <h3 className="text-lg font-semibold text-gray-800 mb-4">Quick Actions</h3>
                        <div className="space-y-3">
                            <button className="w-full flex items-center space-x-3 p-3 bg-blue-50 rounded-xl hover:bg-blue-100 transition-colors">
                                <div className="w-10 h-10 bg-blue-500 rounded-lg flex items-center justify-center">
                                    <i className="fas fa-upload text-white"></i>
                                </div>
                                <span className="font-medium text-gray-800">Upload Data</span>
                            </button>

                            <button className="w-full flex items-center space-x-3 p-3 bg-purple-50 rounded-xl hover:bg-purple-100 transition-colors">
                                <div className="w-10 h-10 bg-purple-500 rounded-lg flex items-center justify-center">
                                    <i className="fas fa-robot text-white"></i>
                                </div>
                                <span className="font-medium text-gray-800">Train Model</span>
                            </button>

                            <button className="w-full flex items-center space-x-3 p-3 bg-orange-50 rounded-xl hover:bg-orange-100 transition-colors">
                                <div className="w-10 h-10 bg-orange-500 rounded-lg flex items-center justify-center">
                                    <i className="fas fa-download text-white"></i>
                                </div>
                                <span className="font-medium text-gray-800">Export Results</span>
                            </button>
                        </div>
                    </div>

                    {/* AI Performance Metrics */}
                    <div className="glass-card rounded-3xl p-6">
                        <h3 className="text-lg font-semibold text-gray-800 mb-4">Performance Metrics</h3>
                        <div className="space-y-4">
                            <div>
                                <div className="flex justify-between mb-2">
                                    <span className="text-sm text-gray-600">Accuracy</span>
                                    <span className="text-sm font-medium text-gray-800">94.2%</span>
                                </div>
                                <div className="w-full h-2 bg-gray-200 rounded-full">
                                    <div className="h-2 bg-green-500 rounded-full" style={{ width: '94.2%' }}></div>
                                </div>
                            </div>

                            <div>
                                <div className="flex justify-between mb-2">
                                    <span className="text-sm text-gray-600">Precision</span>
                                    <span className="text-sm font-medium text-gray-800">87.8%</span>
                                </div>
                                <div className="w-full h-2 bg-gray-200 rounded-full">
                                    <div className="h-2 bg-blue-500 rounded-full" style={{ width: '87.8%' }}></div>
                                </div>
                            </div>

                            <div>
                                <div className="flex justify-between mb-2">
                                    <span className="text-sm text-gray-600">Recall</span>
                                    <span className="text-sm font-medium text-gray-800">91.5%</span>
                                </div>
                                <div className="w-full h-2 bg-gray-200 rounded-full">
                                    <div className="h-2 bg-purple-500 rounded-full" style={{ width: '91.5%' }}></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};