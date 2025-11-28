# BusCity - Admin Web

Một admin web nhỏ để quản lý đăng tin tức cho app BusCity. Dùng Firebase Realtime Database và Firebase Auth (Email/Password).

**Tệp chính**

- `index.html` — UI
- `app.js` — logic (Firebase initialisation, Auth, CRUD)
- `style.css` — styles

**Cấu hình Firebase**

- Firebase config hiện được lấy từ `app/google-services.json` (đã chèn `apiKey`, `projectId`, `databaseURL`).

**Thiết lập (một lần, trên Firebase Console)**

1. Mở Firebase Console của project `buscityapp`.
2. Vào `Authentication` → `Sign-in method` → bật **Email/Password**.
3. Vào `Realtime Database` → Tạo database nếu chưa có, chế độ `locked` hoặc `test` theo nhu cầu. Quy tắc viết/đọc hợp lý (ví dụ chỉ cho admin đọc/ghi). Nếu đang thử nghiệm, có thể tạm set rules cho dev.
4. Tạo một user admin (Authentication → Users → Add user) hoặc tự đăng ký từ UI nếu cho phép.

**Chạy local**
Phần tử này là static. Mở `admin/index.html` bằng HTTP server (không mở file trực tiếp để tránh vấn đề CORS/Auth). Ví dụ với PowerShell / Python:

PowerShell (Windows):

```powershell
# từ thư mục project
cd .\admin
python -m http.server 8000
# Sau đó mở http://localhost:8000
```

Hoặc dùng Node (nếu có `npx`):

```powershell
cd .\admin
npx http-server -p 8000
```

**Sử dụng**

- Đăng nhập bằng email và mật khẩu đã tạo.
- Giao diện cho phép tạo tin mới (tiêu đề, nội dung, URL hình, ngày), sửa, xóa.
- Dữ liệu lưu vào Realtime DB dưới nhánh `news`.

**Upload hình**

- Giao diện hiện hỗ trợ upload file ảnh. Nếu bạn chọn một file, nó sẽ được upload lên Firebase Storage vào thư mục `news_images/` và URL tải về sẽ được lưu vào thuộc tính `imageUrl` trong Realtime DB. Nếu bạn không upload file, có thể điền `URL hình` thủ công.

**Quản trị (role)**

- Quyền quản trị được kiểm soát bằng nhánh Realtime DB `admins`. Thêm UID của user admin vào đường dẫn `admins/{uid}` với giá trị `true` để cấp quyền. Ví dụ:

```
admins:
	- "UID_OF_ADMIN_1": true
	- "UID_OF_ADMIN_2": true
```

Khi một user đăng nhập, admin UI sẽ kiểm tra `admins/{uid}` — nếu không tồn tại hoặc giá trị sai, user sẽ bị yêu cầu đăng xuất.

**Ví dụ Rules (Realtime Database)**

Đặt trong Realtime Database → Rules (sửa theo nhu cầu). Dưới đây là ví dụ chỉ cho phép admin ghi/xóa `news`, còn đọc công khai:

```json
{
  "rules": {
    "news": {
      ".read": true,
      ".write": "root.child('admins').child(auth.uid).val() === true"
    },
    "admins": {
      ".read": false,
      ".write": "root.child('admins').child(auth.uid).val() === true"
    }
  }
}
```

**Ví dụ Rules (Firebase Storage)**

Ví dụ rule để chỉ admin được upload / xóa hình trong `news_images/`:

```js
rules_version = '2';
service firebase.storage {
	match /b/{bucket}/o {
		match /news_images/{allPaths=**} {
			allow read: if true;
			allow write: if request.auth != null &&
										request.auth.uid in get(/databases/(default)/documents/admins).data; // if using Firestore
		}
		// Fallback deny
		match /{allPaths=**} {
			allow read: if false;
			allow write: if false;
		}
	}
}
```

Note: The Storage rules example above references Firestore for admin checks as an example. For Realtime Database checks in Storage rules you can mirror admin UIDs into custom claims (recommended) or use a safer server-side check (Cloud Functions) because Storage rules can't directly access Realtime Database easily.

**Lưu ý bảo mật (tiếp)**

- Để bảo mật chặt hơn, cân nhắc: dùng Custom Claims (Identity) cho admin role, hoặc sử dụng Cloud Functions để thực hiện các thao tác quản trị. Ví dụ: lưu danh sách admin trên server và cấp token có claim `admin=true`.

**Lưu ý bảo mật**

- Hiện mẫu này cho phép bất kỳ tài khoản đăng nhập nào để quản trị. Hãy sử dụng quy tắc Realtime Database để giới hạn quyền chỉ cho UID admin hoặc cấu hình Cloud Functions để kiểm tra role.
- Đừng commit `apiKey` ra public repo nếu không muốn công khai cấu hình (tuy `apiKey` web là không bí mật, vẫn cần bảo vệ rules).

Nếu bạn muốn, tôi có thể:

- Thêm upload hình qua Firebase Storage.
- Bổ sung role/permission để chỉ UID nhất định được quyền quản trị.
- Triển khai admin lên Firebase Hosting.
