import { initializeApp } from "https://www.gstatic.com/firebasejs/9.22.1/firebase-app.js";
import {
  getAuth,
  onAuthStateChanged,
  signOut,
  signInWithEmailAndPassword,
} from "https://www.gstatic.com/firebasejs/9.22.1/firebase-auth.js";
import {
  getDatabase,
  ref,
  set,
  remove,
  onValue,
} from "https://www.gstatic.com/firebasejs/9.22.1/firebase-database.js";

// Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyCknADtCHfXDClWl3I3YQKZtn5m2ppf70o",
  authDomain: "buscityapp.firebaseapp.com",
  databaseURL: "https://buscityapp-default-rtdb.firebaseio.com",
  projectId: "buscityapp",
  storageBucket: "buscityapp.firebasestorage.app",
  messagingSenderId: "80584192899",
  appId: "1:80584192899:android:005fc34c2bc1c090823cb6",
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const database = getDatabase(app);

// DOM elements
const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const loginBtn = document.getElementById("login-btn");
const loginForm = document.getElementById("login-form");
const authError = document.getElementById("auth-error");
const authHero = document.getElementById("auth-hero");
const returnsSection = document.getElementById("returns-section");
const userArea = document.getElementById("user-area");
const userEmail = document.getElementById("user-email");
const signoutBtn = document.getElementById("signout-btn");

// Returns form elements
const returnsForm = document.getElementById("returns-form");
const returnIdInput = document.getElementById("return-id");
const routeNumberInput = document.getElementById("route-number");
const stopNameInput = document.getElementById("stop-name");
const stopOrderInput = document.getElementById("stop-order");
const latitudeInput = document.getElementById("latitude");
const longitudeInput = document.getElementById("longitude");
const cancelBtn = document.getElementById("cancel-btn");
const returnsList = document.getElementById("returns-list");
const importBtn = document.getElementById("import-btn");
const searchInput = document.getElementById("search-stop");

let returnsData = {};

// Helpers
function safeString(v) {
  return v === undefined || v === null ? "" : String(v);
}

function safeNumber(v) {
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

// Login logic
loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    await signInWithEmailAndPassword(
      auth,
      emailInput.value,
      passwordInput.value
    );
  } catch (error) {
    authError.textContent =
      error.code === "auth/invalid-login-credentials"
        ? "Email hoặc mật khẩu không chính xác"
        : error.message;
    authError.classList.remove("hidden");
  }
});

// Sign out logic
signoutBtn.addEventListener("click", () => {
  signOut(auth);
});

// Auth state changes
onAuthStateChanged(auth, (user) => {
  if (user) {
    // Check if user is admin
    const adminCheckRef = ref(database, `admins/${user.uid}`);
    onValue(adminCheckRef, (snap) => {
      const isAdmin = snap.exists() && !!snap.val();

      if (!isAdmin) {
        authError.textContent = `Tài khoản này không có quyền quản trị. UID: ${user.uid}`;
        authError.classList.remove("hidden");
        signOut(auth);
        return;
      }

      authHero.classList.add("hidden");
      returnsSection.classList.remove("hidden");
      userArea.classList.remove("hidden");
      userEmail.textContent = user.email;
      loginForm.reset();
      authError.classList.add("hidden");
      loadReturns();
    });
  } else {
    authHero.classList.remove("hidden");
    returnsSection.classList.add("hidden");
    userArea.classList.add("hidden");
  }
});

// Load returns from Firebase
function loadReturns() {
  const returnsRef = ref(database, "busStopsReturn");
  onValue(returnsRef, (snapshot) => {
    returnsData = snapshot.val() || {};
    displayReturns();
  });
}

// Display returns
function displayReturns() {
  if (!returnsList) return;
  returnsList.innerHTML = "";
  Object.entries(returnsData).forEach(([id, stop]) => {
    const stopCard = document.createElement("div");
    stopCard.className = "news-card";
    const rNumber = safeString(stop.routeNumber);
    const sName = safeString(stop.stopName);
    const order = safeNumber(stop.stopOrder);
    const lat = safeNumber(stop.lat);
    const lng = safeNumber(stop.lng);
    const pos =
      lat !== null && lng !== null
        ? `(${lat.toFixed(4)}, ${lng.toFixed(4)})`
        : "N/A";
    stopCard.innerHTML = `
      <h3>Tuyến ${rNumber} - ${sName}</h3>
      <p><strong>Thứ tự:</strong> ${order !== null ? order : "N/A"}</p>
      <p><strong>Vị trí:</strong> ${pos}</p>
      <div class="news-actions">
        <button class="edit" onclick="editReturn('${id}')">Sửa</button>
        <button class="delete" onclick="deleteReturn('${id}')">Xóa</button>
      </div>
    `;
    returnsList.appendChild(stopCard);
  });
}

// Filter returns based on search
function filterReturns(searchTerm) {
  returnsList.innerHTML = "";
  const term = searchTerm.toLowerCase().trim();

  // If search is empty, show all returns
  if (!term) {
    displayReturns();
    return;
  }

  Object.entries(returnsData).forEach(([id, stop]) => {
    const routeNumber = safeString(stop.routeNumber).toLowerCase();
    const stopName = safeString(stop.stopName).toLowerCase();

    // Match if search term is in route number or stop name
    if (routeNumber.includes(term) || stopName.includes(term)) {
      const stopCard = document.createElement("div");
      stopCard.className = "news-card";
      const rNumber = safeString(stop.routeNumber);
      const sName = safeString(stop.stopName);
      const order = safeNumber(stop.stopOrder);
      const lat = safeNumber(stop.lat);
      const lng = safeNumber(stop.lng);
      const pos =
        lat !== null && lng !== null
          ? `(${lat.toFixed(4)}, ${lng.toFixed(4)})`
          : "N/A";
      stopCard.innerHTML = `
        <h3>Tuyến ${rNumber} - ${sName}</h3>
        <p><strong>Thứ tự:</strong> ${order !== null ? order : "N/A"}</p>
        <p><strong>Vị trí:</strong> ${pos}</p>
        <div class="news-actions">
          <button class="edit" onclick="editReturn('${id}')">Sửa</button>
          <button class="delete" onclick="deleteReturn('${id}')">Xóa</button>
        </div>
      `;
      returnsList.appendChild(stopCard);
    }
  });

  // Show message if no results
  if (returnsList.children.length === 0) {
    returnsList.innerHTML = `<p class="empty-state-message">Không tìm thấy trạm nào phù hợp với "${searchTerm}"</p>`;
  }
}

// Edit return
window.editReturn = (id) => {
  const stop = returnsData[id];
  returnIdInput.value = id;
  routeNumberInput.value = safeString(stop.routeNumber);
  stopNameInput.value = safeString(stop.stopName);
  stopOrderInput.value =
    safeNumber(stop.stopOrder) !== null ? safeNumber(stop.stopOrder) : "";
  latitudeInput.value =
    safeNumber(stop.lat) !== null ? safeNumber(stop.lat) : "";
  longitudeInput.value =
    safeNumber(stop.lng) !== null ? safeNumber(stop.lng) : "";
  cancelBtn.classList.remove("hidden");
  window.scrollTo(0, 0);
};

// Delete return
window.deleteReturn = (id) => {
  if (confirm("Bạn có chắc chắn muốn xóa trạm lượt về này?")) {
    remove(ref(database, `busStopsReturn/${id}`));
  }
};

// Save return
returnsForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const returnId = returnIdInput.value || Date.now().toString();
  const returnData = {
    routeNumber: routeNumberInput.value,
    stopName: stopNameInput.value,
    stopOrder: parseInt(stopOrderInput.value),
    lat: parseFloat(latitudeInput.value),
    lng: parseFloat(longitudeInput.value),
  };
  try {
    await set(ref(database, `busStopsReturn/${returnId}`), returnData);
    returnsForm.reset();
    returnIdInput.value = "";
    cancelBtn.classList.add("hidden");
    alert("Lưu thành công!");
  } catch (error) {
    alert("Lỗi: " + error.message);
  }
});

// Import sample data
importBtn.addEventListener("click", async () => {
  if (!confirm("Import dữ liệu mẫu? Tất cả trạm lượt về cũ sẽ bị xóa!")) {
    return;
  }

  try {
    const response = await fetch("sample-routes-data.json");
    const data = await response.json();
    const returnsToImport = data.stopsReturn;

    let count = 0;
    for (const [key, returnData] of Object.entries(returnsToImport)) {
      await set(ref(database, `busStopsReturn/${key}`), returnData);
      count++;
    }

    alert(`✅ Đã import thành công ${count} trạm lượt về!`);
    loadReturns();
  } catch (error) {
    alert("❌ Lỗi import: " + error.message);
    console.error("Import error:", error);
  }
});

// Cancel edit
cancelBtn.addEventListener("click", () => {
  returnsForm.reset();
  returnIdInput.value = "";
  cancelBtn.classList.add("hidden");
});

// Search returns
searchInput.addEventListener("input", (e) => {
  filterReturns(e.target.value);
});
