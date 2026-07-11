// crew_roster_management.js
const BACKEND_API_BASE_URL = 'http://localhost:8210/api/crew';
const AUTH_USERNAME = 'scheduler';
const AUTH_PASSWORD = 'scheduler123';
const AUTH_HEADER_VALUE = 'Basic ' + btoa(`${AUTH_USERNAME}:${AUTH_PASSWORD}`);

document.addEventListener('DOMContentLoaded', () => {
    const assignmentForm = document.getElementById('crewAssignmentForm');
    if (assignmentForm) {
        assignmentForm.addEventListener('submit', handleCrewAssignmentSubmit);
    }
    
    const fetchRosterBtn = document.getElementById('btnFetchRoster');
    if (fetchRosterBtn) {
        fetchRosterBtn.addEventListener('click', fetchCrewRosterByName);
    }
    
    const backToPortalBtn = document.getElementById('btnBackToPortal');
    if (backToPortalBtn) {
        backToPortalBtn.addEventListener('click', returnToPortalRoot);
    }
});

/**
 * Endpoint action handler: Assigns Crew Member configuration profile
 */
async function handleCrewAssignmentSubmit(event) {
    event.preventDefault();
    
    const rawRole = document.getElementById('assignedRole').value;
    // FIX: Normalize space characters to underscores to avoid backend Enum string parsing crashes
    const validEnumRole = rawRole ? rawRole.trim().toUpperCase().replace(/\s+/g, '_') : "";
    
    const payload = {
        crewMemberName: document.getElementById('crewMemberName').value.trim(),
        flightId: parseInt(document.getElementById('flightId').value, 10),
        role: validEnumRole,
        dutyHours: parseFloat(document.getElementById('assignedDutyHours').value)
    };

    try {
        const response = await apiFetch(`${BACKEND_API_BASE_URL}/assign`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': AUTH_HEADER_VALUE
            },
            body: JSON.stringify(payload)
        });

        if (response.status === 201 || response.ok) {
            alert('Transaction Successful: Crew assignment updated in system repository.');
            document.getElementById('crewAssignmentForm').reset();
        } else {
            const errorMessage = await response.text();
            alert(`Application Rejection: ${errorMessage || 'Invalid structural entity submission.'}`);
        }
    } catch (error) {
        console.error('Network Transaction Exception: ', error);
        alert('Critical Communications Fault: Unable to contact system backend REST container.');
    }
}

/**
 * Scheduler View: Looks up operational roster ledger items matching text query strings
 */
async function fetchCrewRosterByName() {
    const searchTarget = document.getElementById('searchCrewName').value.trim();
    if (!searchTarget) {
        alert('Validation Guard: Query parameter [Crew Member Name] must not be blank.');
        return;
    }

    try {
        const response = await apiFetch(`${BACKEND_API_BASE_URL}/roster/${encodeURIComponent(searchTarget)}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'Authorization': AUTH_HEADER_VALUE
            }
        });

        if (!response.ok) {
            throw new Error(`Server returned structural status response: ${response.status}`);
        }

        const rawList = await response.json();
        const totalRecordsList = Array.isArray(rawList) ? rawList : [];
        renderRosterGridTable(totalRecordsList);
    } catch (error) {
        console.error('Data Processing Exception:', error);
        alert('Database Query Refusal: Error extracting historical logs for specified user data.');
    }
}

/**
 * Dynamic template builder: Renders enriched scheduler rows with transactional interactive nodes
 */
function renderRosterGridTable(totalRecordsList) {
    const tableBody = document.querySelector('#crewRosterTable tbody');
    const metricsPanel = document.getElementById('dutyMetricsPanel');
    
    if (!tableBody) return;
    tableBody.innerHTML = '';

    if (totalRecordsList.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="10" class="no-records">No operational ledger entities registered to match query.</td></tr>';
        if (metricsPanel) metricsPanel.style.display = 'none';
        return;
    }

    let calculatedCumulativeHours = 0;
    const trackedRecordCount = totalRecordsList.length;

    totalRecordsList.forEach(record => {
        calculatedCumulativeHours += (record.dutyHours || 0);

        let swapControlTemplate = '';
        let hoursControlTemplate = '';
        
        if (record.assignmentStatus !== 'SWAPPED' && record.assignmentStatus !== 'COMPLETED') {
            swapControlTemplate = `
                <div style="display:flex; gap:2px;">
                    <input type="text" id="targetReplacementName-${record.assignmentId}" placeholder="New Name" class="inline-input" style="width:90px; font-size:11px;">
                    <button type="button" class="btn btn-action" onclick="executeCrewSwapTransaction(${record.assignmentId})" style="padding:2px 4px; font-size:11px;">Swap</button>
                </div>`;
            hoursControlTemplate = `
                <div style="display:flex; gap:2px;">
                    <input type="number" step="0.1" id="logDutyHours-${record.assignmentId}" placeholder="Hours" class="inline-input" style="width:90px; font-size:11px;">
                    <button type="button" class="btn btn-success" onclick="executeLogHoursTransaction(${record.assignmentId})" style="padding:2px 4px; font-size:11px; background-color:#10b981; color:white; border:none; border-radius:3px;">Complete</button>
                </div>`;
        } else {
            swapControlTemplate = '<span class="terminal-label" style="color:#6b7280; font-style:italic;">Archival</span>';
            hoursControlTemplate = '<span class="terminal-label" style="color:#6b7280; font-style:italic;">Archival</span>';
        }

        const HTMLGridRow = `
            <tr>
                <td><strong>#${record.assignmentId}</strong></td>
                <td>FL-${record.flightId || 'N/A'}</td>
                <td><span class="role-badge">${record.role}</span></td>
                <td>${record.dutyHours ? record.dutyHours.toFixed(1) : '0.0'} hrs</td>
                <td><span class="status-indicator status-${record.assignmentStatus.toLowerCase()}">${record.assignmentStatus}</span></td>
                <td>${swapControlTemplate}</td>
                <td>${hoursControlTemplate}</td>
            </tr>`;
            
        tableBody.insertAdjacentHTML('beforeend', HTMLGridRow);
    });

    if (document.getElementById('metricTotalFlights')) {
        document.getElementById('metricTotalFlights').innerText = trackedRecordCount;
    }
    if (document.getElementById('metricTotalHours')) {
        document.getElementById('metricTotalHours').innerText = calculatedCumulativeHours.toFixed(1);
    }
    if (metricsPanel) metricsPanel.style.display = 'flex';
}

/**
 * Transaction action execution context: Swaps scheduled employee configurations
 */
async function executeCrewSwapTransaction(targetAssignmentId) {
    const inputNode = document.getElementById(`targetReplacementName-${targetAssignmentId}`);
    if (!inputNode) return;
    
    const verifiedReplacementName = inputNode.value.trim();
    if (!verifiedReplacementName) {
        alert('Validation Guard: Swapped assignee identity structure requirement missing.');
        return;
    }

    try {
        const response = await apiFetch(`${BACKEND_API_BASE_URL}/${targetAssignmentId}/swap`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': AUTH_HEADER_VALUE
            },
            body: JSON.stringify({ crewMemberName: verifiedReplacementName })
        });

        if (response.ok) {
            alert('Transaction Confirmed: Context substitution executed cleanly. View updated.');
            fetchCrewRosterByName();
        } else {
            alert('Business Engine Violation: Selected record swap context rejected by backend criteria validations.');
        }
    } catch (error) {
        console.error('Critical Execution Failure: ', error);
        alert('Server Transaction Crash: Connection lost during modification sequence.');
    }
}

/**
 * Transaction action execution context: Records actual hours and transitions state to COMPLETED
 */
async function executeLogHoursTransaction(targetAssignmentId) {
    const inputNode = document.getElementById(`logDutyHours-${targetAssignmentId}`);
    if (!inputNode) return;

    const hoursValue = parseFloat(inputNode.value);
    if (isNaN(hoursValue) || hoursValue < 0) {
        alert('Validation Guard: Please input a valid non-negative value for flown duty hours.');
        return;
    }

    try {
        const response = await apiFetch(`${BACKEND_API_BASE_URL}/${targetAssignmentId}/duty-hours`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': AUTH_HEADER_VALUE
            },
            body: JSON.stringify({ dutyHours: hoursValue })
        });

        if (response.ok) {
            alert('Flight Segment Logged: Duty limits logged and status successfully shifted to COMPLETED.');
            fetchCrewRosterByName();
        } else {
            alert('Business Engine Violation: Failed to process duty logging metrics.');
        }
    } catch (error) {
        console.error('Critical Execution Failure: ', error);
        alert('Server Transaction Crash: Connection lost during historical save process.');
    }
}

/**
 * Portal navigation engine state toggler
 */
function switchState(targetView) {
    document.getElementById('schedulerLoginForm').style.display = 'none';
    document.getElementById('crewLoginForm').style.display = 'none';

    if (targetView === 'scheduler-login') {
        document.getElementById('schedulerLoginForm').style.display = 'block';
    } else if (targetView === 'crew-login') {
        document.getElementById('crewLoginForm').style.display = 'block';
    }
}

/**
 * Verification handler checking portal gate access profiles
 */
function verifySchedulerLogin() {
    const password = document.getElementById('schedulerPassword').value;
    if (password === 'admin123' || password === 'admin') {
        document.getElementById('gatewayPortal').style.display = 'none';
        document.getElementById('schedulerDashboard').style.display = 'block';
        document.getElementById('btnBackToPortal').style.display = 'block';
        document.getElementById('schedulerPassword').value = '';
    } else {
        alert('Security Access Failure: Invalid Administrative Key Credentials.');
    }
}

/**
 * Crew self-service terminal lookup pipeline
 */
async function executeCrewSelfServiceLookup() {
    const crewName = document.getElementById('portalCrewSearchName').value.trim();
    if (!crewName) {
        alert('Input Requirement: Please fill out your crew identity string.');
        return;
    }

    try {
        const response = await apiFetch(`${BACKEND_API_BASE_URL}/roster/${encodeURIComponent(crewName)}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'Authorization': AUTH_HEADER_VALUE
            }
        });

        if (!response.ok) throw new Error('Data extract refusal.');
        const rawList = await response.json();
        const combinedList = Array.isArray(rawList) ? rawList : [];
        
        const container = document.getElementById('crewSelfServiceContainer');
        if (!container) return;
        container.innerHTML = '';

        if (combinedList.length === 0) {
            container.innerHTML = `<p class="no-records">No active operational flight patterns registered for: "${crewName}"</p>`;
        } else {
            const primaryTrack = combinedList[combinedList.length - 1];
            let cumulativeHours = 0;
            combinedList.forEach(rec => cumulativeHours += (rec.dutyHours || 0));

            // Dynamically fetch Flight Details from Flight Service via Gateway
            let flightData = {};
            if (primaryTrack.flightId) {
                try {
                    const flightRes = await apiFetch(`http://localhost:8210/api/flights/${primaryTrack.flightId}`, {
                        headers: { 'Authorization': AUTH_HEADER_VALUE }
                    });
                    if (flightRes.ok) {
                        flightData = await flightRes.json();
                    }
                } catch (e) {
                    console.warn('Failed to fetch flight details for crew mapping:', e);
                }
            }

            const fNo = flightData.flightNumber || 'N/A';
            const routeStr = (flightData.origin && flightData.destination) ? `${flightData.origin} to ${flightData.destination}` : 'N/A';
            const timingStr = (flightData.departureTime && flightData.arrivalTime) ? `${flightData.departureTime} - ${flightData.arrivalTime}` : 'N/A';

            container.innerHTML = `
                <div class="crew-display-card animate-fade-in" style="background:#fff; padding:20px; border-radius:8px; border:1px solid #e2e8f0; margin-bottom:20px;">
                    <h2 style="margin-top:0; color: #1F3763;">Welcome back, ${primaryTrack.crewMemberName}</h2>
                    <div class="crew-meta-item" style="margin-bottom:8px;"><strong>Assigned Duty Status:</strong> <span class="status-indicator status-${primaryTrack.assignmentStatus.toLowerCase()}">${primaryTrack.assignmentStatus}</span></div>
                    <div class="crew-meta-item" style="margin-bottom:8px;"><strong>Current Assigned Sector:</strong> <span class="role-badge" style="background:#0a2540; color:white; padding:2px 6px; border-radius:4px;">${fNo} (${routeStr})</span></div>
                    <div class="crew-meta-item" style="margin-bottom:8px;"><strong>Flight Schedule Window:</strong> <span><small>${timingStr}</small></span></div>
                    <div class="crew-meta-item" style="margin-bottom:8px;"><strong>Operational Role:</strong> <span>${primaryTrack.role}</span></div>
                    <div class="crew-meta-item" style="margin-bottom:12px;"><strong>Current Leg Hours:</strong> <span>${primaryTrack.dutyHours ? primaryTrack.dutyHours.toFixed(1) : '0.0'} hrs</span></div>
                    <div class="crew-meta-item" style="border-top:2px solid #f1f5f9; margin-top:10px; padding-top:15px;">
                        <strong>Cumulative Total Logged Hours:</strong>
                        <span style="font-size:18px; font-weight:bold; color:#3D7AE6;">${cumulativeHours.toFixed(1)} hrs</span>
                    </div>
                </div>`;
        }

        await fetchAndRenderActiveDashboardTable(crewName);
        document.getElementById('gatewayPortal').style.display = 'none';
        document.getElementById('crewMemberDashboard').style.display = 'block';
        document.getElementById('btnBackToPortal').style.display = 'block';
    } catch (err) {
        console.error(err);
        alert('Network Transaction Exception: Could not pull your individual roster profile from repository container.');
    }
}

/**
 * Self service sub-grid visualization loader
 */
async function fetchAndRenderActiveDashboardTable(crewName) {
    const dashboardTableBody = document.getElementById('crewActiveDashboardData');
    if (!dashboardTableBody) return;
    dashboardTableBody.innerHTML = '';

    try {
        const response = await apiFetch(`${BACKEND_API_BASE_URL}/roster/${encodeURIComponent(crewName)}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'Authorization': AUTH_HEADER_VALUE
            }
        });

        if (!response.ok) throw new Error('Dashboard endpoint failure');
        const rawList = await response.json();
        const allAssignments = Array.isArray(rawList) ? rawList : [];
        const activeAssignments = allAssignments.filter(a => a.assignmentStatus !== 'COMPLETED' && a.assignmentStatus !== 'SWAPPED');

        if (!activeAssignments || activeAssignments.length === 0) {
            dashboardTableBody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: #888; font-style: italic; padding: 20px;">No upcoming scheduled flights are on your radar right now.</td></tr>';
            return;
        }

        activeAssignments.forEach(record => {
            const rowHTML = `
                <tr>
                    <td><strong>#${record.assignmentId}</strong></td>
                    <td><span class="role-badge">${record.role}</span></td>
                    <td>${record.dutyHours ? record.dutyHours.toFixed(1) : '0.0'} hrs</td>
                    <td><span class="status-indicator status-${record.assignmentStatus.toLowerCase()}">${record.assignmentStatus}</span></td>
                    <td>FL-${record.flightId || 'N/A'}</td>
                </tr>`;
            dashboardTableBody.insertAdjacentHTML('beforeend', rowHTML);
        });
    } catch (error) {
        console.error('Dashboard pipeline processing error:', error);
        dashboardTableBody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: #ef4444; font-weight: bold; padding: 20px;">Failed to cleanly retrieve active schedule assignments from repository dataset.</td></tr>';
    }
}

/**
 * Returns interface view context elements back to base terminal root
 */
function returnToPortalRoot() {
    document.getElementById('schedulerDashboard').style.display = 'none';
    document.getElementById('crewMemberDashboard').style.display = 'none';
    document.getElementById('btnBackToPortal').style.display = 'none';
    document.getElementById('gatewayPortal').style.display = 'block';
    document.getElementById('schedulerLoginForm').style.display = 'none';
    document.getElementById('crewLoginForm').style.display = 'none';
    document.getElementById('portalCrewSearchName').value = '';
}