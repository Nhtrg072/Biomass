# Biomass
Calculate aboveground biomass density 

## PHẦN A: TIỀN XỬ LÝ DỮ LIỆU GEDI (File 1-4)

### `1_matrantuongquan1`
- Tính ma trận tương quan giữa các biến
- Vẽ heatmap tương quan
- Tìm các cặp biến có tương quan cao (> 0.95)

### `2_matrantuongquan2`
- Phân tích tương quan và loại bỏ biến dư thừa
- Xuất file CSV với biến đã chọn

### `3_loc_nhieu_GEDI`
- Lọc nhiễu GEDI bằng LightGBM
- Loại bỏ % điểm có error cao nhất

### `4_loc_duplicates_GEDI`
- Xử lý duplicates trong dữ liệu
- Aggregate bằng P90
- Phân tích độ biến thiên

---

## PHẦN B: ĐÀO TẠO MÔ HÌNH 2019 (File 5-9)

### `5_cell1_colab_khai_bao`
- Cài thư viện, mount Drive
- Khai báo hàm và biến

### `6_cell2_colab_RFE_Optuna'`
- RFE (Recursive Feature Elimination)
- Tối ưu hyperparameters bằng Optuna
- Chọn features tốt nhất

### `7_cell3_colab_train_mo_hinh`
- Train model LightGBM với features đã chọn
- Đánh giá trên train/valid/test

### `8_cell4_colab_Scatter_Plots`
- Vẽ scatter plots với density
- Font Times New Roman

### `9_cell5_colab_create_map_2019_2025`
- Dự đoán bản đồ AGBD cho năm 2019-2025
- Xử lý theo chunks

### `bonus_merge_sentinel`
- Ghép nhiều file Sentinel thành 1 file
- Ghi tên bands
- Nén LZW

---

## PHẦN C: DỰ BÁO TƯƠNG LAI (File 10-16)

### `10_setup_colab`
- Kết nối Google Drive
- Cài thư viện: lightgbm, optuna, rasterio
- Chạy đầu tiên 1 lần

### `11_cell1_load_eda`
- Load dữ liệu training
- Phân tích thống kê, outliers
- Vẽ biểu đồ Delta AGB

### `12_cell2_loc_nhieu`
- Lọc nhiễu bằng residual

### `13_cell3_optuna`
- Tối ưu hyperparameters

### `14_cell4_train_model`
- Train model cuối cùng
- Vẽ scatter plots

### `15_cell5_prediction`
- Dự đoán bản đồ sinh khối

### `16_cell5_5_post_process`
- Lọc theo LULC

### Vòng lặp dự báo nhiều năm
1. Cell 15: `STARTYEAR = 2025` → Cell 16 → tạo file 2030
2. Cell 15: `STARTYEAR = 2030`, cập nhật `agbd_predict` → Cell 16 → tạo file 2035
3. Lặp lại...
