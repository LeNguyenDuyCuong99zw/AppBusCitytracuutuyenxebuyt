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
    });
  } else {
    window.location.href = "index.html";
  }
});

console.log("BusCity Admin Accounts loaded");
