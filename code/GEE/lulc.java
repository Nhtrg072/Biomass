package GEE;
var year = 2021;
var image = ee.Image('projects/ee-huutruongnb/assets/lulc_raw/' + year).clip(geometry);

// hiển thị ảnh LULC
var lulcPalette = [
  '#1A5BAB', // 1: Water
  '#358221', // 2: Trees
  '#A7D282', // 3: Grass
  '#87D19E', // 4: Flooded Vegetation
  '#FFDB5C', // 5: Crops
  '#EECFA8', // 6: Scrub/Shrub
  '#ED022A', // 7: Built Area
  '#EDE9E4', // 8: Bare Ground
  '#F2FAFF', // 9: Snow/Ice
  '#C8C8C8',  // 10: Clouds
  '#FFFFFF' // 11: Rangelands
];


var vizParams = {
  min: 1,
  max: 11,
  palette: lulcPalette
};

var forestMask = image.eq(2);

Map.centerObject(image, 9);
Map.addLayer(image, vizParams, 'Bản đồ LULC');
Map.addLayer(forestMask, {min: 0, max: 1, palette: ['black', 'green']}, 'Forest Mask (Trees only)');

// TẠO MẶT NẠ RỪNG ỔN ĐỊNH 
var years = [year, (year+1)];
var lulcCollection = ee.ImageCollection.fromImages(
  years.map(function(year) {
    var image = ee.Image('projects/ee-huutruongnb/assets/lulc/LULC_' + year);
    return image.set('year', year)
                .set('system:time_start', ee.Date.fromYMD(year, 1, 1));
  })
);
var stableForestMask = lulcCollection.map(function(image) {
  return image.eq(2); // Lớp 2 là rừng
}).reduce(ee.Reducer.min());

Map.addLayer(stableForestMask, {min: 0, max: 1, palette: ['white', 'green']}, 'Mat na Rung On dinh');

var exportPath = 'projects/ee-huutruongnb/assets/lulc/';
Export.image.toAsset({
    image: forestMask,
    description: 'LULC_Export',
    assetId: exportPath + 'LULC_'+year, 
    region: geometry, 
    scale: 30, 
    crs: 'EPSG:32648', 
    maxPixels: 1e10
});

