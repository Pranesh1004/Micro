// arfoms-ui.js

let allFlights = [];
let sortState = { key: null, dir: 1 };
const NUMERIC_KEYS = ['economyFare', 'businessFare', 'firstFare'];

const FALLBACK_AIRPORTS = [
  { code: 'DEL', city: 'Delhi' }, { code: 'BOM', city: 'Mumbai' },
  { code: 'BLR', city: 'Bengaluru' }, { code: 'MAA', city: 'Chennai' },
  { code: 'CCU', city: 'Kolkata' }, { code: 'HYD', city: 'Hyderabad' }
];
const FALLBACK_STATUSES = ['SCHEDULED', 'BOARDING', 'DEPARTED', 'ARRIVED', 'CANCELLED'];

// FIX: Explicitly expose switchPage to the window so HTML onclick can call it
window.switchPage = function(pageId) {
  document.querySelectorAll('.app-page').forEach(p => p.classList.remove('active-page'));
  document.getElementById(pageId).classList.add('active-page');
  ['details', 'add', 'update', 'remove'].forEach(n => document.getElementById('nav-' + n).classList.remove('active'));
  
  const map = { 
    'page-details': 'nav-details', 
    'page-add-flight': 'nav-add', 
    'page-update-flight': 'nav-update', 
    'page-remove-flight': 'nav-remove' 
  };
  
  const targetNav = document.getElementById(map[pageId]);
  if (targetNav) targetNav.classList.add('active');
  
  if (pageId === 'page-add-flight') {
    applyAddFlightDateLimits();
  }
};

// FIX: Explicitly expose sortBy to the window scope
window.sortBy = function(key) {
  if (sortState.key === key) {
    sortState.dir *= -1;
  } else {
    sortState.key = key;
    sortState.dir = 1;
  }
  renderTable();
};

// FIX: Explicitly expose clearSearch to the window scope
window.clearSearch = function() {
  document.getElementById('search-input').value = '';
  renderTable();
};

// FIX: Explicitly expose renderTable globally so search inputs can access it
window.renderTable = function() {
  const rows = getFilteredSorted();
  const body = document.getElementById('flights-table-body');
  
  if (!body) return;
  
  if (!Array.isArray(allFlights) || allFlights.length === 0) {
    body.innerHTML = `<tr><td colspan="9" class="text-center text-muted">No flights yet. Add one to get started.</td></tr>`;
    updateResultCount(0);
    return;
  }
  
  if (rows.length === 0) {
    body.innerHTML = `<tr><td colspan="9" class="text-center text-muted">No flights match your search.</td></tr>`;
  } else {
    body.innerHTML = rows.map(f => `
      <tr>
        <td style="color:#3D7AE6; font-weight:700;">${f.flightNumber}</td>
        <td style="font-weight:500;">${f.flightName || '-'}</td>
        <td style="font-weight:500;">${f.origin} &rarr; ${f.destination}</td>
        <td>${f.departureTime || '-'}</td>
        <td>${f.arrivalTime || '-'}</td>
        <td>${money(f.economyFare)}</td>
        <td>${money(f.businessFare)}</td>
        <td>${money(f.firstFare)}</td>
        <td>${statusBadge(f.flightStatus)}</td>
      </tr>`).join('');
  }
  updateResultCount(rows.length);
  updateSortIndicators();
};

function showAlert(message, type) {
  const box = document.getElementById('alert-box');
  if (!box) return;
  box.className = 'alert alert-' + (type || 'info');
  box.textContent = message;
  box.classList.remove('d-none');
  setTimeout(() => box.classList.add('d-none'), 4000);
}

function statusBadge(status) {
  const map = { SCHEDULED: 'bg-success', BOARDING: 'bg-info', DEPARTED: 'bg-primary', ARRIVED: 'bg-secondary', CANCELLED: 'bg-danger' };
  return `<span class="badge ${map[status] || 'bg-secondary'}">${status || 'UNKNOWN'}</span>`;
}

function money(value) {
  return value == null ? '-' : Number(value).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fillAirportDropdowns(airports) {
  const opts = '<option value="">-- Select City --</option>' +
    airports.map(a => `<option value="${a.code}">${a.city} (${a.code})</option>`).join('');
  
  const addOrigin = document.getElementById('add-origin');
  const addDest = document.getElementById('add-destination');
  if (addOrigin) addOrigin.innerHTML = opts;
  if (addDest) addDest.innerHTML = opts;
}

function fillStatusDropdown(statuses) {
  const statusSelect = document.getElementById('schedule-status-select');
  if (statusSelect) {
    statusSelect.innerHTML = statuses.map(s => `<option value="${s}">${s}</option>`).join('');
  }
}

async function loadMetadata() {
  fillAirportDropdowns(FALLBACK_AIRPORTS);
  fillStatusDropdown(FALLBACK_STATUSES);

  try {
    const res = await apiFetch(window.API + '/metadata', {
        headers: { 'Authorization': window.AUTH_HEADER_VALUE }
    });
    if (!res.ok) throw new Error('metadata status ' + res.status);
    const data = await res.json();
    if (data && Array.isArray(data.airports) && data.airports.length) {
      fillAirportDropdowns(data.airports);
    }
    if (data && Array.isArray(data.statuses) && data.statuses.length) {
      fillStatusDropdown(data.statuses);
    }
  } catch (err) {
    console.warn('Using fallback configuration metrics:', err.message);
  }
}

function istNowForInput() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false
  }).formatToParts(new Date());
  const get = t => parts.find(p => p.type === t).value;
  let hour = get('hour');
  if (hour === '24') hour = '00'; 
  return `${get('year')}-${get('month')}-${get('day')}T${hour}:${get('minute')}`;
}

function applyAddFlightDateLimits() {
  const nowIst = istNowForInput();
  const dep = document.querySelector('#add-flight-form [name="departureTime"]');
  const arr = document.querySelector('#add-flight-form [name="arrivalTime"]');
  if (dep) dep.min = nowIst;
  if (arr) arr.min = nowIst;
  if (dep && arr && !dep.dataset.boundMin) {
    dep.dataset.boundMin = '1'; 
    dep.addEventListener('change', () => {
      arr.min = dep.value || nowIst;
      if (arr.value && arr.value < arr.min) arr.value = '';
    });
  }
}

function getFilteredSorted() {
  const term = (document.getElementById('search-input').value || '').trim().toLowerCase();
  if (!Array.isArray(allFlights)) return [];

  let rows = allFlights.filter(f => {
    if (!term) return true;
    return (f.flightNumber || '').toLowerCase().includes(term)
        || (f.flightName || '').toLowerCase().includes(term)
        || (f.origin || '').toLowerCase().includes(term)
        || (f.destination || '').toLowerCase().includes(term);
  });
  if (sortState.key) {
    const k = sortState.key;
    rows = rows.slice().sort((a, b) => {
      if (NUMERIC_KEYS.includes(k)) {
        const av = a[k] == null ? -Infinity : Number(a[k]);
        const bv = b[k] == null ? -Infinity : Number(b[k]);
        return (av - bv) * sortState.dir;
      }
      const av = (a[k] == null ? '' : String(a[k])).toLowerCase();
      const bv = (b[k] == null ? '' : String(b[k])).toLowerCase();
      return av.localeCompare(bv) * sortState.dir;
    });
  }
  return rows;
}

function updateSortIndicators() {
  document.querySelectorAll('.sort-ind').forEach(el => {
    el.textContent = (el.dataset.key === sortState.key) ? (sortState.dir === 1 ? ' \u25B2' : ' \u25BC') : '';
  });
}

function updateResultCount(shown) {
  const total = Array.isArray(allFlights) ? allFlights.length : 0;
  const el = document.getElementById('result-count');
  if (!el) return;
  if (total === 0) {
    el.textContent = '';
  } else if (shown === total) {
    el.textContent = total + (total === 1 ? ' flight' : ' flights');
  } else {
    el.textContent = 'Showing ' + shown + ' of ' + total;
  }
}

async function loadFlights() {
  try {
    const res = await apiFetch(window.API, {
        headers: { 'Authorization': window.AUTH_HEADER_VALUE }
    });
    if (!res.ok) throw new Error('HTTP status ' + res.status);
    const data = await res.json();
    
    if (data && typeof data === 'object' && Array.isArray(data.flights)) {
      allFlights = data.flights;
    } else if (Array.isArray(data)) {
      allFlights = data;
    } else {
      allFlights = [];
    }
    
    window.renderTable();

    const options = '<option value="">-- Select Flight --</option>' +
      allFlights.map(f => `<option value="${f.flightNumber}">${f.flightNumber}</option>`).join('');
      
    ['schedule-flight-select', 'fares-flight-select', 'delete-flight-select'].forEach(id => {
      const selectElement = document.getElementById(id);
      if (selectElement) selectElement.innerHTML = options;
    });
  } catch (err) {
    allFlights = [];
    window.renderTable();
    showAlert('Failed to load data from backend server: ' + err.message, 'danger');
  }
}

function rowLetter(index) {
  let label = '';
  let n = index;
  do {
    label = String.fromCharCode(65 + (n % 26)) + label;
    n = Math.floor(n / 26) - 1;
  } while (n >= 0);
  return label;
}

function renderSeatPreview() {
  const rowsInput = document.getElementById('seat-rows');
  const colsInput = document.getElementById('seat-columns');
  if (!rowsInput || !colsInput) return;

  const rows = parseInt(rowsInput.value || '0', 10);
  const cols = parseInt(colsInput.value || '0', 10);
  const seatCountInput = document.getElementById('seat-count');
  const aisleInput = document.getElementById('seat-aisle');
  const preview = document.getElementById('seat-layout-preview');

  if (!rows || !cols || rows < 1 || cols < 1) {
    if (preview) {
      preview.classList.add('text-muted');
      preview.innerHTML = 'Enter rows and columns to preview the seat layout.';
    }
    return;
  }

  if (seatCountInput) seatCountInput.value = rows * cols;

  let aisleAfter = aisleInput ? parseInt(aisleInput.value || '0', 10) : 0;
  if (!aisleAfter || aisleAfter < 1) {
    aisleAfter = Math.floor(cols / 2);
  }
  aisleAfter = Math.min(Math.max(aisleAfter, 1), Math.max(cols - 1, 1));

  if (!preview) return;

  if (rows * cols > 400) {
    preview.classList.add('text-muted');
    preview.innerHTML = `Layout too large to preview (${rows} × ${cols} = ${rows * cols} seats).`;
    return;
  }

  preview.classList.remove('text-muted');
  const windowBg = 'linear-gradient(135deg,#0f766e,#10b981)';
  const aisleBg = 'linear-gradient(135deg,#3D7AE6,#1F3763)';

  let html = '<div style="display:inline-block;">';
  for (let r = 0; r < rows; r++) {
    html += '<div style="display:flex; gap:6px; margin-bottom:6px; align-items:center;">';
    html += `<span style="width:26px; font-weight:600; color:#64748b; font-size:.8rem;">${rowLetter(r)}</span>`;
    for (let c = 1; c <= cols; c++) {
      const isWindow = (c === 1 || c === cols);
      const bg = isWindow ? windowBg : aisleBg;
      html += `<span title="${isWindow ? 'Window' : 'Aisle'} seat" style="display:inline-flex; align-items:center; justify-content:center; min-width:42px; padding:6px 8px; border-radius:8px; background:${bg}; color:#fff; font-size:.78rem; font-weight:600;">${rowLetter(r)}${c}</span>`;
      if (c === aisleAfter && c < cols) {
        html += `<span title="Walkway" style="display:inline-block; width:22px; align-self:stretch; border-left:2px dashed #94a3b8; border-right:2px dashed #94a3b8; border-radius:4px; background:repeating-linear-gradient(45deg,#f1f5f9,#f1f5f9 4px,#e2e8f0 4px,#e2e8f0 8px);"></span>`;
      }
    }
    html += '</div>';
  }
  html += '</div>';
  html += `<div class="small text-muted mt-2">${rows} row(s) × ${cols} column(s) = ${rows * cols} seats · walkway after column ${aisleAfter} · window seats auto-highlighted (green)</div>`;
  preview.innerHTML = html;
}

// Bind Element Form Actions
document.addEventListener('DOMContentLoaded', async () => {
  ['seat-rows', 'seat-columns', 'seat-aisle'].forEach(id => {
    const inputEl = document.getElementById(id);
    if (inputEl) inputEl.addEventListener('input', renderSeatPreview);
  });
  renderSeatPreview();
  applyAddFlightDateLimits();
  
  await loadMetadata();
  await loadFlights();

  const addForm = document.getElementById('add-flight-form');
  if (addForm) {
    addForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const form = e.target;
      const data = Object.fromEntries(new FormData(form).entries());
      if (data.origin && data.origin === data.destination) {
        document.getElementById('route-error').style.display = 'block';
        return;
      }
      const routeErr = document.getElementById('route-error');
      if (routeErr) routeErr.style.display = 'none';

      const nowIst = istNowForInput();
      if (data.departureTime && data.departureTime < nowIst) {
        showAlert('Departure date/time cannot be in the past (IST).', 'danger');
        return;
      }
      if (data.arrivalTime && data.arrivalTime < nowIst) {
        showAlert('Arrival date/time cannot be in the past (IST).', 'danger');
        return;
      }
      if (data.departureTime && data.arrivalTime && data.arrivalTime <= data.departureTime) {
        showAlert('Arrival must be after departure.', 'danger');
        return;
      }

      const cols = data.seatColumns ? parseInt(data.seatColumns, 10) : null;
      let aisleAfter = data.seatAisleAfter ? parseInt(data.seatAisleAfter, 10) : null;
      if ((!aisleAfter || aisleAfter < 1) && cols) {
        aisleAfter = Math.floor(cols / 2);
      }
      const payload = {
        flightNumber: data.flightNumber,
        flightName: data.flightName,
        origin: data.origin,
        destination: data.destination,
        departureTime: data.departureTime,
        arrivalTime: data.arrivalTime,
        economyFare: parseFloat(data.economyFare),
        businessFare: parseFloat(data.premiumFare),
        firstFare: parseFloat(data.firstFare),
        seatCount: data.seatCount ? parseInt(data.seatCount, 10) : null,
        seatRows: data.seatRows ? parseInt(data.seatRows, 10) : null,
        seatColumns: cols,
        seatAisleAfter: aisleAfter
      };
      try {
        await window.sendRequest(window.API, 'POST', payload);
        showAlert('Flight ' + data.flightNumber + ' added successfully.', 'success');
        form.reset();
        renderSeatPreview();
        await loadFlights();
        window.switchPage('page-details');
      } catch (err) { showAlert(err.message, 'danger'); }
    });
  }

  const updateForm = document.getElementById('update-schedule-form');
  if (updateForm) {
    updateForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const data = Object.fromEntries(new FormData(e.target).entries());
      const payload = {
        departureTime: data.departureTime || null,
        arrivalTime: data.arrivalTime || null,
        flightStatus: data.flightStatus || null
      };
      try {
        const flightNum = data.flightNumber;
        const flightObj = allFlights.find(f => f.flightNumber === flightNum);
        const flightId = flightObj ? flightObj.flightId : flightNum;
        await window.sendRequest(window.API + '/' + encodeURIComponent(flightId) + '/schedule', 'PATCH', payload);
        showAlert('Schedule updated for ' + data.flightNumber + '.', 'success');
        e.target.reset();
        await loadFlights();
      } catch (err) { showAlert(err.message, 'danger'); }
    });
  }

  const fareForm = document.getElementById('set-fares-form');
  if (fareForm) {
    fareForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const data = Object.fromEntries(new FormData(e.target).entries());
      const payload = {
        economyFare: parseFloat(data.economyFare),
        businessFare: parseFloat(data.premiumFare),
        firstFare: parseFloat(data.firstFare)
      };
      try {
        const flightNum = data.flightNumber;
        const flightObj = allFlights.find(f => f.flightNumber === flightNum);
        const flightId = flightObj ? flightObj.flightId : flightNum;
        await window.sendRequest(window.API + '/' + encodeURIComponent(flightId) + '/fare-class', 'PATCH', payload);
        showAlert('Fares updated for ' + data.flightNumber + '.', 'success');
        e.target.reset();
        await loadFlights();
      } catch (err) { showAlert(err.message, 'danger'); }
    });
  }

  const deleteForm = document.getElementById('delete-flight-form');
  if (deleteForm) {
    deleteForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const data = Object.fromEntries(new FormData(e.target).entries());
      if (!confirm('Confirm complete removal of ' + data.flightNumber + '?')) return;
      try {
        const flightNum = data.flightNumber;
        const flightObj = allFlights.find(f => f.flightNumber === flightNum);
        const flightId = flightObj ? flightObj.flightId : flightNum;
        await window.sendRequest(window.API + '/' + encodeURIComponent(flightId), 'DELETE');
        showAlert('Flight ' + data.flightNumber + ' removed.', 'success');
        e.target.reset();
        await loadFlights();
      } catch (err) { showAlert(err.message, 'danger'); }
    });
  }
});