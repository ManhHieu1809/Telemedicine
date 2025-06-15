// API service
const api = {
    baseURL: 'http://localhost:8080/api',

    getHeaders() {
        const token = localStorage.getItem('jwtToken');
        const headers = {
            'Content-Type': 'application/json'
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        return headers;
    },

    async get(endpoint) {
        try {
            const response = await axios.get(`${this.baseURL}${endpoint}`, {
                headers: this.getHeaders()
            });
            return response.data;
        } catch (error) {
            if (error.response?.status === 401) {
                localStorage.removeItem('jwtToken');
                localStorage.removeItem('userId');
                window.location.reload();
            }
            throw error;
        }
    },

    async post(endpoint, data) {
        try {
            const response = await axios.post(`${this.baseURL}${endpoint}`, data, {
                headers: this.getHeaders()
            });
            return response.data;
        } catch (error) {
            if (error.response?.status === 401) {
                localStorage.removeItem('jwtToken');
                localStorage.removeItem('userId');
                window.location.reload();
            }
            throw error;
        }
    },

    async put(endpoint, data) {
        try {
            const response = await axios.put(`${this.baseURL}${endpoint}`, data, {
                headers: this.getHeaders()
            });
            return response.data;
        } catch (error) {
            if (error.response?.status === 401) {
                localStorage.removeItem('jwtToken');
                localStorage.removeItem('userId');
                window.location.reload();
            }
            throw error;
        }
    },

    async delete(endpoint) {
        try {
            const response = await axios.delete(`${this.baseURL}${endpoint}`, {
                headers: this.getHeaders()
            });
            return response.data;
        } catch (error) {
            if (error.response?.status === 401) {
                localStorage.removeItem('jwtToken');
                localStorage.removeItem('userId');
                window.location.reload();
            }
            throw error;
        }
    },

    // Login method without auth headers
    async login(email, password) {
        const response = await axios.post(`${this.baseURL}/auth/login`, {
            email,
            password
        }, {
            headers: {
                'Content-Type': 'application/json'
            }
        });
        return response.data;
    }
};