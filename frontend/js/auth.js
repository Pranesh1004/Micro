// auth.js
// Global Authentication and Role-Based Access Control

const API_GATEWAY = 'http://localhost:8210';

// Initialize Auth State
function getAuthState() {
    const token = localStorage.getItem('arfoms_token');
    const role = localStorage.getItem('arfoms_role');
    const name = localStorage.getItem('arfoms_name');
    return { token, role, name };
}

// Redirect if not logged in
function requireAuth() {
    const { token } = getAuthState();
    if (!token) {
        window.location.replace('/auth/login/index.html');
    }
}

// Check Role access
function requireRole(allowedRoles) {
    requireAuth();
    const { role } = getAuthState();
    if (role !== 'admin' && !allowedRoles.includes(role)) {
        alert("Access Denied: You do not have permission to view this module.");
        window.location.replace('/index.html'); // Redirect to dashboard
    }
}

// Global fetch wrapper that attaches the JWT token
window.apiFetch = async function(endpoint, options = {}) {
    const { token } = getAuthState();
    
    if (!options.headers) {
        options.headers = {};
    }
    
    options.headers['Content-Type'] = 'application/json';
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }
    
    const url = endpoint.startsWith('http') ? endpoint : `${API_GATEWAY}${endpoint}`;
    
    try {
        const res = await fetch(url, options);
        if (res.status === 401 || res.status === 403) {
            // Token expired or invalid
            logout();
            return null;
        }
        return res;
    } catch (err) {
        console.error("Network error:", err);
        throw err;
    }
};

// Logout
function logout() {
    localStorage.removeItem('arfoms_token');
    localStorage.removeItem('arfoms_role');
    localStorage.removeItem('arfoms_name');
    window.location.replace('/auth/login/index.html');
}

// Add user info to UI if elements exist
document.addEventListener('DOMContentLoaded', () => {
    const userNameSpan = document.getElementById('authUserName');
    const userRoleSpan = document.getElementById('authUserRole');
    const logoutBtn = document.getElementById('btnLogout');
    
    const state = getAuthState();
    if (state.name && userNameSpan) userNameSpan.textContent = state.name;
    if (state.role && userRoleSpan) userRoleSpan.textContent = state.role.toUpperCase();
    
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            logout();
        });
    }
});
