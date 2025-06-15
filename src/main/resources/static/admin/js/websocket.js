// WebSocket service for real-time updates
const useWebSocket = () => {
    const [stompClient, setStompClient] = useState(null);
    const [notifications, setNotifications] = useState([]);
    const [isConnected, setIsConnected] = useState(false);

    useEffect(() => {
        const token = localStorage.getItem('jwtToken');
        if (!token) return;

        const socket = new SockJS('http://localhost:8080/ws');
        const client = Stomp.over(socket);

        client.connect(
            { Authorization: `Bearer ${token}` },
            () => {
                setStompClient(client);
                setIsConnected(true);

                // Subscribe to admin notifications
                client.subscribe('/topic/admin-notifications', (message) => {
                    const notification = JSON.parse(message.body);
                    setNotifications(prev => [notification, ...prev.slice(0, 49)]); // Keep last 50
                });

                // Subscribe to system stats updates
                client.subscribe('/topic/system-stats', (message) => {
                    const stats = JSON.parse(message.body);
                    // Trigger stats update event
                    window.dispatchEvent(new CustomEvent('statsUpdate', { detail: stats }));
                });
            },
            (error) => {
                console.error('WebSocket connection error:', error);
                setIsConnected(false);
            }
        );

        return () => {
            if (client) {
                client.disconnect();
                setIsConnected(false);
            }
        };
    }, []);

    const sendNotification = (message) => {
        if (stompClient && isConnected) {
            stompClient.send('/app/admin-notification', {}, JSON.stringify({
                message,
                timestamp: new Date().toISOString(),
                type: 'SYSTEM'
            }));
        }
    };

    return { notifications, isConnected, sendNotification };
};