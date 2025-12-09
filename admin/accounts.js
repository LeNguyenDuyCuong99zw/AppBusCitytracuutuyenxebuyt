// Accounts (Admin management) module
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.22.1/firebase-app.js";
import {
  getAuth,
  signOut,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/9.22.1/firebase-auth.js";
import {
  getDatabase,
  ref,
  set,
  onValue,
  remove,
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
const db = getDatabase(app);

// DOM elements
const userEmailSpan = document.getElementById("user-email");
const signoutBtn = document.getElementById("signout-btn");
const addAdminForm = document.getElementById("add-admin-form");
const adminUidInput = document.getElementById("admin-uid-input");
const addAdminBtn = document.getElementById("add-admin-btn");
const fillMyUidBtn = document.getElementById("fill-my-uid-btn");
const adminListDiv = document.getElementById("admin-list");
const authErrorDiv = document.getElementById("auth-error");

let adminsListening = false;
const adminsRef = ref(db, "admins");

// Tab navigation
const tabButtons = document.querySelectorAll(".tab-btn");
const tabContents = document.querySelectorAll(".tab-content");
tabButtons.forEach((btn) => {
  btn.addEventListener("click", () => {
    const tabName = btn.getAttribute("data-tab");
    // Hide all
    tabContents.forEach((c) => c.classList.remove("active"));
    tabButtons.forEach((b) => b.classList.remove("active"));
    // Show selected
    document.getElementById(tabName).classList.add("active");
    btn.classList.add("active");
    // Load users if user tab is clicked
    if (tabName === "user-manage") {
      loadUsers();
    }
  });
});

// Logout handler
signoutBtn.addEventListener("click", async () => {
  await signOut(auth);
  window.location.href = "index.html";
});

// Start listening to admins list
function startListeningAdmins() {
  if (adminsListening) return;
  adminsListening = true;
  onValue(adminsRef, (snapshot) => {
    const data = snapshot.val() || {};
    renderAdminList(data);
  });
}

// Render admin list
function renderAdminList(data) {
  if (!adminListDiv) return;
  adminListDiv.innerHTML = "";
  const entries = Object.entries(data || {});
  if (entries.length === 0) {
    adminListDiv.innerHTML = "<p>Chưa có admin nào được cấp.</p>";
    return;
  }
  entries.forEach(([uid, val]) => {
    const row = document.createElement("div");
    row.style.display = "flex";
    row.style.justifyContent = "space-between";
    row.style.alignItems = "center";
    row.style.padding = "6px 8px";
    row.style.borderBottom = "1px solid #efefef";
    const left = document.createElement("div");
    left.textContent = uid + (val !== true ? ` — ${JSON.stringify(val)}` : "");
    const btn = document.createElement("button");
    btn.textContent = "Xóa";
    btn.style.background = "#e53935";
    btn.style.color = "#fff";
    btn.style.border = "none";
    btn.style.padding = "6px 8px";
    btn.style.borderRadius = "6px";
    btn.addEventListener("click", async () => {
      if (!confirm(`Xóa quyền admin cho UID ${uid}?`)) return;
      try {
        await remove(ref(db, `admins/${uid}`));
        console.log("Removed admin", uid);
      } catch (err) {
        console.error("Failed removing admin", err);
        if (authErrorDiv) {
          authErrorDiv.textContent =
            "Xóa thất bại: " + (err.message || err.code);
          authErrorDiv.style.display = "block";
        }
      }
    });
    row.appendChild(left);
    row.appendChild(btn);
    adminListDiv.appendChild(row);
  });
}

// Add admin form handler
if (addAdminForm) {
  addAdminForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const uid = ((adminUidInput && adminUidInput.value) || "").trim();
    if (!uid) {
      if (authErrorDiv) {
        authErrorDiv.textContent = "Vui lòng nhập UID để thêm.";
        authErrorDiv.style.display = "block";
      }
      return;
    }
    try {
      await set(ref(db, `admins/${uid}`), true);
      adminUidInput.value = "";
      if (authErrorDiv) {
        authErrorDiv.textContent = `Đã thêm admin: ${uid}`;
        authErrorDiv.style.display = "block";
      }
    } catch (err) {
      console.error("Add admin failed", err);
      if (authErrorDiv) {
        authErrorDiv.textContent =
          "Thêm admin thất bại: " + (err.message || err.code);
        authErrorDiv.style.display = "block";
      }
    }
  });
}

// Fill current user's UID
if (fillMyUidBtn) {
  fillMyUidBtn.addEventListener("click", (e) => {
    e.preventDefault();
    const uid = auth.currentUser && auth.currentUser.uid;
    if (uid && adminUidInput) adminUidInput.value = uid;
  });
}

// ====== User Management ======
const searchUserInput = document.getElementById("search-user");
const searchUserBtn = document.getElementById("search-user-btn");
const usersListDiv = document.getElementById("users-list");
const userStatsDiv = document.getElementById("user-stats");

let allUsers = [];
let usersListening = false;
const usersRef = ref(db, "users");

// Load and listen to users
async function loadUsers() {
  if (usersListening) return;
  usersListening = true;
  onValue(usersRef, (snapshot) => {
    allUsers = [];
    snapshot.forEach((childSnapshot) => {
      const user = childSnapshot.val();
      user.uid = childSnapshot.key;
      allUsers.push(user);
    });
    renderUserStats();
    renderUsersList(allUsers);
  });
}

// Render user statistics
function renderUserStats() {
  const total = allUsers.length;
  const verified = allUsers.filter((u) => u.isVerified).length;
  const ratedUsers = allUsers.filter(
    (u) => u.totalRatings && u.totalRatings > 0
  ).length;

  userStatsDiv.innerHTML = `
    <div class="stat-card">
      <div class="stat-label">Tổng người dùng</div>
      <div class="stat-value">${total}</div>
    </div>
    <div class="stat-card">
      <div class="stat-label">Đã xác thực</div>
      <div class="stat-value">${verified}</div>
    </div>
    <div class="stat-card">
      <div class="stat-label">Người đánh giá</div>
      <div class="stat-value">${ratedUsers}</div>
    </div>
  `;
}

// Render users list
function renderUsersList(users) {
  usersListDiv.innerHTML = "";

  if (users.length === 0) {
    usersListDiv.innerHTML =
      '<div class="empty-users-state">📭 Chưa có người dùng nào</div>';
    return;
  }

  const table = document.createElement("table");
  table.className = "users-table";

  // Header
  const thead = document.createElement("thead");
  thead.innerHTML = `
    <tr>
      <th>Tên</th>
      <th>Email</th>
      <th>Điện thoại</th>
      <th>UID</th>
      <th>Xác thực</th>
      <th>Đánh giá</th>
      <th>Ngày tạo</th>
    </tr>
  `;
  table.appendChild(thead);

  // Body
  const tbody = document.createElement("tbody");
  users.forEach((user) => {
    const row = document.createElement("tr");
    row.className = "users-table-row";

    const verifiedBadge = user.isVerified
      ? '<span class="badge badge-verified">✓ Có</span>'
      : '<span class="badge badge-pending">⏳ Chưa</span>';

    const createDate = user.createdAt
      ? new Date(user.createdAt).toLocaleDateString("vi-VN")
      : "N/A";

    // Lấy thông tin từ user profile (displayName hoặc profileName từ DataStore)
    const userName = escapeHtml(
      user.profileName || user.displayName || user.name || "Ẩn danh"
    );
    const userEmail = escapeHtml(user.email || "N/A");
    const userPhone = escapeHtml(user.profilePhone || user.phone || "N/A");

    row.innerHTML = `
      <td>${userName}</td>
      <td>${userEmail}</td>
      <td>${userPhone}</td>
      <td class="uid-cell">${escapeHtml(user.uid)}</td>
      <td>${verifiedBadge}</td>
      <td class="rating-cell">${user.totalRatings || 0}</td>
      <td class="date-cell">${createDate}</td>
    `;
    tbody.appendChild(row);
  });
  table.appendChild(tbody);
  usersListDiv.appendChild(table);
}

// Search users
if (searchUserBtn) {
  searchUserBtn.addEventListener("click", () => {
    const query =
      (searchUserInput && searchUserInput.value.toLowerCase().trim()) || "";
    const filtered = allUsers.filter(
      (u) =>
        (u.email && u.email.toLowerCase().includes(query)) ||
        (u.uid && u.uid.toLowerCase().includes(query)) ||
        (u.profileName && u.profileName.toLowerCase().includes(query)) ||
        (u.displayName && u.displayName.toLowerCase().includes(query))
    );
    renderUsersList(filtered);
  });
}

// Search on Enter
if (searchUserInput) {
  searchUserInput.addEventListener("keyup", (e) => {
    if (e.key === "Enter" && searchUserBtn) {
      searchUserBtn.click();
    }
  });
}

// Helper to escape HTML
function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

// Auth state change
onAuthStateChanged(auth, (user) => {
  if (user) {
    // Check if admin
    const adminCheckRef = ref(db, `admins/${user.uid}`);
    onValue(adminCheckRef, (snap) => {
      const isAdmin = snap.exists() && !!snap.val();
      if (!isAdmin) {
        alert("Không có quyền truy cập trang này");
        window.location.href = "index.html";
        return;
      }
      userEmailSpan.textContent = user.email || user.uid;
      startListeningAdmins();
      loadUsers(); // Load users on auth success
    });
  } else {
    window.location.href = "index.html";
  }
});

console.log("BusCity Admin Accounts loaded");
