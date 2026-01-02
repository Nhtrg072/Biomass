Đây là 3 code chạy trên GEE

final_image gồm 4 phần:
1. tiền xử lí sentinel: lọc mây, lọc nhiễu, tính toán chỉ số.... và gộp thành 1 ảnh composite
2. tính texture và gộp với ảnh composite 
    (danh sách tính texture: chạy code ma trận tương quan lần 1) 
3. tiền xử lí địa hình topography
4. gộp lại và xuất ảnh ra assets hoặc drive 
    (danh sách các biến: chạy code ma trận tương quan lần 2) 

lulc dùng để hiển thị các lớp lulc và xuất mặt nạ rừng ổn định qua các năm 

trainingdata dùng để xuất dữ liệu để lọc tương quan và training ra file csv 
điều chỉnh code cho phù hợp từng chức năng
1. khai báo ảnh gộp s1, s2 sau khi tiền xử lí để lọc tương quan lần 1 và dùng danh sách đó để tính texture
2. khai báo ảnh final_image là ảnh gộp s1, s2 đã tính texture và địa hình để xuất ra training data
3. khai báo các dữ liệu training cho dự đoán sinh khối tương lai để xuất ra csv 