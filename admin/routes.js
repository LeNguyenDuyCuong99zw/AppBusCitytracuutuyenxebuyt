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

// Firebase config (same as auth.js)
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
const routesSection = document.getElementById("routes-section");
const userArea = document.getElementById("user-area");
const userEmail = document.getElementById("user-email");
const signoutBtn = document.getElementById("signout-btn");

// Routes form elements
const routesForm = document.getElementById("routes-form");
const routeIdInput = document.getElementById("route-id");
const routeNumberInput = document.getElementById("route-number");
const routeNameInput = document.getElementById("route-name");
const startTimeInput = document.getElementById("start-time");
const endTimeInput = document.getElementById("end-time");
const priceInput = document.getElementById("price");
const studentPriceInput = document.getElementById("student-price");
const monthlyPass30Input = document.getElementById("monthly-pass-30");
const routeTypeInput = document.getElementById("route-type");
const runTimeInput = document.getElementById("run-time");
const spacingInput = document.getElementById("spacing");
const stopsInput = document.getElementById("stops");
const ratingInput = document.getElementById("rating");
const cancelBtn = document.getElementById("cancel-btn");
const routesList = document.getElementById("routes-list");
const importBtn = document.getElementById("import-btn");
const searchInput = document.getElementById("search-route");

let routesData = {};

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
      routesSection.classList.remove("hidden");
      userArea.classList.remove("hidden");
      userEmail.textContent = user.email;
      loginForm.reset();
      authError.classList.add("hidden");
      loadRoutes();
    });
  } else {
    authHero.classList.remove("hidden");
    routesSection.classList.add("hidden");
    userArea.classList.add("hidden");
  }
});

// Load routes from Firebase
function loadRoutes() {
  const routesRef = ref(database, "routes");
  onValue(routesRef, (snapshot) => {
    routesData = snapshot.val() || {};
    displayRoutes();
  });
}

// Display routes
function displayRoutes() {
  routesList.innerHTML = "";
  Object.entries(routesData).forEach(([id, route]) => {
    const routeCard = document.createElement("div");
    routeCard.className = "news-card";
    routeCard.innerHTML = `
      <h3>Tuyến ${route.routeNumber} - ${route.routeName}</h3>
      <p><strong>Giờ:</strong> ${route.startTime} - ${route.endTime}</p>
      <p><strong>Giá:</strong> ${route.price.toLocaleString()} VND</p>
      <p><strong>Loại:</strong> ${route.routeType || "N/A"}</p>
      <p><strong>Thời gian chạy:</strong> ${route.runTime || "N/A"}</p>
      <p><strong>Đánh giá:</strong> ⭐ ${route.rating || 0}</p>
      <div class="news-actions">
        <button class="edit" onclick="editRoute('${id}')">Sửa</button>
        <button class="delete" onclick="deleteRoute('${id}')">Xóa</button>
      </div>
    `;
    routesList.appendChild(routeCard);
  });
}

// Filter routes based on search
function filterRoutes(searchTerm) {
  routesList.innerHTML = "";
  const term = searchTerm.toLowerCase().trim();

  // If search is empty, show all routes
  if (!term) {
    displayRoutes();
    return;
  }

  Object.entries(routesData).forEach(([id, route]) => {
    const routeNumber = route.routeNumber.toLowerCase();
    const routeName = route.routeName.toLowerCase();

    // Match if search term is in route number or route name
    if (routeNumber.includes(term) || routeName.includes(term)) {
      const routeCard = document.createElement("div");
      routeCard.className = "news-card";
      routeCard.innerHTML = `
        <h3>Tuyến ${route.routeNumber} - ${route.routeName}</h3>
        <p><strong>Giờ:</strong> ${route.startTime} - ${route.endTime}</p>
        <p><strong>Giá:</strong> ${route.price.toLocaleString()} VND</p>
        <p><strong>Loại:</strong> ${route.routeType || "N/A"}</p>
        <p><strong>Thời gian chạy:</strong> ${route.runTime || "N/A"}</p>
        <p><strong>Đánh giá:</strong> ⭐ ${route.rating || 0}</p>
        <div class="news-actions">
          <button class="edit" onclick="editRoute('${id}')">Sửa</button>
          <button class="delete" onclick="deleteRoute('${id}')">Xóa</button>
        </div>
      `;
      routesList.appendChild(routeCard);
    }
  });

  // Show message if no results
  if (routesList.children.length === 0) {
    routesList.innerHTML = `<p style="text-align: center; color: #999; padding: 20px;">Không tìm thấy tuyến nào phù hợp với "${searchTerm}"</p>`;
  }
}

// Edit route
window.editRoute = (id) => {
  const route = routesData[id];
  routeIdInput.value = id;
  routeNumberInput.value = route.routeNumber;
  routeNameInput.value = route.routeName;
  startTimeInput.value = route.startTime;
  endTimeInput.value = route.endTime;
  priceInput.value = route.price;
  studentPriceInput.value = route.studentPrice || "";
  monthlyPass30Input.value = route.monthlyPass30Price || "";
  routeTypeInput.value = route.routeType || "";
  runTimeInput.value = route.runTime || "";
  spacingInput.value = route.spacing || "";
  stopsInput.value = route.stops || "";
  ratingInput.value = route.rating || "";
  cancelBtn.classList.remove("hidden");
  window.scrollTo(0, 0);
};

// Delete route
window.deleteRoute = (id) => {
  if (confirm("Bạn có chắc chắn muốn xóa tuyến này?")) {
    remove(ref(database, `routes/${id}`));
  }
};

// Save route
routesForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const routeId = routeIdInput.value || Date.now().toString();
  const routeData = {
    routeNumber: routeNumberInput.value,
    routeName: routeNameInput.value,
    startTime: startTimeInput.value,
    endTime: endTimeInput.value,
    price: parseInt(priceInput.value),
    studentPrice: parseInt(studentPriceInput.value) || 0,
    monthlyPass30Price: parseInt(monthlyPass30Input.value) || 0,
    routeType: routeTypeInput.value || "Phổ thông",
    runTime: runTimeInput.value || "0",
    spacing: spacingInput.value || "0",
    stops: parseInt(stopsInput.value) || 0,
    rating: parseFloat(ratingInput.value) || 0,
  };
  try {
    await set(ref(database, `routes/${routeId}`), routeData);
    routesForm.reset();
    routeIdInput.value = "";
    cancelBtn.classList.add("hidden");
    alert("Lưu thành công!");
  } catch (error) {
    alert("Lỗi: " + error.message);
  }
});

// Import sample data
importBtn.addEventListener("click", async () => {
  if (!confirm("Import dữ liệu mẫu? Tất cả tuyến cũ sẽ bị xóa!")) {
    return;
  }

  try {
    const response = await fetch("sample-routes-data.json");
    const data = await response.json();
    const routesToImport = data.routes;

    let count = 0;
    for (const [key, routeData] of Object.entries(routesToImport)) {
      await set(ref(database, `routes/${key}`), routeData);
      count++;
    }

    alert(`✅ Đã import thành công ${count} tuyến!`);
    loadRoutes();
  } catch (error) {
    alert("❌ Lỗi import: " + error.message);
    console.error("Import error:", error);
  }
});

// Cancel edit
cancelBtn.addEventListener("click", () => {
  routesForm.reset();
  routeIdInput.value = "";
  cancelBtn.classList.add("hidden");
});

// Search routes
searchInput.addEventListener("input", (e) => {
  filterRoutes(e.target.value);
});
