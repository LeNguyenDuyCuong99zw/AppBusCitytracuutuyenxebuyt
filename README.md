# 🚍 BusCity – Ứng dụng Tra Cứu Xe Buýt Thông Minh

BusCity là ứng dụng hỗ trợ người dùng tra cứu tuyến xe buýt, xem lộ trình, theo dõi tin tức giao thông và lưu các tuyến yêu thích.  
Ứng dụng được xây dựng bằng **Kotlin + Jetpack Compose**, tích hợp **Mapbox/MapLibre**, **Firebase**, và **Room Database** và WebAdmin **Firebase**

---

## ✨ Tính năng chính

### 🔍 Tra cứu tuyến xe buýt

- Xem danh sách tuyến, thông tin chi tiết.
- Lộ trình lượt đi – lượt về đầy đủ.
- Tìm kiếm theo mã hoặc tên tuyến.

### 🗺️ Bản đồ tương tác

- **MapScreen**: hiển thị vị trí hiện tại + trạm gần bạn.
- **RouteDetailMapScreen**: hiển thị polyline + marker dừng.
- **StopMapScreen**: xem chi tiết từng điểm dừng.
- Tối ưu hiệu suất khi hiển thị nhiều marker/polylines.

### ❤️ Tuyến yêu thích

- Lưu tuyến bằng **Room Database**.
- UI dạng grid, animation mượt mà.

### 📰 Tin tức giao thông

- Lấy dữ liệu realtime từ Firebase.
- Hiển thị bằng LazyColumn + ảnh + card đẹp.

### 🔐 Xác thực người dùng

- Đăng nhập/đăng ký bằng Firebase Authentication.
- Lưu session người dùng, cập nhật hồ sơ.

### 📍 Vị trí & Quyền truy cập

- Module xử lý LocationPermission riêng.
- Tự động phát hiện điểm dừng gần vị trí hiện tại.

---

## 🛠️ Công nghệ sử dụng

| Thành phần       | Công nghệ                                  |
| ---------------- | ------------------------------------------ |
| UI               | Jetpack Compose, Material 3                |
| Navigation       | Compose Navigation                         |
| Bản đồ           | Mapbox / MapLibre SDK                      |
| Local Storage    | Room Database                              |
| Backend          | Firebase Authentication, Realtime Database |
| State Management | ViewModel + StateFlow/Livedata             |
| Build            | Gradle KTS                                 |

---

## 👨‍💻 Thành viên thực hiện

---

### **Lê Nguyễn Duy Cường**

**MSSV: 080205008616**

- Xây dựng toàn bộ hệ thống ứng dụng bao gồm Backend (Room Database, Repository, ViewModel), Frontend logic và Web Admin (Node.js/Express).
- Phát triển các chức năng chính: Tra cứu tuyến, Tìm đường đi thông minh, Quản lý yêu thích, Hoạt động offline.
- Tích hợp các API bên ngoài:
  - **OSRM** để tính lộ trình
  - **MapTiler** và **OpenCage** để geocoding
  - **Firebase** để xác thực và lưu trữ dữ liệu
- Xây dựng thuật toán tìm tuyến với hệ thống tính điểm ưu tiên, công thức **Haversine** tính khoảng cách, xử lý bất đồng bộ bằng **Kotlin Coroutines**, caching đa tầng.
- Cùng với Đạt phát triển chức năng **Xem trạm xung quanh** sử dụng GPS, tính khoảng cách và hiển thị danh sách trạm.
- Cùng với Đạt và Khang phát triển giao diện Jetpack Compose:
  - **HomeScreen**, **SearchScreen**, **BusRouteDetailScreen**, **RouteResultsScreen**
- Soạn thảo tài liệu và viết báo cáo bằng **LaTeX/Markdown**.

---

### **Phạm Võ Thành Đạt**

**MSSV: 080205008151**

- Chịu trách nhiệm chính về hệ thống xác thực: LoginActivity, RegisterActivity với Firebase Authentication, validation đầu vào, quản lý session và token.
- Phát triển quản lý tài khoản: **ProfileScreen**, **SettingsScreen**, **AccountPreferences**.
- Cùng với Cường phát triển tính năng **Xem trạm xung quanh**: thiết kế giao diện **StopMapScreen**, xử lý **LocationPermission**, hiển thị thông tin chi tiết trạm.
- Cùng với Cường và Khang phát triển UI Compose:
  - **SplashScreen**, theme, color, typography (Material Design 3).
- Đảm bảo nội dung hiển thị rõ ràng, chính xác, dễ tiếp cận.

---

### **Phạm Hoài Khang**

**MSSV: 080205006382**

- Thiết kế giao diện Jetpack Compose với hệ thống icon rõ ràng, bảng màu hài hòa, bố cục trực quan.
- Cùng với Cường và Đạt phát triển các màn hình chính:
  - **HomeScreen** (Bottom Navigation)
  - **SearchScreen**
  - **BusRouteDetailScreen**
  - **LocationPermission**
- Lập trình giao diện bản đồ tương tác (Mapbox/MapLibre):
  - **MapScreen**, **RouteDetailMapScreen**, **StopMapScreen**
  - Hiển thị polyline, markers và tối ưu rendering.
- Phát triển giao diện tin tức:
  - **NewsScreen**: card tin tức, hình ảnh, chia sẻ, lưu tin.
- Phát triển chức năng yêu thích:
  - **FavoriteScreen** với Room Database.
- Thiết kế Web Admin (HTML/CSS) quản lý tuyến, trạm, tin tức.
- Đảm bảo responsive design và tối ưu hiệu suất.

---

## 1. 📥 Tải ứng dụng

Quét mã QR để tải file apk:

<p align="center">
  <img src="https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=https://drive.google.com/uc?id=17Kzvg-sokOV1GkapZTECVHmC-coKrx8Z&export=download" alt="Download BusCity APK" />
</p>
## 📥 Clone & Chạy Dự Án BusCity

### 2. Clone mã nguồn & mở dự án

```bash
git clone https://github.com/LeNguyenDuyCuong99zw/AppBusCitytracuutuyenxebuyt.git
cd AppBusCitytracuutuyenxebuyt

1 Mở Android Studio

2 Chọn Open → chọn thư mục dự án vừa clone

3 Chờ Gradle Sync hoàn tất


```
