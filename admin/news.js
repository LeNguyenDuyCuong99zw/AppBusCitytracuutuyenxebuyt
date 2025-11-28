// News management module
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.22.1/firebase-app.js";
import {
  getAuth,
  signOut,
  onAuthStateChanged,
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
let newsListening = false;
const newsRef = ref(db, "news");

// Logout handler
signoutBtn.addEventListener("click", async () => {
  await signOut(auth);
  window.location.href = "index.html";
});

// Start listening to news
function startListeningNews() {
  if (newsListening) return;
  newsListening = true;
  onValue(newsRef, (snapshot) => {
    const data = snapshot.val() || {};
    renderNewsList(data);
  });
}

// Stop listening to news
function stopListeningNews() {
  newsListening = false;
}

// Render news list
function renderNewsList(data) {
  newsListDiv.innerHTML = "";
  const entries = Object.entries(data).sort((a, b) => {
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

// Start editing
function startEdit(key, item) {
  editingKey = key;
  titleInput.value = item.title || "";
  contentInput.value = item.content || "";
  imageUrlInput.value = item.imageUrl || "";
  if (imageFileInput) imageFileInput.value = null;
  dateInput.value = item.date || "";
  cancelEditBtn.classList.remove("hidden");
  saveBtn.textContent = "Cập nhật";
  // Scroll to form
  newsForm.scrollIntoView({ behavior: "smooth" });
}

// Reset form
function resetForm() {
  editingKey = null;
  newsForm.reset();
  cancelEditBtn.classList.add("hidden");
  saveBtn.textContent = "Lưu tin";
}

// Cancel edit
cancelEditBtn.addEventListener("click", () => {
  resetForm();
});

// Save news
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
    // Upload file if selected
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

// Delete news
async function deleteNews(key) {
  if (!confirm("Bạn muốn xóa tin này?")) return;
  try {
    const itemRef = ref(db, `news/${key}`);
    await remove(itemRef);
  } catch (err) {
    alert("Xóa thất bại: " + err.message);
  }
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
      startListeningNews();
    });
  } else {
    window.location.href = "index.html";
  }
});

console.log("BusCity Admin News loaded");
