// js/components/Header.js
const Header = ({ user }) => {
    const [searchQuery, setSearchQuery] = useState('');

    return (
        <header className="bg-white border-b border-gray-200 px-8 py-4">
            <div className="flex items-center justify-between">
                {/* Greeting */}
                <div className="flex items-center space-x-4">
                    <h1 className="text-2xl font-bold text-gray-800">
                        Hello, Bocchi!
                        <span className="ml-2 text-2xl">👋</span>
                    </h1>
                </div>

                {/* Search Bar */}
                <div className="flex-1 max-w-xl mx-8">
                    <div className="relative">
                        <input
                            type="text"
                            placeholder="Search the osler AI Dashboard..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full bg-gray-100 border-0 rounded-full py-3 px-6 pr-12 text-gray-700 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-green-500"
                        />
                        <button className="absolute right-4 top-1/2 transform -translate-y-1/2">
                            <i className="fas fa-search text-gray-400"></i>
                        </button>
                    </div>
                </div>

                {/* Right Section */}
                <div className="flex items-center space-x-6">
                    {/* Notifications */}
                    <div className="relative">
                        <button className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center hover:bg-gray-200 transition-colors">
                            <i className="fas fa-bell text-gray-600"></i>
                            <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 rounded-full flex items-center justify-center text-white text-xs">
                                3
                            </span>
                        </button>
                    </div>

                    <div className="relative">
                        <button className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center hover:bg-gray-200 transition-colors">
                            <i className="fas fa-comment text-gray-600"></i>
                            <span className="absolute -top-1 -right-1 w-5 h-5 bg-green-500 rounded-full flex items-center justify-center text-white text-xs">
                                5
                            </span>
                        </button>
                    </div>

                    {/* User Profile */}
                    <div className="flex items-center space-x-3">
                        <div className="text-right">
                            <p className="text-sm text-gray-500">🌟 Good Morning,</p>
                            <p className="text-sm font-semibold text-gray-800">{user.name}</p>
                        </div>
                        <div className="relative">
                            <img
                                src={user.avatar}
                                alt="Profile"
                                className="w-10 h-10 rounded-full object-cover border-2 border-green-500"
                            />
                            <button className="absolute -bottom-1 -right-1 w-4 h-4 bg-white rounded-full flex items-center justify-center border border-gray-200">
                                <i className="fas fa-chevron-down text-gray-400 text-xs"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </header>
    );
};