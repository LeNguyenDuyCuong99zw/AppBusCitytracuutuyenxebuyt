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
const stopsSection = document.getElementById("stops-section");
const userArea = document.getElementById("user-area");
const userEmail = document.getElementById("user-email");
const signoutBtn = document.getElementById("signout-btn");

// Stops form elements
const stopsForm = document.getElementById("stops-form");
const stopIdInput = document.getElementById("stop-id");
const routeNumberInput = document.getElementById("route-number");
const stopNameInput = document.getElementById("stop-name");
const stopOrderInput = document.getElementById("stop-order");
const latitudeInput = document.getElementById("latitude");
const longitudeInput = document.getElementById("longitude");
const cancelBtn = document.getElementById("cancel-btn");
const stopsList = document.getElementById("stops-list");
const importBtn = document.getElementById("import-btn");
const searchInput = document.getElementById("search-stop");

let stopsData = {};

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
      stopsSection.classList.remove("hidden");
      userArea.classList.remove("hidden");
      userEmail.textContent = user.email;
      loginForm.reset();
      authError.classList.add("hidden");
      loadStops();
    });
  } else {
    authHero.classList.remove("hidden");
    stopsSection.classList.add("hidden");
    userArea.classList.add("hidden");
  }
});

// Load stops from Firebase
function loadStops() {
  const stopsRef = ref(database, "busStops");
  onValue(stopsRef, (snapshot) => {
    stopsData = snapshot.val() || {};
    displayStops();
  });
}

// Display stops
function displayStops() {
  stopsList.innerHTML = "";
  Object.entries(stopsData).forEach(([id, stop]) => {
    const stopCard = document.createElement("div");
    stopCard.className = "news-card";
    stopCard.innerHTML = `
      <h3>Tuyến ${stop.routeNumber} - ${stop.stopName}</h3>
      <p><strong>Thứ tự:</strong> ${stop.stopOrder}</p>
      <p><strong>Vị trí:</strong> (${stop.lat.toFixed(4)}, ${stop.lng.toFixed(
      4
    )})</p>
      <div class="news-actions">
        <button class="edit" onclick="editStop('${id}')">Sửa</button>
        <button class="delete" onclick="deleteStop('${id}')">Xóa</button>
      </div>
    `;
    stopsList.appendChild(stopCard);
  });
}

// Filter stops based on search
function filterStops(searchTerm) {
  stopsList.innerHTML = "";
  const term = searchTerm.toLowerCase().trim();

  // If search is empty, show all stops
  if (!term) {
    displayStops();
    return;
  }

  Object.entries(stopsData).forEach(([id, stop]) => {
    const routeNumber = stop.routeNumber.toLowerCase();
    const stopName = stop.stopName.toLowerCase();

    // Match if search term is in route number or stop name
    if (routeNumber.includes(term) || stopName.includes(term)) {
      const stopCard = document.createElement("div");
      stopCard.className = "news-card";
      stopCard.innerHTML = `
        <h3>Tuyến ${stop.routeNumber} - ${stop.stopName}</h3>
        <p><strong>Thứ tự:</strong> ${stop.stopOrder}</p>
        <p><strong>Vị trí:</strong> (${stop.lat.toFixed(4)}, ${stop.lng.toFixed(
        4
      )})</p>
        <div class="news-actions">
          <button class="edit" onclick="editStop('${id}')">Sửa</button>
          <button class="delete" onclick="deleteStop('${id}')">Xóa</button>
        </div>
      `;
      stopsList.appendChild(stopCard);
    }
  });

  // Show message if no results
  if (stopsList.children.length === 0) {
    stopsList.innerHTML = `<p style="text-align: center; color: #999; padding: 20px;">Không tìm thấy trạm nào phù hợp với "${searchTerm}"</p>`;
  }
}

// Edit stop
window.editStop = (id) => {
  const stop = stopsData[id];
  stopIdInput.value = id;
  routeNumberInput.value = stop.routeNumber;
  stopNameInput.value = stop.stopName;
  stopOrderInput.value = stop.stopOrder;
  latitudeInput.value = stop.lat;
  longitudeInput.value = stop.lng;
  cancelBtn.classList.remove("hidden");
  window.scrollTo(0, 0);
};

// Delete stop
window.deleteStop = (id) => {
  if (confirm("Bạn có chắc chắn muốn xóa trạm này?")) {
    remove(ref(database, `busStops/${id}`));
  }
};

// Save stop
stopsForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const stopId = stopIdInput.value || Date.now().toString();
  const stopData = {
    routeNumber: routeNumberInput.value,
    stopName: stopNameInput.value,
    stopOrder: parseInt(stopOrderInput.value),
    lat: parseFloat(latitudeInput.value),
    lng: parseFloat(longitudeInput.value),
  };
  try {
    await set(ref(database, `busStops/${stopId}`), stopData);
    stopsForm.reset();
    stopIdInput.value = "";
    cancelBtn.classList.add("hidden");
    alert("Lưu thành công!");
  } catch (error) {
    alert("Lỗi: " + error.message);
  }
});

// Import sample data
importBtn.addEventListener("click", async () => {
  if (!confirm("Import dữ liệu mẫu? Tất cả trạm cũ sẽ bị xóa!")) {
    return;
  }

  try {
    const response = await fetch("sample-routes-data.json");
    const data = await response.json();
    const stopsToImport = data.stops;

    let count = 0;
    for (const [key, stopData] of Object.entries(stopsToImport)) {
      await set(ref(database, `busStops/${key}`), stopData);
      count++;
    }

    alert(`✅ Đã import thành công ${count} trạm!`);
    loadStops();
  } catch (error) {
    alert("❌ Lỗi import: " + error.message);
    console.error("Import error:", error);
  }
});

// Cancel edit
cancelBtn.addEventListener("click", () => {
  stopsForm.reset();
  stopIdInput.value = "";
  cancelBtn.classList.add("hidden");
});

// Search stops
searchInput.addEventListener("input", (e) => {
  filterStops(e.target.value);
});
