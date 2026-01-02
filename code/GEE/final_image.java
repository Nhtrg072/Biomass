var geometry = ee.FeatureCollection("projects/ee-huutruongnb/assets/taybac");
Map.centerObject(geometry);

var year = 2019;

var startDate = ee.Date.fromYMD(year, 01, 01);
var endDate = ee.Date.fromYMD(year, 12, 31);

var finalPredictors = [
        'B12_mean_7x7', 'B5', 'B8_contrast_7x7', 'B9_contrast_7x7', 'CRI_mean_7x7', 'MTCI', 
        'MTCI_mean_7x7', 'NDMI', 'NDRE1', 'VV', 'VH_contrast_7x7', 'dem', 'ratio_variance_7x7', 'tri']

// =========================================================================
// PHẦN 1: TIỀN XỬ LÝ DỮ LIỆU S1 VÀ S2
// =========================================================================

//------------------------------
// Xử lý dữ liệu Sentinel-2
//------------------------------

var s2 = ee.ImageCollection("COPERNICUS/S2_SR_HARMONIZED");
var filteredS2 = s2.filterBounds(geometry)
                   .filterDate(startDate, endDate);

var s2Projection = ee.Image(filteredS2.first()).select('B4').projection();
var scaleBands = function(image) {
  return image.multiply(0.0001).copyProperties(image, ['system:time_start']);
};

var csPlus = ee.ImageCollection('GOOGLE/CLOUD_SCORE_PLUS/V1/S2_HARMONIZED');
var csPlusBands = csPlus.first().bandNames();
var filteredS2WithCs = filteredS2.linkCollection(csPlus, csPlusBands);

var maskLowQA = function(image) {
  var mask = image.select('cs').gte(0.5);
  return image.updateMask(mask);
};

var addIndices = function(image) {
  var ndmi = image.normalizedDifference(['B8', 'B11']).rename('NDMI');

  var ndre1 = image.normalizedDifference(['B8', 'B5']).rename('NDRE1');

  var mtci = image.expression(
      '(RE2 - RE1) / (RE1 - RE0)', {
        'RE2': image.select('B7'), 'RE1': image.select('B6'), 'RE0': image.select('B5')
      }).rename('MTCI');

  var ireci = image.expression(
      '(RE3 - RED) / (RE1 / RE2)', {
        'RE3': image.select('B7'), 'RED': image.select('B4'), 'RE1': image.select('B5'), 'RE2': image.select('B6')
      }).rename('IRECI');

  var evi = image.expression(
      '2.5 * ((NIR - RED) / (NIR + 6 * RED - 7.5 * BLUE + 1))', {
        'NIR': image.select('B8'), 'RED': image.select('B4'), 'BLUE': image.select('B2')
      }).rename('EVI');

  var alpha = 0.2;
  var wdrvi = image.expression(
      '(alpha * NIR - RED) / (alpha * NIR + RED)', {
        'NIR': image.select('B8'), 'RED': image.select('B4'), 'alpha': alpha
      }).rename('WDRVI');

  var sipi = image.expression(
      '(NIR - BLUE) / (NIR - RED)', {
        'NIR': image.select('B8'), 'BLUE': image.select('B2'), 'RED': image.select('B4')
      }).rename('SIPI');

  var ari = image.expression(
      '(1 / GREEN) - (1 / RE1)', {
        'GREEN': image.select('B3'), 'RE1': image.select('B5')
      }).rename('ARI');

  var cri = image.expression(
      '(1 / BLUE) - (1 / RE1)', {
        'BLUE': image.select('B2'), 'RE1': image.select('B5')
      }).rename('CRI');

  return image.addBands([
      ndmi, ndre1, mtci, ireci, evi, wdrvi, sipi, ari, cri
  ]);
};

var s2Processed = filteredS2WithCs
  .map(maskLowQA)
  .select('B.*')
  .map(scaleBands)
  .map(addIndices);

var s2Composite = s2Processed.median()  
  .setDefaultProjection(s2Projection)  
  .clip(geometry); 

Map.addLayer(s2Composite, {bands: ['B4', 'B3', 'B2'], min: 0, max: 0.3}, 'Sentinel-2 Composite (RGB)');

// ------------------------------
// Xử lý dữ liệu Sentinel-1
// ------------------------------
var s1Collection = ee.ImageCollection('COPERNICUS/S1_GRD')
    .filter(ee.Filter.date(startDate, endDate))
    .filter(ee.Filter.bounds(geometry))
    .filter(ee.Filter.listContains('transmitterReceiverPolarisation', 'VV'))
    .filter(ee.Filter.listContains('transmitterReceiverPolarisation', 'VH'))
    .filter(ee.Filter.eq('instrumentMode', 'IW'))
    .filter(ee.Filter.eq('orbitProperties_pass', 'ASCENDING'));

var speckleFilter = function(image) {
  return image.focal_median({radius: 20, units: 'meters'})
              .copyProperties(image, ['system:time_start']);
};
var s1Filtered = s1Collection.map(speckleFilter);

var s1Composite = s1Filtered.median().clip(geometry);
var ratio = s1Composite.select('VH').divide(s1Composite.select('VV')).rename('ratio');
var s1Base = s1Composite.addBands(ratio);

// ------------------------------
// Gộp S1 và S2 lại
// ------------------------------
var combinedImage = s1Base.addBands(s2Composite);


// =========================================================================
// PHẦN 2: HÀM TÍNH TOÁN TEXTURE GLCM
// =========================================================================

var calculateAndAddAllTextures = function(image, bandList, windowRadius) {
  
  var windowSize = 2 * windowRadius + 1;
  var windowSizeStr = ee.String('_' + windowSize + 'x' + windowSize);

  var allTextureBands = ee.Image(bandList.map(function(bandName) {
    bandName = ee.String(bandName);
    
    var inputBand = image.select(bandName).multiply(1000).toInt16();
    
    // Tính toán GLCM
    var glcm = inputBand.glcmTexture({size: windowRadius});
    
    var contrast = glcm.select(bandName.cat('_contrast')).rename(bandName.cat('_contrast').cat(windowSizeStr));
                       
    var corr = glcm.select(bandName.cat('_corr')).rename(bandName.cat('_corr').cat(windowSizeStr));
                   
    var variance = glcm.select(bandName.cat('_var')).rename(bandName.cat('_variance').cat(windowSizeStr));
                       
    var homogeneity = glcm.select(bandName.cat('_idm')).rename(bandName.cat('_homogeneity').cat(windowSizeStr));
                          
    var entropy = glcm.select(bandName.cat('_ent')).rename(bandName.cat('_entropy').cat(windowSizeStr));
                      
    var kernel = ee.Kernel.square({radius: windowRadius, units: 'pixels'});
    var mean = inputBand.reduceNeighborhood({
      reducer: ee.Reducer.mean(),
      kernel: kernel
    }).rename(bandName.cat('_mean').cat(windowSizeStr));
    
     return ee.Image.cat([contrast, corr, variance, homogeneity, entropy, mean]);
  }));
  
  return image.addBands(allTextureBands);
};

// =========================================================================
//  Cấu hình các band cần tính texture và gộp ảnh
// =========================================================================

var bandsToProcess = [
  'ARI', 'B1', 'B11', 'B12', 'B2', 'B5', 'B4', 'B6', 'B8', 'B9', 
  'CRI', 'EVI', 'IRECI', 'MTCI', 'NDMI', 'NDRE1', 'SIPI', 
  'VH', 'VV', 'ratio'
];

var KERNEL_RADIUS = 3; 

var finalImage = calculateAndAddAllTextures(combinedImage, bandsToProcess, KERNEL_RADIUS);

// =========================================================================
// PHẦN 3: ĐỊA HÌNH
// =========================================================================

var glo30 = ee.ImageCollection('COPERNICUS/DEM/GLO30');
var glo30Filtered = glo30.filter(ee.Filter.bounds(geometry)).select('DEM');

var demProj = glo30Filtered.first().select(0).projection();

var elevation = glo30Filtered.mosaic().rename('dem') 
  .setDefaultProjection(demProj)
  .clip(geometry);

var aspect = ee.Terrain.aspect(elevation).rename('aspect');

var hillshade = ee.Terrain.hillshade(elevation).rename('hillshade');

var tri = elevation.reduceNeighborhood({
  reducer: ee.Reducer.stdDev(),
  kernel: ee.Kernel.square({radius: 1})
}).rename('tri');

var demBands = elevation.addBands(slope)
                        .addBands(aspect)
                        .addBands(hillshade)
                        .addBands(tri);
                        
finalImage = finalImage.addBands(demBands);

// =========================================================================
// PHẦN 4: LỰA CHỌN BANDS VÀ XUẤT ẢNH
// =========================================================================
var exportPath = 'projects/ee-huutruongnb/assets/data/';

var imageToExport = finalImage.select(finalPredictors);

print("Các band trong ảnh cuối cùng sẽ được xuất:", imageToExport.bandNames());

// Xuất ảnh ra Assets
Export.image.toAsset({
  image: imageToExport,
  description: 'image_optimized_' + year,
  assetId: exportPath + 'image_' + year, 
  region: geometry, 
  scale: 30, 
  crs: 'EPSG:32648', 
  maxPixels: 1e10 
});

// Xuất ảnh ra Google Drive
var image_Export_to_drive = imageToExport.toFloat();
Export.image.toDrive({
  image: image_Export_to_drive,
  description: 'image_' + year + '_drive',
  folder: 'data', 
  fileNamePrefix: 'image_' + year,
  region: geometry, 
  scale: 30, 
  crs: 'EPSG:32648', 
  maxPixels: 1e13
});
