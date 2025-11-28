// Admin app using Firebase v9 modular SDK (CDN imports)
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.22.1/firebase-app.js";
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  sendPasswordResetEmail,
  createUserWithEmailAndPassword,
} from "https://www.gstatic.com/firebasejs/9.22.1/firebase-auth.js";
import {
  getDatabase,
  ref,
  push,
  set,
  onValue,
  update,
  remove,
} from "https://www.gstatic.com/firebasejs/9.22.1/firebase-database.js";
import {
  getStorage,
  ref as sRef,
  uploadBytesResumable,
  getDownloadURL,
} from "https://www.gstatic.com/firebasejs/9.22.1/firebase-storage.js";

// Firebase config (from app/google-services.json)
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

// DOM
const loginForm = document.getElementById("login-form");
const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const userArea = document.getElementById("user-area");
const userEmailSpan = document.getElementById("user-email");
const signoutBtn = document.getElementById("signout-btn");
const authErrorDiv = document.getElementById("auth-error");
const resetPasswordBtn = document.getElementById("reset-password-btn");
const registerBtn = document.getElementById("register-btn");

const editorSection = document.getElementById("editor");
const listSection = document.getElementById("list");
const newsForm = document.getElementById("news-form");
const titleInput = document.getElementById("news-title");
const contentInput = document.getElementById("news-content");
const imageUrlInput = document.getElementById("news-image-url");
const imageFileInput = document.getElementById("news-image-file");
const uploadProgress = document.getElementById("upload-progress");
const uploadPercent = document.getElementById("upload-percent");
const dateInput = document.getElementById("news-date");
const saveBtn = document.getElementById("save-news-btn");
const cancelEditBtn = document.getElementById("cancel-edit-btn");
const newsListDiv = document.getElementById("news-list");

let editingKey = null;

// Auth: login
loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const email = emailInput.value.trim();
  const password = passwordInput.value;
  try {
    await signInWithEmailAndPassword(auth, email, password);
    // Show immediate feedback so the user sees login succeeded while we
    // check admin rights (onAuthStateChanged will still run).
    if (authErrorDiv) {
      const uid =
        auth.currentUser && auth.currentUser.uid
          ? auth.currentUser.uid
          : "(chưa có UID)";
      authErrorDiv.textContent = `Đăng nhập thành công. UID: ${uid} — đang kiểm tra quyền quản trị...`;
      authErrorDiv.style.display = "block";
    }
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

// Register new user (note: does NOT auto-grant admin rights)
if (registerBtn) {
  registerBtn.addEventListener("click", async (e) => {
    e.preventDefault();
    const email = emailInput.value.trim();
    const password = passwordInput.value;
    if (!email || !password) {
      if (authErrorDiv) {
        authErrorDiv.textContent =
          "Vui lòng nhập email và mật khẩu để đăng ký.";
        authErrorDiv.style.display = "block";
      } else {
        alert("Vui lòng nhập email và mật khẩu để đăng ký.");
      }
      return;
    }
    try {
      const cred = await createUserWithEmailAndPassword(auth, email, password);
      const uid = cred.user && cred.user.uid;
      if (authErrorDiv) {
        authErrorDiv.textContent =
          "Tạo tài khoản thành công. UID: " +
          uid +
          ".\nLưu UID này vào /admins để cấp quyền quản trị.";
        authErrorDiv.style.display = "block";
      } else {
        alert(
          "Tạo tài khoản thành công. UID: " +
            uid +
            ". Lưu UID này vào /admins để cấp quyền quản trị."
        );
      }
    } catch (err) {
      console.error("Register failed", err);
      if (authErrorDiv) {
        authErrorDiv.textContent =
          "Đăng ký thất bại: " + (err.message || err.code);
        authErrorDiv.style.display = "block";
      } else {
        alert("Đăng ký thất bại: " + (err.message || err.code));
      }
    }
  });
}

// Auth state change
onAuthStateChanged(auth, (user) => {
  const loginFormEl = document.getElementById("login-form");
  if (user) {
    // check if uid exists under /admins
    const adminCheckRef = ref(db, `admins/${user.uid}`);
    onValue(adminCheckRef, (snap) => {
      const val = snap.exists() ? snap.val() : null;
      const isAdmin = snap.exists() && !!val;
      console.log(
        "Admin check for uid=",
        user.uid,
        "dbValue=",
        val,
        "isAdmin=",
        isAdmin
      );
      if (!isAdmin) {
        // show a helpful message with UID and DB value so user can diagnose
        if (authErrorDiv) {
          authErrorDiv.textContent = `Tài khoản ${
            user.uid
          } không có quyền quản trị. DB value: ${JSON.stringify(val)}`;
          authErrorDiv.style.display = "block";
        }
        alert("Tài khoản này không có quyền quản trị. UID: " + user.uid);
        signOut(auth);
        return;
      }
      // is admin -> show admin UI
      loginFormEl.classList.add("hidden");
      userArea.classList.remove("hidden");
      editorSection.classList.remove("hidden");
      if (adminManageSection) adminManageSection.classList.remove("hidden");
      listSection.classList.remove("hidden");
      userEmailSpan.textContent = user.email || user.uid;
      if (authErrorDiv) authErrorDiv.style.display = "none";
      startListeningNews();
      startListeningAdmins();
    });
  } else {
    loginFormEl.classList.remove("hidden");
    userArea.classList.add("hidden");
    editorSection.classList.add("hidden");
    listSection.classList.add("hidden");
    if (adminManageSection) adminManageSection.classList.add("hidden");
    userEmailSpan.textContent = "";
    if (authErrorDiv) authErrorDiv.style.display = "none";
    stopListeningNews();
    newsListDiv.innerHTML = "";
  }
});

// --- Admin management: DOM refs, listeners, add/remove ---
const adminManageSection = document.getElementById("admin-manage");
const addAdminForm = document.getElementById("add-admin-form");
const adminUidInput = document.getElementById("admin-uid-input");
const addAdminBtn = document.getElementById("add-admin-btn");
const fillMyUidBtn = document.getElementById("fill-my-uid-btn");
const adminListDiv = document.getElementById("admin-list");

let adminsListening = false;
const adminsRef = ref(db, "admins");

function startListeningAdmins() {
  if (adminsListening) return;
  adminsListening = true;
  onValue(adminsRef, (snapshot) => {
    const data = snapshot.val() || {};
    renderAdminList(data);
  });
}

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

if (fillMyUidBtn) {
  fillMyUidBtn.addEventListener("click", (e) => {
    e.preventDefault();
    const uid = auth.currentUser && auth.currentUser.uid;
    if (uid && adminUidInput) adminUidInput.value = uid;
  });
}
// News CRUD
const newsRef = ref(db, "news");
let unsubscribeNews = null;

function startListeningNews() {
  if (unsubscribeNews) return;
  onValue(newsRef, (snapshot) => {
    const data = snapshot.val() || {};
    renderNewsList(data);
  });
}

function stopListeningNews() {
  // onValue doesn't return an unsubscribe in CDN v9. We can trigger by reassigning listener, but for simplicity we rely on UI hide.
}

function renderNewsList(data) {
  newsListDiv.innerHTML = "";
  // convert object to list sorted by key (or date if set)
  const entries = Object.entries(data).sort((a, b) => {
    // try comparing date fields if present
    const ad = a[1].date || "";
    const bd = b[1].date || "";
    return bd.localeCompare(ad);
  });

  if (entries.length === 0) {
    newsListDiv.innerHTML = "<p>Chưa có tin nào. Tạo tin mới ở phía trên.</p>";
    return;
  }

  entries.forEach(([key, item]) => {
    const card = document.createElement("div");
    card.className = "news-card";

    if (item.imageUrl) {
      const img = document.createElement("img");
      img.src = item.imageUrl;
      img.alt = item.title || "";
      card.appendChild(img);
    }

    const h3 = document.createElement("h3");
    h3.textContent = item.title || "(Không có tiêu đề)";
    card.appendChild(h3);

    if (item.date) {
      const d = document.createElement("div");
      d.style.fontSize = "12px";
      d.style.color = "#666";
      d.textContent = item.date;
      card.appendChild(d);
    }

    if (item.content) {
      const p = document.createElement("p");
      p.textContent = item.content;
      card.appendChild(p);
    }

    const actions = document.createElement("div");
    actions.className = "news-actions";

    const editBtn = document.createElement("button");
    editBtn.className = "edit";
    editBtn.textContent = "Sửa";
    editBtn.addEventListener("click", () => startEdit(key, item));

    const delBtn = document.createElement("button");
    delBtn.className = "delete";
    delBtn.textContent = "Xóa";
    delBtn.addEventListener("click", () => deleteNews(key));

    actions.appendChild(editBtn);
    actions.appendChild(delBtn);
    card.appendChild(actions);

    newsListDiv.appendChild(card);
  });
}

function startEdit(key, item) {
  editingKey = key;
  titleInput.value = item.title || "";
  contentInput.value = item.content || "";
  imageUrlInput.value = item.imageUrl || "";
  // reset file input
  if (imageFileInput) imageFileInput.value = null;
  // If item.date present in format YYYY-MM-DD, put it
  dateInput.value = item.date || "";
  cancelEditBtn.classList.remove("hidden");
  saveBtn.textContent = "Cập nhật";
}

cancelEditBtn.addEventListener("click", () => {
  resetForm();
});

function resetForm() {
  editingKey = null;
  newsForm.reset();
  cancelEditBtn.classList.add("hidden");
  saveBtn.textContent = "Lưu tin";
}

newsForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const title = titleInput.value.trim();
  const content = contentInput.value.trim();
  let imageUrl = imageUrlInput.value.trim() || "";
  const dateVal = dateInput.value || new Date().toISOString().slice(0, 10);
  if (!title) {
    alert("Tiêu đề không được bỏ trống");
    return;
  }

  try {
    // If a file is selected, upload it first to Firebase Storage
    if (imageFileInput && imageFileInput.files && imageFileInput.files[0]) {
      const file = imageFileInput.files[0];
      const storage = getStorage(app);
      const path = `news_images/${Date.now()}_${file.name}`;
      const sReference = sRef(storage, path);
      const uploadTask = uploadBytesResumable(sReference, file);
      uploadProgress.classList.remove("hidden");
      const downloadUrl = await new Promise((resolve, reject) => {
        uploadTask.on(
          "state_changed",
          (snapshot) => {
            const percent = Math.round(
              (snapshot.bytesTransferred / snapshot.totalBytes) * 100
            );
            uploadPercent.textContent = percent + "%";
          },
          (error) => {
            uploadProgress.classList.add("hidden");
            reject(error);
          },
          async () => {
            const url = await getDownloadURL(uploadTask.snapshot.ref);
            uploadProgress.classList.add("hidden");
            resolve(url);
          }
        );
      });
      imageUrl = downloadUrl;
    }

    if (editingKey) {
      const itemRef = ref(db, `news/${editingKey}`);
      await update(itemRef, {
        title,
        content,
        imageUrl,
        date: dateVal,
      });
      resetForm();
    } else {
      const newRef = push(newsRef);
      await set(newRef, {
        title,
        content,
        imageUrl,
        date: dateVal,
      });
      newsForm.reset();
    }
  } catch (err) {
    alert("Lỗi lưu tin: " + err.message);
  }
});

async function deleteNews(key) {
  if (!confirm("Bạn muốn xóa tin này?")) return;
  try {
    const itemRef = ref(db, `news/${key}`);
    await remove(itemRef);
  } catch (err) {
    alert("Xóa thất bại: " + err.message);
  }
}

// Helpful console message
console.log("BusCity Admin loaded");
