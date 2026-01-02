// =========================================================================
// PHẦN 1: KHAI BÁO BAN ĐẦU VÀ TẢI DỮ LIỆU
// =========================================================================
var geometry = ee.FeatureCollection("projects/ee-huutruongnb/assets/taybac");
Map.centerObject(geometry);

var exportPath = 'projects/ee-huutruongnb/assets/data/';
var exportPath1 = 'projects/ee-huutruongnb/assets/dienbien_test/';

var season = 'dry';
var year = 2019;
var part = 1;

// Input data 

var topography = ee.Image(exportPath + 'topography');
var gediClean = ee.FeatureCollection(exportPath + 'l4a_part' + part);

var final_Image = ee.Image(exportPath + 'agbd_' + year + '_all' ).rename('agbd_predict');
var dist_rivers = ee.Image(exportPath + 'distance_to_rivers').rename('Dist_Rivers');
var dist_urban = ee.Image(exportPath + 'distance_to_urban_2019').rename('Dist_Urban');
var climate_change = ee.Image(exportPath + 'climate_change_2019_2024');

// Scale và Projection
var gridScale = 30;
var gridProjection = ee.Projection('EPSG:32648').atScale(gridScale);


// =========================================================================
// PHẦN 2: CHUẨN BỊ DỮ LIỆU VÀ CÁC BIẾN
// =========================================================================

// // DATA TÍNH TEXTURES
// var stackedResampled = s2Composite.addBands(s1Composite)
//                                   .addBands(demBands)
//                                   .resample('bilinear')
//                                   .setDefaultProjection(gridProjection);
                                  
// var predictors = s2Composite.bandNames()
//                             .cat(demBands.bandNames())
//                             .cat(s1Composite.bandNames()); // lọc bands = mttq để tính texture

// DATA CÓ TEXTURES
var stackedResampled = final_Image.addBands(topography)
                                  .addBands(dist_rivers)
                                  .addBands(dist_urban)
                                  .addBands(climate_change)
                                  .resample('bilinear')
                                  .setDefaultProjection(gridProjection);

var predictors = final_Image.bandNames()
                            .cat(topography.bandNames())
                            .cat(dist_rivers.bandNames())
                            .cat(dist_urban.bandNames())
                            .cat(climate_change.bandNames()); // lọc = mttq lần 2 để chạy rfe


var predicted = 'agbd';

// =========================================================================
// PHẦN 3: TẠO DỮ LIỆU TRAINING 
// =========================================================================

print('Số điểm GEDI ban đầu:', gediClean.size());
var finalPoints = gediClean;
print('Số điểm sử dụng để training:', finalPoints.size());

var trainingData = stackedResampled.reduceRegions({
  collection: finalPoints,
  reducer: ee.Reducer.first(),
  scale: gridScale
}).filter(ee.Filter.notNull(predictors.add(predicted)));

print('Tổng số mẫu huấn luyện:', trainingData.size());

// =========================================================================
// PHẦN 4: XUẤT DỮ LIỆU ĐỂ PHÂN TÍCH TƯƠNG QUAN
// =========================================================================

// Export the final training data FeatureCollection to Google Drive
Export.table.toDrive({
  collection: trainingData,
  description: 'Export_TrainingData',
  folder: 'data',
  // fileNamePrefix: 'training_data_'+ season + 'for_textures',
  fileNamePrefix: 'training_data_'+ season + '_for_optuna_rf_part' + part,
  fileFormat: 'CSV'
});