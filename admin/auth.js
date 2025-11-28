// Auth module for login and dashboard navigation
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.22.1/firebase-app.js";
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  sendPasswordResetEmail,
} from "https://www.gstatic.com/firebasejs/9.22.1/firebase-auth.js";
import {
  getDatabase,
  ref,
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
const db = getDatabase(app);

// DOM elements
const loginForm = document.getElementById("login-form");
const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const userArea = document.getElementById("user-area");
const userEmailSpan = document.getElementById("user-email");
const signoutBtn = document.getElementById("signout-btn");
const authErrorDiv = document.getElementById("auth-error");
const resetPasswordBtn = document.getElementById("reset-password-btn");
const authHeroSection = document.getElementById("auth-hero");
const dashboardSection = document.getElementById("dashboard");

// Login handler
loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const email = emailInput.value.trim();
  const password = passwordInput.value;
  try {
    await signInWithEmailAndPassword(auth, email, password);
    // onAuthStateChanged sẽ kiểm tra quyền và hiển thị dashboard
  } catch (err) {
    console.error("Login failed", err);
    const code = err.code || "unknown";
    let msg = err.message || "Đăng nhập thất bại";
    if (code === "auth/user-not-found") {
      msg = "Không tìm thấy tài khoản. Kiểm tra email hoặc đăng ký mới.";
    } else if (
      code === "auth/wrong-password" ||
      code === "auth/invalid-login-credentials"
    ) {
      msg = "Sai mật khẩu. Bạn có thể đặt lại mật khẩu.";
    } else if (code === "auth/invalid-email") {
      msg = "Email không hợp lệ.";
    }
    if (authErrorDiv) {
      authErrorDiv.textContent = msg + " (" + code + ")";
      authErrorDiv.style.display = "block";
    } else {
      alert(msg + " (" + code + ")");
    }
  }
});

// Logout handler
signoutBtn.addEventListener("click", async () => {
  await signOut(auth);
});

// Password reset
if (resetPasswordBtn) {
  resetPasswordBtn.addEventListener("click", async (e) => {
    e.preventDefault();
    const email = emailInput.value.trim();
    if (!email) {
      if (authErrorDiv) {
        authErrorDiv.textContent = "Vui lòng nhập email để đặt lại mật khẩu.";
        authErrorDiv.style.display = "block";
      } else {
        alert("Vui lòng nhập email để đặt lại mật khẩu.");
      }
      return;
    }
    try {
      await sendPasswordResetEmail(auth, email);
      if (authErrorDiv) {
        authErrorDiv.textContent =
          "Đã gửi email đặt lại mật khẩu. Kiểm tra hộp thư.";
        authErrorDiv.style.display = "block";
      } else {
        alert("Đã gửi email đặt lại mật khẩu. Kiểm tra hộp thư.");
      }
    } catch (err) {
      console.error("Reset password failed", err);
      if (authErrorDiv) {
        authErrorDiv.textContent =
          "Không thể gửi email đặt lại: " + (err.message || err.code);
        authErrorDiv.style.display = "block";
      } else {
        alert("Không thể gửi email đặt lại: " + (err.message || err.code));
      }
    }
  });
}

// Auth state change handler
onAuthStateChanged(auth, (user) => {
  if (user) {
    // Check if user is admin
    const adminCheckRef = ref(db, `admins/${user.uid}`);
    onValue(adminCheckRef, (snap) => {
      const val = snap.exists() ? snap.val() : null;
      const isAdmin = snap.exists() && !!val;
      console.log("Admin check for uid=", user.uid, "isAdmin=", isAdmin);

      if (!isAdmin) {
        if (authErrorDiv) {
          authErrorDiv.textContent = `Tài khoản ${user.uid} không có quyền quản trị.`;
          authErrorDiv.style.display = "block";
        }
        alert("Tài khoản này không có quyền quản trị. UID: " + user.uid);
        signOut(auth);
        return;
      }

      // Show dashboard, hide login
      authHeroSection.classList.add("hidden");
      dashboardSection.classList.remove("hidden");
      userArea.classList.remove("hidden");
      userEmailSpan.textContent = user.email || user.uid;
      if (authErrorDiv) authErrorDiv.style.display = "none";
    });
  } else {
    // User logged out
    authHeroSection.classList.remove("hidden");
    dashboardSection.classList.add("hidden");
    userArea.classList.add("hidden");
    userEmailSpan.textContent = "";
    if (authErrorDiv) authErrorDiv.style.display = "none";
  }
});

console.log("BusCity Admin Auth loaded");
