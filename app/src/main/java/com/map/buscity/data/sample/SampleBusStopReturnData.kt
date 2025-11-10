package com.map.buscity.data.sample

import com.map.buscity.data.BusStopReturn

object SampleBusStopReturnData {
    fun getSampleReturnStops() = listOf(
        // For simplicity, define a few return stops (reverse direction) for route 01 and others
        // Route 01 return (Bến xe buýt Chợ Lớn -> Bến Thành)
BusStopReturn(routeNumber = "01", stopName = "Bến xe buýt Chợ Lớn", lat = 10.751246, lng = 106.652693, stopOrder = 1),
BusStopReturn(routeNumber = "01", stopName = "Tháp Mười", lat = 10.750334, lng = 106.653093, stopOrder = 2),
BusStopReturn(routeNumber = "01", stopName = "Chợ Kim Biên", lat = 10.750689, lng = 106.655086, stopOrder = 3),
BusStopReturn(routeNumber = "01", stopName = "Bưu điện Quận 5", lat = 10.751372, lng = 106.659219, stopOrder = 4),
BusStopReturn(routeNumber = "01", stopName = "Rạp Đại Quang", lat = 10.752524, lng = 106.658897, stopOrder = 5),
BusStopReturn(routeNumber = "01", stopName = "Lương Nhữ Học", lat = 10.752951, lng = 106.660423, stopOrder = 6),
BusStopReturn(routeNumber = "01", stopName = "Triệu Quang Phục", lat = 10.753077, lng = 106.662081, stopOrder = 7),
BusStopReturn(routeNumber = "01", stopName = "Đại học Sư phạm Thể dục thể thao", lat = 10.753315, lng = 106.663502, stopOrder = 8),
BusStopReturn(routeNumber = "01", stopName = "Siêu thị Điện máy Chợ Lớn", lat = 10.753591, lng = 106.664919, stopOrder = 9),
BusStopReturn(routeNumber = "01", stopName = "Ngô Quyền", lat = 10.753952, lng = 106.666933, stopOrder = 10),
BusStopReturn(routeNumber = "01", stopName = "Chung cư Nguyễn Trãi", lat = 10.754287, lng = 106.668577, stopOrder = 11),
BusStopReturn(routeNumber = "01", stopName = "Bệnh viện Nguyễn Tri Phương", lat = 10.754601, lng = 106.670237, stopOrder = 12),
BusStopReturn(routeNumber = "01", stopName = "Trần Phú", lat = 10.755241, lng = 106.673223, stopOrder = 13),
BusStopReturn(routeNumber = "01", stopName = "Bệnh viện Nguyễn Trãi", lat = 10.755784, lng = 106.675114, stopOrder = 14),
BusStopReturn(routeNumber = "01", stopName = "Bệnh viện Chấn Thương Chỉnh hình", lat = 10.75433, lng = 106.678298, stopOrder = 15),
BusStopReturn(routeNumber = "01", stopName = "Trần Bình Trọng", lat = 10.754871, lng = 106.680065, stopOrder = 16),
BusStopReturn(routeNumber = "01", stopName = "Nguyễn Biểu", lat = 10.756015, lng = 106.684402, stopOrder = 17),
BusStopReturn(routeNumber = "01", stopName = "Chợ Nanci", lat = 10.757117, lng = 106.686044, stopOrder = 18),
BusStopReturn(routeNumber = "01", stopName = "Tổng Cty Samco", lat = 10.759726, lng = 106.688324, stopOrder = 19),
BusStopReturn(routeNumber = "01", stopName = "Sở PCCC", lat = 10.760655, lng = 106.689134, stopOrder = 20),
BusStopReturn(routeNumber = "01", stopName = "Hồ Hảo Hớn", lat = 10.762414, lng = 106.690695, stopOrder = 21),
BusStopReturn(routeNumber = "01", stopName = "Bệnh Viện Răng Hàm Mặt", lat = 10.763566, lng = 106.691674, stopOrder = 22),
BusStopReturn(routeNumber = "01", stopName = "Rạp Trần Hưng Đạo", lat = 10.764771, lng = 106.692741, stopOrder = 23),
BusStopReturn(routeNumber = "01", stopName = "KTX Trần Hưng Đạo", lat = 10.767128, lng = 106.694788, stopOrder = 24),
BusStopReturn(routeNumber = "01", stopName = "Nguyễn Kim", lat = 10.768325, lng = 106.695904, stopOrder = 25),
BusStopReturn(routeNumber = "01", stopName = "Trạm Trung chuyển trên đường Hàm Nghi", lat = 10.770811, lng = 106.700766, stopOrder = 26),
BusStopReturn(routeNumber = "01", stopName = "Nam Kỳ Khởi Nghĩa", lat = 10.770769, lng = 106.701523, stopOrder = 27),
BusStopReturn(routeNumber = "01", stopName = "Chợ Cũ", lat = 10.770745, lng = 106.702081, stopOrder = 28),
BusStopReturn(routeNumber = "01", stopName = "Giao lộ Hàm Nghi - Tôn Thất Đạm", lat = 10.770687, lng = 106.703095, stopOrder = 29),
BusStopReturn(routeNumber = "01", stopName = "Hồ Tùng Mậu", lat = 10.770705, lng = 106.704063, stopOrder = 30),
BusStopReturn(routeNumber = "01", stopName = "Bến Bạch Đằng", lat = 10.774108, lng = 106.706678, stopOrder = 31),
BusStopReturn(routeNumber = "01", stopName = "Công Trường Mê Linh", lat = 10.776093, lng = 106.70575, stopOrder = 32),
BusStopReturn(routeNumber = "01", stopName = "Công Trường Mê Linh", lat = 10.776865, lng = 106.705793, stopOrder = 33),


        // Add a few simple return stops for route 03
        BusStopReturn(routeNumber = "03", stopName = "Thạnh Xuân", lat = 10.8620, lng = 106.7020, stopOrder = 1),
        BusStopReturn(routeNumber = "03", stopName = "Gò Vấp", lat = 10.8362, lng = 106.6853, stopOrder = 2),
        BusStopReturn(routeNumber = "03", stopName = "Ngã tư Bảy Hiền", lat = 10.7990, lng = 106.6728, stopOrder = 3),
        BusStopReturn(routeNumber = "03", stopName = "Công viên 23/9", lat = 10.7712, lng = 106.6932, stopOrder = 4),
        BusStopReturn(routeNumber = "03", stopName = "Bến Thành", lat = 10.7722, lng = 106.6986, stopOrder = 5)
    )
}
