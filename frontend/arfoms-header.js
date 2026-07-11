// arfoms-header.js

// 1. Shared Global Constants
window.API = 'http://localhost:8210/api/flights';
window.AUTH_HEADER_VALUE = sessionStorage.getItem('authHeader') || ('Basic ' + btoa('flightscheduler:flightscheduler'));

// 2. Shared Network Communication Engine attached to the global window scope
window.sendRequest = async function(url, method, bodyObj) {
  const options = { 
    method, 
    headers: { 
      'Content-Type': 'application/json'
    } 
  };
  if (bodyObj) options.body = JSON.stringify(bodyObj);
  
  // Use the new global apiFetch wrapper from auth.js so the JWT token is injected!
  const res = await window.apiFetch(url, options);
  
  if (!res.ok) {
    let message = 'Request failed (' + res.status + ')';
    try { 
      const err = await res.json(); 
      if (err.message) message = err.message; 
    } catch (e) {}
    throw new Error(message);
  }
  return res.status === 204 ? null : res.json();
};

console.log("ARFOMS Core configuration loaded successfully.");