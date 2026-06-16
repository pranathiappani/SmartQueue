const API_BASE = '/api/v1';
let authToken = null;
let currentRole = null;
let currentUsername = null;
let currentQueueId = null;
let currentQueueType = null;
let fetchTokensInterval = null;
let dashboardChart = null;

/* ---------------- AUTHENTICATION ---------------- */

window.switchAuthTab = function(tab) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.auth-form').forEach(f => f.classList.remove('active'));
    document.getElementById('tab-' + tab).classList.add('active');
    document.getElementById('form-' + tab).classList.add('active');
    document.getElementById('log-error').innerText = '';
    document.getElementById('reg-error').innerText = '';
}

window.doLogin = async function() {
    const user = document.getElementById('log-username').value.trim();
    const pass = document.getElementById('log-password').value.trim();
    
    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user, password: pass })
        });
        
        if (!res.ok) throw new Error("Invalid username or password");
        
        const data = await res.json();
        
        // Very simple JWT decode to get role (in production use a proper library)
        const payloadBase64 = data.token.split('.')[1];
        const decoded = JSON.parse(atob(payloadBase64));
        const role = decoded.role; // The spring backend puts role inside jwt

        authToken = data.token;
        currentRole = role;
        currentUsername = user;
        
        localStorage.setItem('authToken', authToken);
        localStorage.setItem('currentRole', currentRole);
        localStorage.setItem('currentUsername', currentUsername);
        
        loginSuccess();
    } catch (e) {
        document.getElementById('log-error').innerText = e.message;
    }
}

window.doRegister = async function() {
    const user = document.getElementById('reg-username').value.trim();
    const pass = document.getElementById('reg-password').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const phone = document.getElementById('reg-phone').value.trim();
    const isAdmin = document.getElementById('reg-is-admin').checked;
    
    try {
        const res = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
                username: user, 
                password: pass, 
                email: email, 
                phone: phone, 
                role: isAdmin ? "ADMIN" : "USER" 
            })
        });
        
        if (!res.ok) {
            const err = await res.text();
            throw new Error(err);
        }
        
        document.getElementById('reg-error').style.color = "var(--success-color)";
        document.getElementById('reg-error').innerText = "Account created! You can now login.";
        setTimeout(() => switchAuthTab('login'), 1500);
    } catch (e) {
        document.getElementById('reg-error').style.color = "var(--error-color)";
        document.getElementById('reg-error').innerText = e.message;
    }
}

function loginSuccess(isRestore = false) {
    document.getElementById('login-view').classList.add('hidden');
    document.getElementById('app-container').classList.remove('hidden');
    
    document.getElementById('user-info').innerText = `${currentUsername} (${currentRole})`;
    
    // Apply Role Restrictions to UI
    if (currentRole === 'ADMIN') {
        document.getElementById('btn-new-queue').style.display = 'block';
        document.getElementById('btn-gen-token').style.display = 'none'; // Admins don't generate tokens for themselves
        document.getElementById('btn-vip-token').style.display = 'block';
        document.getElementById('dashboard-admin').style.display = 'block';
        document.getElementById('dashboard-user').style.display = 'none';
    } else {
        document.getElementById('btn-new-queue').style.display = 'none';
        document.getElementById('btn-gen-token').style.display = 'block';
        document.getElementById('btn-vip-token').style.display = 'none';
        document.getElementById('dashboard-admin').style.display = 'none';
        document.getElementById('dashboard-user').style.display = 'block';
    }
    
    if (isRestore) {
        const viewId = localStorage.getItem('currentViewId');
        if (viewId === 'view-queues') {
            document.getElementById('nav-queues').click();
        } else if (viewId === 'view-tokens') {
            currentQueueId = localStorage.getItem('currentQueueId');
            currentQueueType = localStorage.getItem('currentQueueType');
            const qName = localStorage.getItem('currentQueueName') || 'Queue';
            document.getElementById('tokens-queue-title').innerText = qName + " - Live Tokens";
            switchToTokensView();
        } else {
            document.getElementById('nav-dashboard').click();
        }
    } else {
        // By default, go to dashboard
        document.getElementById('nav-dashboard').click();
    }
}

window.doLogout = function() {
    authToken = null;
    currentRole = null;
    currentUsername = null;
    currentQueueId = null;
    localStorage.clear();
    if(fetchTokensInterval) clearInterval(fetchTokensInterval);
    document.getElementById('app-container').classList.add('hidden');
    document.getElementById('login-view').classList.remove('hidden');
    switchAuthTab('login');
}

function authHeader() {
    return { 'Authorization': `Bearer ${authToken}`, 'Content-Type': 'application/json' };
}

/* ---------------- NAVIGATION ---------------- */

document.getElementById('nav-dashboard').onclick = (e) => {
    e.preventDefault();
    switchView('view-dashboard', 'nav-dashboard');
    if (fetchTokensInterval) clearInterval(fetchTokensInterval);
    currentQueueId = null;
    if (currentRole === 'ADMIN') {
        fetchAdminDashboard();
    } else {
        fetchUserDashboard();
        fetchTokensInterval = setInterval(fetchUserDashboard, 3000);
    }
};

document.getElementById('nav-queues').onclick = (e) => {
    e.preventDefault();
    switchView('view-queues', 'nav-queues');
    if (fetchTokensInterval) clearInterval(fetchTokensInterval);
    currentQueueId = null;
    fetchQueues();
};

function switchToTokensView() {
    switchView('view-tokens');
    if (fetchTokensInterval) clearInterval(fetchTokensInterval);
    fetchTokensForCurrentQueue();
    fetchTokensInterval = setInterval(fetchTokensForCurrentQueue, 3000);
}

function switchView(viewId, navId) {
    localStorage.setItem('currentViewId', viewId);
    document.querySelectorAll('.view').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.sidebar nav a').forEach(el => el.classList.remove('active'));
    document.getElementById(viewId).classList.add('active');
    if(navId) {
        let navEl = document.getElementById(navId);
        if(navEl) navEl.classList.add('active');
    }
}

/* ---------------- DASHBOARDS ---------------- */

async function fetchAdminDashboard() {
    if(!authToken) return;
    const res = await fetch(`${API_BASE}/analytics/summary`, { headers: authHeader() });
    const data = await res.json();
    
    document.getElementById('stat-queues').innerText = data.totalQueues;
    document.getElementById('stat-waiting').innerText = data.totalTokensWaiting;
    document.getElementById('stat-served').innerText = data.totalTokensCompleted;
    
    const labels = data.queueStats.map(q => q.queueName);
    const volumes = data.queueStats.map(q => q.totalTokens);
    
    if (dashboardChart) dashboardChart.destroy();
    
    const ctx = document.getElementById('analyticsChart').getContext('2d');
    dashboardChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Total Tokens Generated',
                data: volumes,
                backgroundColor: 'rgba(59, 130, 246, 0.5)',
                borderColor: 'rgba(59, 130, 246, 1)',
                borderWidth: 1,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { beginAtZero: true, ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                x: { ticks: { color: '#94a3b8' }, grid: { display: false } }
            },
            plugins: {
                legend: { labels: { color: '#f8fafc' } }
            }
        }
    });
}

async function fetchUserDashboard() {
    if(!authToken) return;
    const res = await fetch(`${API_BASE}/tokens/my-tokens`, { headers: authHeader() });
    const tokens = await res.json();
    
    const list = document.getElementById('my-token-list');
    list.innerHTML = '';
    
    if(tokens.length === 0) {
        list.innerHTML = '<p style="color: var(--text-secondary); padding: 1rem;">You do not have any active tokens. Join a queue to get started!</p>';
        return;
    }
    
    tokens.forEach(t => {
        list.innerHTML += `
            <div class="token-row">
                <div class="token-info">
                    <strong>${t.tokenNumber}</strong>
                    <span>Queue: ${t.serviceQueue.name}</span>
                    <div style="font-size: 0.8rem; color: var(--text-secondary); margin-top: 5px;">
                        Created: ${formatTime(t.createdAt)} 
                        ${t.scheduledTime ? '<br><strong style="color:var(--accent-color);">Appointment: ' + formatTime(t.scheduledTime) + '</strong>' : ''}
                        ${t.status === 'WAITING' && t.expectedTime && !t.scheduledTime ? '| Estimated Turn: ' + formatTime(t.expectedTime) : ''}
                        ${t.activatedAt ? '| Started: ' + formatTime(t.activatedAt) : ''} 
                        ${t.completedAt ? '| Finished: ' + formatTime(t.completedAt) : ''}
                    </div>
                </div>
                <div>
                    <span class="status-badge status-${t.status}" style="margin-right:1rem;">${t.status}</span>
                </div>
            </div>
        `;
    });
}

/* ---------------- QUEUES ---------------- */

async function fetchQueues() {
    if(!authToken) return;
    const res = await fetch(`${API_BASE}/queues`, { headers: authHeader() });
    const queues = await res.json();
    const grid = document.getElementById('queue-grid');
    grid.innerHTML = '';
    queues.forEach(q => {
        const typeLabel = q.type === 'SCHEDULED' ? 'Appointments' : 'Walk-In';
        grid.innerHTML += `
            <div class="glass-panel queue-card">
                <h4>${q.name} <span style="font-size:0.7rem; background:rgba(255,255,255,0.1); padding:2px 6px; border-radius:10px;">${typeLabel}</span></h4>
                <p>${q.description}</p>
                <div class="stats">
                    <span>Avg Time: ${q.averageServiceTimeMinutes}m</span>
                    <button class="btn secondary" style="padding: 0.4rem 0.8rem; font-size: 0.8rem;" onclick="selectQueue('${q.id}', '${q.name}', '${q.type}')">Select</button>
                </div>
            </div>
        `;
    });
}

window.selectQueue = function(id, name, type) {
    currentQueueId = id;
    currentQueueType = type;
    localStorage.setItem('currentQueueId', id);
    localStorage.setItem('currentQueueType', type);
    localStorage.setItem('currentQueueName', name);
    document.getElementById('tokens-queue-title').innerText = name + " - Live Tokens";
    switchToTokensView();
}

window.showCreateQueueModal = function() { document.getElementById('modal-overlay').classList.remove('hidden'); }
window.closeModal = function() { document.getElementById('modal-overlay').classList.add('hidden'); }

window.toggleQueueTypeInputs = function() {
    const type = document.getElementById('queue-type').value;
    if (type === 'SCHEDULED') {
        document.getElementById('live-inputs').style.display = 'none';
        document.getElementById('schedule-inputs').style.display = 'block';
    } else {
        document.getElementById('live-inputs').style.display = 'block';
        document.getElementById('schedule-inputs').style.display = 'none';
    }
}

window.createQueue = async function() {
    const payload = {
        name: document.getElementById('queue-name').value,
        description: document.getElementById('queue-desc').value,
        averageServiceTimeMinutes: parseInt(document.getElementById('queue-time').value) || 5,
        type: document.getElementById('queue-type').value,
        slotDurationMinutes: parseInt(document.getElementById('queue-slot-duration').value),
        slotBufferMinutes: parseInt(document.getElementById('queue-slot-buffer').value),
        operatingHoursStart: document.getElementById('queue-hours-start').value + ':00',
        operatingHoursEnd: document.getElementById('queue-hours-end').value + ':00'
    };
    await fetch(`${API_BASE}/queues`, { method: 'POST', headers: authHeader(), body: JSON.stringify(payload) });
    closeModal();
    fetchQueues();
}

/* ---------------- TOKENS ---------------- */

let liveTokens = [];

async function fetchTokensForCurrentQueue() {
    if(!authToken || !currentQueueId) return;
    const res = await fetch(`${API_BASE}/tokens/queue/${currentQueueId}`, { headers: authHeader() });
    liveTokens = await res.json();
    renderTokens();
}

function formatTime(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

window.generateToken = async function() {
    if(!currentQueueId) return alert("Please select a Queue first from the Service Queues tab");
    
    if (currentQueueType === 'SCHEDULED') {
        document.getElementById('slot-modal').classList.remove('hidden');
        document.getElementById('slot-date').value = new Date().toISOString().split('T')[0];
        fetchSlots();
        return;
    }
    
    await fetch(`${API_BASE}/tokens`, { method: 'POST', headers: authHeader(), body: JSON.stringify({queueId: currentQueueId}) });
    fetchTokensForCurrentQueue();
}

window.closeSlotModal = function() { document.getElementById('slot-modal').classList.add('hidden'); }

window.fetchSlots = async function() {
    const date = document.getElementById('slot-date').value;
    if (!date) return;
    
    const res = await fetch(`${API_BASE}/tokens/queue/${currentQueueId}/slots?date=${date}`, { headers: authHeader() });
    const slots = await res.json();
    
    const grid = document.getElementById('slot-grid');
    grid.innerHTML = '';
    
    if (slots.length === 0) {
        grid.innerHTML = '<p>No available slots for this date.</p>';
        return;
    }
    
    slots.forEach(time => {
        grid.innerHTML += `<button class="btn secondary" style="padding:0.5rem;" onclick="bookSlot('${date}T${time}:00')">${time}</button>`;
    });
}

window.bookSlot = async function(scheduledTime) {
    try {
        const res = await fetch(`${API_BASE}/tokens`, { 
            method: 'POST', headers: authHeader(), 
            body: JSON.stringify({queueId: currentQueueId, scheduledTime: scheduledTime}) 
        });
        if (!res.ok) {
            const txt = await res.text();
            throw new Error(txt);
        }
        closeSlotModal();
        fetchTokensForCurrentQueue();
    } catch (e) {
        alert(e.message);
    }
}

window.generateVipToken = async function() {
    if(!currentQueueId) return alert("Please select a Queue first");
    
    if (currentQueueType === 'SCHEDULED') {
        alert("VIP Tokens cannot be generated for Scheduled Appointments currently.");
        return;
    }
    
    const targetUser = prompt("Enter the username to assign the VIP token to:");
    if (!targetUser) return;
    try {
        const res = await fetch(`${API_BASE}/tokens/vip`, { method: 'POST', headers: authHeader(), body: JSON.stringify({queueId: currentQueueId, username: targetUser}) });
        if (!res.ok) throw new Error("Could not generate VIP Token. Does the user exist?");
        fetchTokensForCurrentQueue();
    } catch (e) {
        alert(e.message);
    }
}

window.delayToken = async function(id) {
    if (!confirm("Are you sure you want to push your token back slightly?")) return;
    await fetch(`${API_BASE}/tokens/${id}/delay`, { method: 'POST', headers: authHeader() });
    fetchTokensForCurrentQueue();
    // also update dashboard if user is there
    if (document.getElementById('view-dashboard').classList.contains('active')) {
        fetchUserDashboard();
    }
}

window.callToken = async function(id) {
    await fetch(`${API_BASE}/tokens/${id}/status?status=ACTIVE`, { method: 'PUT', headers: authHeader() });
    fetchTokensForCurrentQueue();
}

window.completeToken = async function(id) {
    await fetch(`${API_BASE}/tokens/${id}/status?status=COMPLETED`, { method: 'PUT', headers: authHeader() });
    fetchTokensForCurrentQueue();
}

function renderTokens() {
    const list = document.getElementById('token-list');
    list.innerHTML = '';
    
    if(liveTokens.length === 0) {
        list.innerHTML = '<p style="color: var(--text-secondary); padding: 1rem;">No tokens in this queue.</p>';
        return;
    }
    
    // Reverse so newest at bottom, or up to preference. Let's keep normal order (oldest first)
    liveTokens.forEach(t => {
        const isMyToken = t.user.username === currentUsername;
        
        // Build Action Buttons
        let actionButtons = '';
        if (currentRole === 'ADMIN') {
            if (t.status === 'WAITING') {
                actionButtons = `<button class="btn primary" style="padding: 0.4rem 1rem;" onclick="callToken('${t.id}')">Call Up</button>`;
            } else if (t.status === 'ACTIVE') {
                actionButtons = `<button class="btn secondary" style="padding: 0.4rem 1rem;" onclick="completeToken('${t.id}')">Finish</button>`;
            }
        } else if (isMyToken && t.status === 'WAITING') {
            actionButtons = `<button class="btn secondary" style="padding: 0.4rem 1rem;" onclick="delayToken('${t.id}')">Running Late? (Delay)</button>`;
        }
        
        list.innerHTML += `
            <div class="token-row ${isMyToken ? 'highlight-me' : ''}">
                <div class="token-info">
                    <strong>${t.tokenNumber}</strong>
                    <span>User: ${t.user.username}</span>
                    ${isMyToken ? '<span style="margin-left:10px; font-size:0.8rem; color:var(--accent-color);">(This is You)</span>' : ''}
                    <div style="font-size: 0.8rem; color: var(--text-secondary); margin-top: 5px;">
                        Created: ${formatTime(t.createdAt)} 
                        ${t.scheduledTime ? '<br><strong style="color:var(--accent-color);">Appointment: ' + formatTime(t.scheduledTime) + '</strong>' : ''}
                        ${t.status === 'WAITING' && t.expectedTime && !t.scheduledTime ? '| Estimated Turn: ' + formatTime(t.expectedTime) : ''}
                        ${t.activatedAt ? '| Started: ' + formatTime(t.activatedAt) : ''} 
                        ${t.completedAt ? '| Finished: ' + formatTime(t.completedAt) : ''}
                    </div>
                </div>
                <div>
                    <span class="status-badge status-${t.status}" style="margin-right:1rem;">${t.status}</span>
                    ${actionButtons}
                </div>
            </div>
        `;
    });
}

/* --- AI Chatbot Logic --- */
function toggleChat() {
    const chatWidget = document.getElementById('chat-widget');
    const icon = document.getElementById('chat-toggle-icon');
    if (chatWidget.classList.contains('closed')) {
        chatWidget.classList.remove('closed');
        icon.textContent = '▼';
        document.getElementById('chat-input').focus();
    } else {
        chatWidget.classList.add('closed');
        icon.textContent = '▲';
    }
}

function handleChatKeyPress(event) {
    if (event.key === 'Enter') {
        sendChatMessage();
    }
}

async function sendChatMessage() {
    const input = document.getElementById('chat-input');
    const message = input.value.trim();
    if (!message) return;

    appendChatMessage(message, 'user-msg');
    input.value = '';

    const loadingId = appendChatMessage('...', 'ai-msg');

    try {
        const payload = {
            message: message,
            queueId: currentQueueId
        };

        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: authHeader(),
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            const data = await response.json();
            updateChatMessage(loadingId, data.response || "Sorry, I couldn't process that.");
        } else {
            updateChatMessage(loadingId, "Connection error. Please try again later.");
        }
    } catch (e) {
        console.error("Chat Error: ", e);
        updateChatMessage(loadingId, "Something went wrong.");
    }
}

function appendChatMessage(text, className) {
    const chatBody = document.getElementById('chat-body');
    const msgDiv = document.createElement('div');
    msgDiv.className = 'chat-message ' + className;
    msgDiv.innerHTML = text; // allow markdown/bold formatting from the backend
    
    const id = 'msg-' + Date.now() + '-' + Math.floor(Math.random() * 10000);
    msgDiv.id = id;
    
    chatBody.appendChild(msgDiv);
    chatBody.scrollTop = chatBody.scrollHeight;
    return id;
}

function updateChatMessage(id, newText) {
    const msgDiv = document.getElementById(id);
    if (msgDiv) {
        // Basic markdown parser for bold text
        const htmlText = newText.replace(/\*\*(.*?)\*\*/g, '<b>$1</b>');
        msgDiv.innerHTML = htmlText;
    }
}

function restoreSession() {
    const savedToken = localStorage.getItem('authToken');
    if (savedToken) {
        authToken = savedToken;
        currentRole = localStorage.getItem('currentRole');
        currentUsername = localStorage.getItem('currentUsername');
        loginSuccess(true);
    }
}
restoreSession();
document.addEventListener('DOMContentLoaded', restoreSession);

// Intercept fetch to handle expired JWTs gracefully
const originalFetch = window.fetch;
window.fetch = async function() {
    const res = await originalFetch.apply(this, arguments);
    if (res.status === 401 || res.status === 403) {
        const url = arguments[0];
        if (typeof url === 'string' && !url.includes('/auth/')) {
            doLogout();
            alert("Your session has expired. Please log in again.");
        }
    }
    return res;
};
