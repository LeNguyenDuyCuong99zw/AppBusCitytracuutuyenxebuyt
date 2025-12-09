// Route ratings management module
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.22.1/firebase-app.js";
import {
  getAuth,
  signOut,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/9.22.1/firebase-auth.js";
import {
  getDatabase,
  ref,
  onValue,
  update,
  remove,
} from "https://www.gstatic.com/firebasejs/9.22.1/firebase-database.js";

// Firebase config (same as app.js)
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
const ratingsListDiv = document.getElementById("ratings-list");
const statsDiv = document.getElementById("stats");
const searchRouteInput = document.getElementById("searchRoute");
const filterStatusSelect = document.getElementById("filterStatus");
const filterBtn = document.getElementById("filter-btn");
const refreshBtn = document.getElementById("refresh-btn");

let allRatings = [];
let ratingsListening = false;
const ratingsRef = ref(db, "route_ratings");

// Logout handler
signoutBtn.addEventListener("click", async () => {
  await signOut(auth);
  window.location.href = "index.html";
});

// Filter button
filterBtn.addEventListener("click", () => {
  renderUI();
});

// Refresh button
refreshBtn.addEventListener("click", () => {
  loadRatings();
});

// Search on Enter
searchRouteInput.addEventListener("keyup", (e) => {
  if (e.key === "Enter") {
    renderUI();
  }
});

// Start listening to ratings
function startListeningRatings() {
  if (ratingsListening) return;
  ratingsListening = true;
  onValue(ratingsRef, (snapshot) => {
    allRatings = [];
    snapshot.forEach((childSnapshot) => {
      const rating = childSnapshot.val();
      rating.id = childSnapshot.key;
      allRatings.push(rating);
    });
    // Sort by timestamp descending
    allRatings.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
    renderUI();
  });
}

// Load ratings (initial fetch)
function loadRatings() {
  // Clear filters to show all
  searchRouteInput.value = "";
  filterStatusSelect.value = "";
  startListeningRatings();
}

// Render UI with current filters
function renderUI() {
  const searchRoute = searchRouteInput.value.toLowerCase().trim();
  const filterStatus = filterStatusSelect.value;

  // Filter ratings
  const filtered = allRatings.filter((r) => {
    const routeMatch =
      !searchRoute || (r.routeNumber || "").toLowerCase().includes(searchRoute);
    const statusMatch =
      !filterStatus ||
      (filterStatus === "verified" && r.isVerified) ||
      (filterStatus === "pending" && !r.isVerified);
    return routeMatch && statusMatch;
  });

  // Render stats
  renderStats(filtered);

  // Render list
  renderRatingsList(filtered);
}

// Render statistics
function renderStats(ratings) {
  const total = ratings.length;
  const verified = ratings.filter((r) => r.isVerified).length;
  const pending = total - verified;
  const routes = new Set(ratings.map((r) => r.routeNumber || "")).size;

  let avgRating = 0;
  if (ratings.length > 0) {
    const sum = ratings.reduce((acc, r) => acc + (r.rating || 0), 0);
    avgRating = (sum / ratings.length).toFixed(1);
  }

  statsDiv.innerHTML = `
    <div class="stat-item">
      <div class="stat-label">Tổng đánh giá</div>
      <div class="stat-value">${total}</div>
    </div>
    <div class="stat-item">
      <div class="stat-label">Đã duyệt</div>
      <div class="stat-value">${verified}</div>
    </div>
    <div class="stat-item">
      <div class="stat-label">Chưa duyệt</div>
      <div class="stat-value">${pending}</div>
    </div>
    <div class="stat-item">
      <div class="stat-label">Đánh giá trung bình</div>
      <div class="stat-value">${avgRating} ⭐</div>
    </div>
    <div class="stat-item">
      <div class="stat-label">Tuyến</div>
      <div class="stat-value">${routes}</div>
    </div>
  `;
}

// Render ratings list
function renderRatingsList(ratings) {
  ratingsListDiv.innerHTML = "";

  if (ratings.length === 0) {
    ratingsListDiv.innerHTML =
      '<div class="empty-state">📭 Chưa có đánh giá nào</div>';
    return;
  }

  ratings.forEach((r) => {
    const date = new Date(r.timestamp || Date.now());
    const dateStr = date.toLocaleString("vi-VN");
    const stars = "⭐".repeat(r.rating || 0) + "☆".repeat(5 - (r.rating || 0));
    const statusClass = r.isVerified ? "verified" : "pending";
    const statusText = r.isVerified ? "✓ Đã duyệt" : "⏳ Chưa duyệt";
    const userInitial = (r.userName || "A")[0].toUpperCase();

    const card = document.createElement("div");
    card.className = "rating-card";

    card.innerHTML = `
      <div class="rating-header">
        <div class="rating-user">
          <div class="rating-user-avatar">${escapeHtml(userInitial)}</div>
          <div class="rating-user-info">
            <div class="rating-user-name">${escapeHtml(
              r.userName || "Ẩn danh"
            )}</div>
            <div class="rating-user-id">UID: ${escapeHtml(r.userId || "")}</div>
          </div>
        </div>
        <div class="rating-header-right">
          <span class="rating-route">${escapeHtml(
            r.routeNumber || "N/A"
          )}</span>
          <span class="rating-stars">${stars}</span>
          <span class="rating-status ${statusClass}">${statusText}</span>
        </div>
      </div>
      ${
        r.feedback
          ? `<div class="rating-feedback">${escapeHtml(r.feedback)}</div>`
          : '<div class="no-feedback">(Không có phản hồi)</div>'
      }
      <div class="rating-footer">
        <div class="rating-time">${dateStr}</div>
        <div class="rating-actions" data-id="${r.id}" data-verified="${
      r.isVerified
    }">
        </div>
      </div>
    `;

    ratingsListDiv.appendChild(card);

    // Add event listeners to action buttons
    const actionsDiv = card.querySelector(".rating-actions");
    const ratingId = r.id;
    const isVerified = r.isVerified;

    const statusBtn = document.createElement("button");
    statusBtn.className = "secondary-cta";
    statusBtn.textContent = isVerified ? "✗ Bỏ duyệt" : "✓ Duyệt";
    statusBtn.addEventListener("click", () => {
      if (isVerified) {
        unverifyRating(ratingId);
      } else {
        verifyRating(ratingId);
      }
    });
    actionsDiv.appendChild(statusBtn);

    const deleteBtn = document.createElement("button");
    deleteBtn.className = "delete";
    deleteBtn.textContent = "🗑️ Xóa";
    deleteBtn.addEventListener("click", () => {
      deleteRating(ratingId);
    });
    actionsDiv.appendChild(deleteBtn);
  });
}

// Verify rating
window.verifyRating = async (id) => {
  try {
    await update(ref(db, `route_ratings/${id}`), { isVerified: true });
  } catch (err) {
    alert("Lỗi: " + (err.message || err));
  }
};

// Unverify rating
window.unverifyRating = async (id) => {
  try {
    await update(ref(db, `route_ratings/${id}`), { isVerified: false });
  } catch (err) {
    alert("Lỗi: " + (err.message || err));
  }
};

// Delete rating
window.deleteRating = async (id) => {
  if (!confirm("Xóa đánh giá này?")) return;
  try {
    await remove(ref(db, `route_ratings/${id}`));
  } catch (err) {
    alert("Lỗi: " + (err.message || err));
  }
};

// Helper function to escape HTML
function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

// Auth state change - require admin (same pattern as news.js)
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
      loadRatings();
    });
  } else {
    window.location.href = "index.html";
  }
});

console.log("BusCity Admin Route Ratings loaded");
