/*
 * 3DCityDB-Web-Map-Client
 * http://www.3dcitydb.org/
 * 
 * Copyright 2015 - 2017
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 * 
 * The 3DCityDB-Web-Map-Client is jointly developed with the following
 * cooperation partners:
 * 
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**-----------------------------------------Separate Line-------------------------------------------------**/

// URL controller
var urlController = new UrlController();

/*---------------------------------  set globe variables  ----------------------------------------*/
// BingMapsAPI Key for Bing Imagery Layers and Geocoder
// If this is not valid, the Bing Imagery Layers will be removed and the Bing Geocoder will be replaced with OSM Nominatim
var bingToken = urlController.getUrlParaValue('bingToken', window.location.href, CitydbUtil);
if (Cesium.defined(bingToken) && bingToken !== "") {
    Cesium.BingMapsApi.defaultKey = bingToken;
}

// Define clock to be animated per default
var clock = new Cesium.Clock({
    shouldAnimate: true
});

// create 3Dcitydb-web-map instance
var shadows = urlController.getUrlParaValue('shadows', window.location.href, CitydbUtil);
var terrainShadows = urlController.getUrlParaValue('terrainShadows', window.location.href, CitydbUtil);
var geeMetadata = new Cesium.GoogleEarthEnterpriseMetadata('http://www.earthenterprise.org/3d');
var gee = new Cesium.GoogleEarthEnterpriseTerrainProvider({
    metadata : geeMetadata
});
var maptiler = new Cesium.CesiumTerrainProvider({
    url: 'https://api.maptiler.com/tiles/terrain-quantized-mesh-v2/?key=TRuPEmfpRFg51wlyVA8c',
    credit: new Cesium.Credit("\u003ca href=\"https://www.maptiler.com/copyright/\" target=\"_blank\"\u003e\u0026copy;MapTiler\u003c/a\u003e \u003ca href=\"https://www.openstreetmap.org/copyright\" target=\"_blank\"\u003e\u0026copy; OpenStreetMap contributors\u003c/a\u003e", true),
    requestVertexNormals: true
});

var cesiumViewerOptions = {
    selectedImageryProviderViewModel: Cesium.createDefaultImageryProviderViewModels()[1],
    timeline: false,
    animation: false,
    fullscreenButton: false,
    shadows: (shadows == "true"),
    terrainShadows: parseInt(terrainShadows),
    clockViewModel: new Cesium.ClockViewModel(clock),
    terrainProvider: maptiler
}


// If neither BingMapsAPI key nor ionToken is present, use the OpenStreetMap Geocoder Nominatim
var ionToken = urlController.getUrlParaValue('ionToken', window.location.href, CitydbUtil);
if (Cesium.defined(ionToken) && ionToken !== "") {
    Cesium.Ion.defaultAccessToken = ionToken;
}
if ((!Cesium.defined(Cesium.BingMapsApi.defaultKey) || Cesium.BingMapsApi.defaultKey === "")
    && (!Cesium.defined(ionToken) || ionToken === "")) {
    cesiumViewerOptions.geocoder = new OpenStreetMapNominatimGeocoder();
}

var cesiumViewer = new Cesium.Viewer('cesiumContainer', cesiumViewerOptions);

adjustIonFeatures();

navigationInitialization('cesiumContainer', cesiumViewer);

var cesiumCamera = cesiumViewer.scene.camera;
var webMap = new WebMap3DCityDB(cesiumViewer);

// set default input parameter value and bind the view and model
var addLayerViewModel = {
    url: "",
    name: "",
    layerDataType: "",
    layerProxy: false,
    layerClampToGround: true,
    gltfVersion: "",
    thematicDataUrl: "",
    thematicDataSource: "",
    tableType: "",
    // googleSheetsApiKey: "",
    // googleSheetsRanges: "",
    // googleSheetsClientId: "",
    cityobjectsJsonUrl: "",
    minLodPixels: "",
    maxLodPixels: "",
    maxSizeOfCachedTiles: 200,
    maxCountOfVisibleTiles: 200
};
Cesium.knockout.track(addLayerViewModel);
Cesium.knockout.applyBindings(addLayerViewModel, document.getElementById('citydb_addlayerpanel'));

var addWmsViewModel = {
    name: '',
    iconUrl: '',
    tooltip: '',
    url: '',
    layers: '',
    additionalParameters: '',
    proxyUrl: '/proxy/'
};
Cesium.knockout.track(addWmsViewModel);
Cesium.knockout.applyBindings(addWmsViewModel, document.getElementById('citydb_addwmspanel'));

var addTerrainViewModel = {
    name: '',
    iconUrl: '',
    tooltip: '',
    url: ''
};
Cesium.knockout.track(addTerrainViewModel);
Cesium.knockout.applyBindings(addTerrainViewModel, document.getElementById('citydb_addterrainpanel'));

var addSplashWindowModel = {
    url: '',
    showOnStart: ''
};
Cesium.knockout.track(addSplashWindowModel);
Cesium.knockout.applyBindings(addSplashWindowModel, document.getElementById('citydb_addsplashwindow'));

// Splash controller
var splashController = new SplashController(addSplashWindowModel);
splashController.setCookie("ignoreSplashWindow", "true")

/*---------------------------------  Load Configurations and Layers  ----------------------------------------*/

initClient();

// Store clicked entities
var clickedEntities = {};

function initClient() {
    // adjust cesium navigation help popup for splash window
    splashController.insertSplashInfoHelp();
    // read splash window from url
    splashController.getSplashWindowFromUrl(window.location.href, urlController, jQuery, CitydbUtil, Cesium);

    // init progress indicator gif
    document.getElementById('loadingIndicator').style.display = 'none';

    // activate mouseClick Events		
    webMap.activateMouseClickEvents(true);
    webMap.activateMouseMoveEvents(true);
    webMap.activateViewChangedEvent(true);

    // hide Cesium logo
    var textViewer = document.getElementsByClassName("cesium-widget-credits")[0];
    textViewer.parentNode.removeChild(textViewer);

    // activate debug mode
    var debugStr = urlController.getUrlParaValue('debug', window.location.href, CitydbUtil);
    if (debugStr == "true") {
        cesiumViewer.extend(Cesium.viewerCesiumInspectorMixin);
        cesiumViewer.cesiumInspector.viewModel.dropDownVisible = false;
    }

    // set title of the web page
    var titleStr = urlController.getUrlParaValue('title', window.location.href, CitydbUtil);
    if (titleStr) {
        document.title = titleStr;
    }

    // It's an extended Geocoder widget which can also be used for searching object by its gmlid.
    cesiumViewer.geocoder.viewModel._searchCommand.beforeExecute.addEventListener(function (info) {
        var callGeocodingService = info.args[0];
        if (callGeocodingService != true) {
            var gmlId = cesiumViewer.geocoder.viewModel.searchText;
            info.cancel = true;
            cesiumViewer.geocoder.viewModel.searchText = "Searching now.......";
            zoomToObjectById(gmlId, function () {
                cesiumViewer.geocoder.viewModel.searchText = gmlId;
            }, function () {
                cesiumViewer.geocoder.viewModel.searchText = gmlId;
                cesiumViewer.geocoder.viewModel.search.call(this, true);
            });
        }
    });

    // inspect the status of the showed and cached tiles	
    inspectTileStatus();

    // display current infos of active layer in the main menu
    observeActiveLayer();

    // set CIA context based on input in url
    var cia_context = (new URL(window.location.href)).searchParams.get('context');
    this.cia_context = cia_context ? cia_context : '';

    // load city based on input in url
    var city = (new URL(window.location.href)).searchParams.get('city');
    loadCity(city);

    // Zoom to desired camera position and load layers if encoded in the url...
    zoomToDefaultCameraPosition().then(function (info) {
        var layers = urlController.getLayersFromUrl(window.location.href, CitydbUtil, CitydbKmlLayer, Cesium3DTilesDataLayer, Cesium);
        loadLayerGroup(layers);

        var basemapConfigString = urlController.getUrlParaValue('basemap', window.location.href, CitydbUtil);
        if (basemapConfigString) {
            var viewMoModel = Cesium.queryToObject(Object.keys(Cesium.queryToObject(basemapConfigString))[0]);
            for (key in viewMoModel) {
                addWmsViewModel[key] = viewMoModel[key];
            }
            addWebMapServiceProvider();
        }

        var cesiumWorldTerrainString = urlController.getUrlParaValue('cesiumWorldTerrain', window.location.href, CitydbUtil);
        var maptilerTerrainString = urlController.getUrlParaValue('maptiler', window.location.href, CitydbUtil);
        if(cesiumWorldTerrainString == "true") {
            // if the Cesium World Terrain is given in the URL --> activate, else other terrains
            cesiumViewer.terrainProvider = Cesium.createWorldTerrain();
            var baseLayerPickerViewModel = cesiumViewer.baseLayerPicker.viewModel;
            baseLayerPickerViewModel.selectedTerrain = baseLayerPickerViewModel.terrainProviderViewModels[1];
        } else if (maptilerTerrainString == 'true'){
            cesiumViewer.terrainProvider = maptiler;
            var baseLayerPickerViewModel = cesiumViewer.baseLayerPicker.viewModel;
            baseLayerPickerViewModel.selectedTerrain = baseLayerPickerViewModel.terrainProviderViewModels[1];
        }else {
            var terrainConfigString = urlController.getUrlParaValue('terrain', window.location.href, CitydbUtil);
            if (terrainConfigString) {
                var viewMoModel = Cesium.queryToObject(Object.keys(Cesium.queryToObject(terrainConfigString))[0]);
                for (key in viewMoModel) {
                    addTerrainViewModel[key] = viewMoModel[key];
                }
                addTerrainProvider();
            }
        }
    });

    // jump to a timepoint
    var dayTimeStr = urlController.getUrlParaValue('dayTime', window.location.href, CitydbUtil);
    if (dayTimeStr) {
        var julianDate = Cesium.JulianDate.fromIso8601(decodeURIComponent(dayTimeStr));
        var clock = cesiumViewer.cesiumWidget.clock;
        clock.currentTime = julianDate;
        clock.shouldAnimate = false;
    }

    // Bring the cesium navigation help popup above the compass
    var cesiumNavHelp = document.getElementsByClassName("cesium-navigation-help")[0];
    cesiumNavHelp.style.zIndex = 99999;

    // If the web client has a layer, add an onclick event to the home button to fly to this layer
    var cesiumHomeButton = document.getElementsByClassName("cesium-button cesium-toolbar-button cesium-home-button")[0];
    cesiumHomeButton.onclick = function () {
        zoomToDefaultCameraPosition();
    }
}

function loadCity(city) {
    if (city == 'pirmasens') {
        loadPirmasens();
    } else if (city == 'berlin') {
        loadBerlin();
    } else if (city == 'kingslynn') {
        loadKingsLynn();
    } else if (city == 'singaporeEPSG4326') {
        loadSingapore();
    } else if (city == 'jurongisland'){
        loadJurongIsland();
    }
}

function loadPirmasens() {
    // set title
    document.title = 'Pirmasens';

    // set camera view
    var cameraPostion = {
        latitude: 49.194269,
        longitude: 7.5981472,
        height: 800,
        heading: 345.2992773976952,
        pitch: -44.26228062802528,
        roll: 359.933888621294
    }

    // set terrain to maptiler
    cesiumViewer.terrainProvider = maptiler;

    flyToCameraPosition(cameraPostion);

    // find relevant files and load layers
    getAndLoadLayers('exported_pirmasens');
}

function loadBerlin() {
    // set title
    document.title = 'Berlin';

    // set camera view
    var cameraPostion = {
        latitude: 52.517479728958044,
        longitude: 13.411141287558161,
        height: 534.3099172951087,
        heading: 345.2992773976952,
        pitch: -44.26228062802528,
        roll: 359.933888621294
    }
    flyToCameraPosition(cameraPostion);

    // find relevant files and load layers
    getAndLoadLayers('exported_berlin');
}

function loadKingsLynn() {
    // set title
    document.title = 'King\'s Lynn';

    // set camera view
    var cameraPostion = {
        latitude: 52.753,
        longitude: 0.3851230367748717,
        height: 300,
        heading: 0,
        pitch: -44.26228062802528,
        roll: 0
    }
    flyToCameraPosition(cameraPostion);

    // find relevant files and load layers
    getAndLoadLayers('exported_kingslynn');
}

function loadSingapore() {
    // set title
    document.title = 'Singapore';

    // set camera view
    var cameraPostion = {
        latitude: 1.286014,
        longitude: 103.836364,
        height: 2000,
        heading: 345.2992773976952,
        pitch: -44.26228062802528,
        roll: 359.933888621294
    }

    flyToCameraPosition(cameraPostion);

    // find relevant files and load layers
    getAndLoadLayers('exported_singapore');
}

function loadJurongIsland() {
    // set title
    document.title = 'Jurong Island';

    // set camera view
    var cameraPostion = {
        latitude: 1.254414386242766,
        longitude: 103.66773374157039,
        height: 500,
        heading: 345.2992773976952,
        pitch: -44.26228062802528,
        roll: 359.933888621294
    }

    flyToCameraPosition(cameraPostion);

    // find relevant files and load layers
    getAndLoadLayers('exported_jurong_island');
}

function loadSingapore() {
    // set title
    document.title = 'Singapore';


    // set camera view
    var cameraPostion = {
        latitude:  1.279,
        longitude: 103.84,
        height: 2800,
        heading: 360,
        pitch: -60,
        roll: 356,
    }
    flyToCameraPosition(cameraPostion);

    // find relevant files and load layers
    getAndLoadLayers('exported_singapore');
}

// send get request to server to discover files in specified folder, create and load layers
function getAndLoadLayers(folder) {
    var url = this.location.origin;
    var _layers = new Array();
    var folderpath = url + '/3dwebclient/' + folder + '/';
    var filepathname = '';
    var options = {
        url: '',
        name: '',
        layerDataType: 'COLLADA/KML/gTIF',
        layerProxy: false,
        layerClampToGround: false,
        gltfVersion: '2.0',
        thematicDataUrl: '',
        thematicDataSource: 'KML',
        tableType: 'Horizontal',
        cityobjectsJsonUrl: '',
        minLodPixels: '',
        maxLodPixels: '',
        maxSizeOfCachedTiles: 1000,  //200
        maxCountOfVisibleTiles: 1000 //200
    }

    var reqUrl = url + '/files/';

    $.get(reqUrl + folder, function (data) {
        // if data includes 'Tiles', only add MasterJSON as layer, else add all files
        if (data.includes("Tiles")) {
            for (let i = 0; i < data.length; i++) {
                if (data[i].match(new RegExp("\\w*_\\w*_MasterJSON.json"))) {
                    filepathname = folderpath + data[i]
                    options.url = filepathname;
                    options.cityobjectsJsonUrl = folderpath + "test_extruded.json"
                    options.name = (new URL(window.location.href)).searchParams.get('city');
                    _layers.push(new CitydbKmlLayer(options));
                    loadLayerGroup(_layers);
                    break;
                }
            }
        } else {
            for (let i = 0; i < data.length; i++) {
                filepathname = folderpath + data[i];
                options.url = filepathname;
                options.name = data[i];
                _layers.push(new CitydbKmlLayer(options));
                if (i == data.length - 1) {
                    loadLayerGroup(_layers);
                }
            }
        }
    });
}

function observeActiveLayer() {
    var observable = Cesium.knockout.getObservable(webMap, '_activeLayer');

    observable.subscribe(function (selectedLayer) {
        if (Cesium.defined(selectedLayer)) {
            document.getElementById(selectedLayer.id).childNodes[0].checked = true;

            updateAddLayerViewModel(selectedLayer);
        }
    });

    function updateAddLayerViewModel(selectedLayer) {
        addLayerViewModel.url = selectedLayer.url;
        addLayerViewModel.name = selectedLayer.name;
        addLayerViewModel.layerDataType = selectedLayer.layerDataType;
        addLayerViewModel.layerProxy = selectedLayer.layerProxy;
        addLayerViewModel.layerClampToGround = selectedLayer.layerClampToGround;
        addLayerViewModel.gltfVersion = selectedLayer.gltfVersion;
        addLayerViewModel.thematicDataUrl = selectedLayer.thematicDataUrl;
        addLayerViewModel.thematicDataSource = selectedLayer.thematicDataSource;
        addLayerViewModel.tableType = selectedLayer.tableType;
        // addLayerViewModel.googleSheetsApiKey = selectedLayer.googleSheetsApiKey;
        // addLayerViewModel.googleSheetsRanges = selectedLayer.googleSheetsRanges;
        // addLayerViewModel.googleSheetsClientId = selectedLayer.googleSheetsClientId;
        addLayerViewModel.cityobjectsJsonUrl = selectedLayer.cityobjectsJsonUrl;
        addLayerViewModel.minLodPixels = selectedLayer.minLodPixels;
        addLayerViewModel.maxLodPixels = selectedLayer.maxLodPixels;
        addLayerViewModel.maxSizeOfCachedTiles = selectedLayer.maxSizeOfCachedTiles;
        addLayerViewModel.maxCountOfVisibleTiles = selectedLayer.maxCountOfVisibleTiles;
    }
}

function adjustIonFeatures() {
    // If ion token is not available, remove Cesium World Terrain from the Terrain Providers
    if (!Cesium.defined(ionToken) || ionToken === "") {
        var terrainProviders = cesiumViewer.baseLayerPicker.viewModel.terrainProviderViewModels;
        i = 0;
        while (i < terrainProviders.length) {
            if (terrainProviders[i].name.indexOf("Cesium World Terrain") !== -1) {
                //terrainProviders[i]._creationCommand.canExecute = false;
                terrainProviders.remove(terrainProviders[i]);
            } else {
                i++;
            }
        }

        // Set default imagery to an open-source terrain
        cesiumViewer.baseLayerPicker.viewModel.selectedTerrain = terrainProviders[0];
        console.warn("Due to invalid or missing ion access token from user, Cesium World Terrain has been removed. Please enter your ion access token using the URL-parameter \"ionToken=<your-token>\" and refresh the page if you wish to use ion features.");

        // Cesium ion uses Bing Maps by default -> no need to insert Bing token if an ion token is already available

        // If neither BingMapsAPI key nor ion access token is present, remove Bing Maps from the Imagery Providers
        if (!Cesium.defined(Cesium.BingMapsApi.defaultKey) || Cesium.BingMapsApi.defaultKey === "") {
            var imageryProviders = cesiumViewer.baseLayerPicker.viewModel.imageryProviderViewModels;
            var i = 0;
            while (i < imageryProviders.length) {
                if (imageryProviders[i].name.indexOf("Bing Maps") !== -1) {
                    //imageryProviders[i]._creationCommand.canExecute = false;
                    imageryProviders.remove(imageryProviders[i]);
                } else {
                    i++;
                }
            }

            // Set default imagery to ESRI World Imagery
            cesiumViewer.baseLayerPicker.viewModel.selectedImagery = imageryProviders[3];

            // Disable auto-complete of OSM Geocoder due to OSM usage limitations
            // see https://operations.osmfoundation.org/policies/nominatim/#unacceptable-use
            cesiumViewer._geocoder._viewModel.autoComplete = false;

            console.warn("Due to invalid or missing Bing access token from user, all Bing Maps have been removed. Please enter your Bing Maps API token using the URL-parameter \"bingToken=<your-token>\" and refresh the page if you wish to use Bing Maps.");
        } else {
            console.error("A Bing token has been detected. This requires an ion token to display the terrain correctly. Please either remove the Bing token in the URL to use the default terrain and imagery, or insert an ion token in addition to the existing Bing token to use Cesium World Terrain and Bing Maps.")
            CitydbUtil.showAlertWindow("OK", "Error loading terrain", "A Bing token has been detected. This requires an ion token to display the terrain correctly. Please either remove the Bing token in the URL to use the default terrain and imagery, or insert an ion token in addition to the existing Bing token to use Cesium World Terrain and Bing Maps. Please refer to <a href='https://github.com/3dcitydb/3dcitydb-web-map/releases/tag/v1.9.0' target='_blank'>https://github.com/3dcitydb/3dcitydb-web-map/releases/tag/v1.9.0</a> for more information.");
        }
    }
}

/*---------------------------------  methods and functions  ----------------------------------------*/

function inspectTileStatus() {
    setInterval(function () {
        var cachedTilesInspector = document.getElementById('citydb_cachedTilesInspector');
        var showedTilesInspector = document.getElementById('citydb_showedTilesInspector');
        var layers = webMap._layers;
        var numberOfshowedTiles = 0;
        var numberOfCachedTiles = 0;
        var numberOfTasks = 0;
        var tilesLoaded = true;
        for (var i = 0; i < layers.length; i++) {
            var layer = layers[i];
            if (layers[i].active) {
                if (layer instanceof CitydbKmlLayer) {
                    numberOfshowedTiles = numberOfshowedTiles + Object.keys(layers[i].citydbKmlTilingManager.dataPoolKml).length;
                    numberOfCachedTiles = numberOfCachedTiles + Object.keys(layers[i].citydbKmlTilingManager.networklinkCache).length;
                    numberOfTasks = numberOfTasks + layers[i].citydbKmlTilingManager.taskNumber;
                }
                if (layer instanceof Cesium3DTilesDataLayer) {
                    numberOfshowedTiles = numberOfshowedTiles + layer._tileset._selectedTiles.length;
                    numberOfCachedTiles = numberOfCachedTiles + layer._tileset._statistics.numberContentReady;
                    tilesLoaded = layer._tileset._tilesLoaded;
                }
            }
        }
        showedTilesInspector.innerHTML = 'Number of showed Tiles: ' + numberOfshowedTiles;
        cachedTilesInspector.innerHTML = 'Number of cached Tiles: ' + numberOfCachedTiles;

        var loadingTilesInspector = document.getElementById('citydb_loadingTilesInspector');
        if (numberOfTasks > 0 || !tilesLoaded) {
            loadingTilesInspector.style.display = 'block';
        } else {
            loadingTilesInspector.style.display = 'none';
        }
    }, 200);
}

function listHighlightedObjects() {
    var highlightingListElement = document.getElementById("citydb_highlightinglist");

    emptySelectBox(highlightingListElement, function() {
        var highlightedObjects = webMap.getAllHighlightedObjects();
        for (var i = 0; i < highlightedObjects.length; i++) {
            var option = document.createElement("option");
            option.text = highlightedObjects[i];
            highlightingListElement.add(option);
            highlightingListElement.selectedIndex = 0;
        }
    });
}

//---Extended Web-Map-Client part---//
// Ayda
function computeDistance() {
    var iriArr = [];
    var highlightedObjects = webMap.getAllHighlighted3DObjects();
    var centroids = [];
    for (var i = 0; i < highlightedObjects.length; i++) {
        var entity = highlightedObjects[i][0];
        console.log(entity.polygon);
        var positions = entity.polygon.hierarchy._value.positions;
        var center = Cesium.BoundingSphere.fromPoints(positions).center;
        console.log(center);
        centroids.push(center);
        var iri = entity._iriPrefix + entity._name;
        iri = iri.endsWith('/') ? iri : iri + '/';
        iri = iri.replace(new RegExp('_(\\w*Surface)'), '');
        iriArr.push(iri);
    };
    console.log(centroids);

    var redLine = cesiumViewer.entities.add({
        name: "Distance line",
        polyline: {
            positions: centroids,
            width: 2,
            material: new Cesium.PolylineDashMaterialProperty({
                color: Cesium.Color.WHITE,
                dashLength: 15,
            }),
            clampToGround: true,
        }
    });

    var labelText = '';
    var label = cesiumViewer.entities.add({
        name: 'Distance label',
        position: getMidpoint(centroids[0], centroids[1]),
        label: {
            font : 'bold 22px arial',
            showBackground : false,
            heightReference :Cesium.HeightReference.CLAMP_TO_GROUND,
            horizontalOrigin : Cesium.HorizontalOrigin.CENTER,
            verticalOrigin : Cesium.VerticalOrigin.CENTER,
            pixelOffset : new Cesium.Cartesian2(0, 20),
            eyeOffset: new Cesium.Cartesian3(0,0,-50),
            text: labelText,
            fillColor : Cesium.Color.WHITE,
            backgroundColor : new Cesium.Color(0.0, 0.0, 0.0, 0.7),
            showBackground : true,
        }
    });

    jQuery.ajax({
        url:"http://localhost:8080/agents/distance",
        type: 'POST',
        data: JSON.stringify({iris: iriArr}),
        dataType: 'json',
        contentType: 'application/json',
        success: function(data, status_message, xhr){
            console.log(data["distances"]);
            var distance = Math.round(data["distances"][0]);
            label.label.text = distance.toString() + " " + "m";
        }
    });

}

function getMidpoint(point1, point2) {
    var geodesic = new Cesium.EllipsoidGeodesic();
    var scratch = new Cesium.Cartographic();

    var point1cart = Cesium.Cartographic.fromCartesian(point1);
    var point2cart = Cesium.Cartographic.fromCartesian(point2);

    geodesic.setEndPoints(point1cart, point2cart);
    var midpointCartographic = geodesic.interpolateUsingFraction(0.5, scratch);
    return Cesium.Cartesian3.fromRadians(midpointCartographic.longitude, midpointCartographic.latitude);

}

//Shiying: highlight multiple cityobjects
function highlightMultipleObjects(cityObjectsArray){  // citydbKmlLayer object, list of files in the folder--> get the summaryfile
    //var cityObjectsArray = ["UUID_fddf5c91-cdd6-436a-95e6-aa1fa199b75d", "UUID_e5779fd5-ea90-4d2c-9a0a-cf7f46e5aad3"];
    //var cityObjectsArray = ["http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_fddf5c91-cdd6-436a-95e6-aa1fa199b75d/", "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_e5779fd5-ea90-4d2c-9a0a-cf7f46e5aad3/", "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_b6f4d0de-cf5c-4917-aba0-c1a91fa4960b/"];

    // UUID_fddf5c91-cdd6-436a-95e6-aa1fa199b75d - inside
    // UUID_b6f4d0de-cf5c-4917-aba0-c1a91fa4960b - outside of the scene
    var currentLayer = webMap.activeLayer;
    var filteredObjects = {};
    var highlightColor = currentLayer._highlightColor; // new Cesium.Color(16/255, 77/255, 151/255, 1.0);
    // unhighlight all objects first then highlight the filtered objects
    currentLayer.unHighlightAllObjects();

    for (let i = 0; i < cityObjectsArray.length; i++) {
        var strArray = cityObjectsArray[i].split("/");
        var gmlid = strArray[strArray.length-2];
        filteredObjects[gmlid] = new Cesium.Color(65/255, 168/255, 255/255, 0.8);
    }
    /**
    var cameraPostion = {
        latitude:  1.25648566126433,
        longitude: 103.823907400709,
        height: 2800,
        heading: 360,
        pitch: -60,
        roll: 356,
    }
    flyToCameraPosition(cameraPostion); **/
    //flyToMapLocation(1.264377, 103.837302);
    //zoomToObjectById("UUID_fddf5c91-cdd6-436a-95e6-aa1fa199b75d");
    currentLayer.highlight(filteredObjects);
    pinHighlightObjects(cityObjectsArray);
}

function showResultWindow(selectedDevType, resultJson){

    var resultBoxTitle = document.getElementById("resultBox-title");
    resultBoxTitle.style.visibility = "visible";
    var resultBox = document.getElementById("resultBox-iframe");
    resultBox.style.visibility = "visible";
    var resultBoxContent = document.createElement("div");
    resultBoxContent.style.display = "block";
    var listItem = document.createElement("li");

    listItem.appendChild(processInfoContext(selectedDevType, resultJson));

    var closeButton = document.createElement("span");
    closeButton.className = "close";
    closeButton.textContent = "x";

    listItem.appendChild(closeButton);
    resultBoxContent.appendChild(listItem);
    resultBox.appendChild(resultBoxContent);

    var closebtns = document.getElementsByClassName("close");
    var i;

    for (i = 0; i < closebtns.length; i++) {
        closebtns[i].addEventListener("click", function() {
            //this.parentElement.style.display = 'none';
            this.parentNode.remove();
        });
    }
}

// create the summary text for infobox
// resultjson is json object
function processInfoContext(selectedDevType, resultjson){
    var infoText = document.createElement("div");
    var title = document.createElement("span");

    title.textContent = selectedDevType;
    infoText.appendChild(title);
    infoText.appendChild(document.createElement("br"));

     //{"allowsProgramme":{"Cafe":"","StudentRunBusiness":""},"TotalGFA":""}
    var filteredCount = resultjson["http://www.theworldavatar.com:83/access-agent/access"]["filteredCounts"];
    var inputs = resultjson["context"]["http://www.theworldavatar.com:83/access-agent/access"];
    var allowOption;
    var allowsObjects = [];
    if (Object.keys(inputs).includes("allowsProgramme")){
        allowsObjects = Object.keys(inputs["allowsProgramme"]);
        allowOption = "allowsProgramme";
    } else if (Object.keys(inputs).includes("allowsUse")){
        allowsObjects = Object.keys(inputs["allowsUse"]);
        allowOption = "allowsUse";
    }
    var allowOptText = document.createElement("span");
    allowOptText.textContent = allowOption + ": " + allowsObjects;
    infoText.appendChild(allowOptText);

    var objCount = document.createElement("span");
    objCount.textContent = "Valid Plots: " + filteredCount;
    infoText.appendChild(document.createElement("br"));
    infoText.appendChild(objCount);
    return infoText;
}


function pinHighlightObjects(cityObjectsArray){
    //var cityObjectsArray = ["http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_e05bfbcc-561e-4dc3-a355-8106f3e7fcf4/",
    //    "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_654c80ac-b52f-4a24-9569-3ed727ae9d4e/",
    //    "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_32fdd5cf-bf30-40f3-96a6-e84960fae084/",
    //    "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_aca851c3-2b12-489c-9d83-4d7b1c553e50/"];

    var gmlidArray = [];

    //showResultWindow(cityObjectsArray.length);

    for (let i = 0; i < cityObjectsArray.length; i++) {
        var strArray = cityObjectsArray[i].split("/");
        var gmlid = strArray[strArray.length-2];
        gmlidArray.push(gmlid);
    }

    // get geolocation
    var currentLayer = webMap._activeLayer;
    var testcityobjectsJsonData;
    if (Cesium.defined(currentLayer)) {
        testcityobjectsJsonData = currentLayer.cityobjectsJsonData;
    }

    var customDataSource = new Cesium.CustomDataSource("myPoints");

    for (let i = 0; i < gmlidArray.length; i++){
        var obj = testcityobjectsJsonData[gmlidArray[i]];
        var id = gmlidArray[i];
        var lon = (obj.envelope[0] + obj.envelope[2]) / 2.0;
        var lat = (obj.envelope[1] + obj.envelope[3]) / 2.0;
        console.log(gmlidArray[i] + ": " + lon + ", " + lat);

        addPoint(customDataSource, id, lat, lon);
    }

    cesiumViewer.dataSources.add(customDataSource);

    // lat#long
    //var centroidArray = ["1.3171695133731451#104.00344181680501", "1.232218731520345#103.769346005816", "1.3183791030454#103.6429693656075", "1.4531236632754299#103.81758389573551"];
    //var lon = (obj.envelope[0] + obj.envelope[2]) / 2.0;
    //var lat = (obj.envelope[1] + obj.envelope[3]) / 2.0;
    // Draw point
    //for (let i = 0; i < centroidArray.length; i++){
    //    var res = centroidArray[i].split("#");
    //    addPoint(parseFloat(res[0]), parseFloat(res[1]));
    //}

    // set the camera to the view the centroid of sinagpore to view all results
    // set camera view
    //var cameraPostion = {
    //    latitude: 0.023307207117772493,  //
    //    longitude: 1.8121454529642347,
    //    height: 66468.41306483748,
    //    heading: 0.39630595415744363, //13.68838551977463,
    //    pitch: -89.99396415604053,
    //    roll: 0,
    //}
    //flyToCameraPosition(cameraPostion);
    cesiumViewer.camera.flyTo({
        destination: Cesium.Cartesian3.fromRadians(1.8121454529642347, 0.023307207117772493, 66468)
    })
}

function addPoint(customDataSource, pointId, lat, long){

    customDataSource.entities.add({
        position: Cesium.Cartesian3.fromDegrees(long, lat),
        id: pointId,
        point: {
            pixelSize: 10,
            color: Cesium.Color.YELLOW,
        },
    });
}


	//--- End of Extended Web-Map-Client part---//

function listHiddenObjects() {
    var hidddenListElement = document.getElementById("citydb_hiddenlist");

    emptySelectBox(hidddenListElement, function() {
        var hiddenObjects = webMap.getAllHiddenObjects();
        for (var i = 0; i < hiddenObjects.length; i++) {
            var option = document.createElement("option");
            option.text = hiddenObjects[i];
            hidddenListElement.add(option);
            hidddenListElement.selectedIndex = 0;
        }
    });
}

function emptySelectBox(selectElement, callback) {
    for (var i = selectElement.length - 1; i >= 0; i--) {
        selectElement.remove(1);
    }

    callback();
}

function flyToClickedObject(obj) {
    // The web client stores clicked or ctrlclicked entities in a dictionary clickedEntities with {id, entity} as KVP.
    // The function flyTo from Cesium Viewer will be first employed to fly to the selected entity.
    // NOTE: This flyTo function will fail if the target entity has been unloaded (e.g. user has moved camera away).
    // In this case, the function zoomToObjectById shall be used instead.
    // NOTE: This zoomToObjectById function requires a JSON file containing the IDs and coordinates of objects.
    cesiumViewer.flyTo(clickedEntities[obj.value]).then(function (result) {
        if (!result) {
            zoomToObjectById(obj.value);
        }
    }).otherwise(function (error) {
        zoomToObjectById(obj.value);
    });

    obj.selectedIndex = 0;
}

function saveLayerSettings() {
    var activeLayer = webMap.activeLayer;
    applySaving('url', activeLayer);
    applySaving('name', activeLayer);
    applySaving('layerDataType', activeLayer);
    applySaving('layerProxy', activeLayer);
    applySaving('layerClampToGround', activeLayer);
    applySaving('gltfVersion', activeLayer);
    applySaving('thematicDataUrl', activeLayer);
    applySaving('thematicDataSource', activeLayer);
    applySaving('tableType', activeLayer);
    // applySaving('googleSheetsApiKey', activeLayer);
    // applySaving('googleSheetsRanges', activeLayer);
    // applySaving('googleSheetsClientId', activeLayer);
    applySaving('cityobjectsJsonUrl', activeLayer);
    applySaving('minLodPixels', activeLayer);
    applySaving('maxLodPixels', activeLayer);
    applySaving('maxSizeOfCachedTiles', activeLayer);
    applySaving('maxCountOfVisibleTiles', activeLayer);
    console.log(activeLayer);

    // Update Data Source
    thematicDataSourceAndTableTypeDropdownOnchange();

    // update GUI:
    var nodes = document.getElementById('citydb_layerlistpanel').childNodes;
    for (var i = 0; i < nodes.length; i += 3) {
        var layerOption = nodes[i];
        if (layerOption.id == activeLayer.id) {
            layerOption.childNodes[2].innerHTML = activeLayer.name;
        }
    }

    document.getElementById('loadingIndicator').style.display = 'block';
    var promise = activeLayer.reActivate();
    Cesium.when(promise, function (result) {
        document.getElementById('loadingIndicator').style.display = 'none';
    }, function (error) {
        CitydbUtil.showAlertWindow("OK", "Error", error.message);
        document.getElementById('loadingIndicator').style.display = 'none';
    })

    function applySaving(propertyName, activeLayer) {
        var newValue = addLayerViewModel[propertyName];
        if (propertyName === 'maxLodPixels' && newValue == -1) {
            newValue = Number.MAX_VALUE;
        }
        if (Cesium.isArray(newValue)) {
            activeLayer[propertyName] = newValue[0];
        } else {
            activeLayer[propertyName] = newValue;
        }
    }
}

function loadLayerGroup(_layers) {
    if (_layers.length == 0)
        return;

    document.getElementById('loadingIndicator').style.display = 'block';
    _loadLayer(0);

    function _loadLayer(index) {
        var promise = webMap.addLayer(_layers[index]);
        Cesium.when(promise, function (addedLayer) {
            console.log(addedLayer);
            var options = getDataSourceControllerOptions(addedLayer);
            addedLayer.dataSourceController = new DataSourceController(addedLayer.thematicDataSource, signInController, options);
            addEventListeners(addedLayer);
            addLayerToList(addedLayer);
            if (index < (_layers.length - 1)) {
                index++;
                _loadLayer(index);
            } else {
                webMap._activeLayer = _layers[0];
                document.getElementById('loadingIndicator').style.display = 'none';

                // show/hide glTF version based on the value of Layer Data Type
                layerDataTypeDropdownOnchange();

                thematicDataSourceAndTableTypeDropdownOnchange();
            }
        }).otherwise(function (error) {
            CitydbUtil.showAlertWindow("OK", "Error", error.message);
            console.log(error.stack);
            document.getElementById('loadingIndicator').style.display = 'none';
        });
    }
}

function addLayerToList(layer) {
    var radio = document.createElement('input');
    radio.type = "radio";
    radio.name = "dummyradio";
    radio.onchange = function (event) {
        var targetRadio = event.target;
        var layerId = targetRadio.parentNode.id;
        webMap.activeLayer = webMap.getLayerbyId(layerId);
        console.log(webMap.activeLayer);
    };

    var checkbox = document.createElement('input');
    checkbox.type = "checkbox";
    checkbox.id = "id";
    checkbox.checked = layer.active;
    checkbox.onchange = function (event) {
        var checkbox = event.target;
        var layerId = checkbox.parentNode.id;
        var citydbLayer = webMap.getLayerbyId(layerId);
        if (checkbox.checked) {
            console.log("Layer " + citydbLayer.name + " is visible now!");
            citydbLayer.activate(true);
        } else {
            console.log("Layer " + citydbLayer.name + " is not visible now!");
            citydbLayer.activate(false);
        }
    };

    var label = document.createElement('label')
    label.appendChild(document.createTextNode(layer.name));

    var layerOption = document.createElement('div');
    layerOption.id = layer.id;
    layerOption.appendChild(radio);
    layerOption.appendChild(checkbox);
    layerOption.appendChild(label);

    label.ondblclick = function (event) {
        event.preventDefault();
        var layerId = event.target.parentNode.id;
        var citydbLayer = webMap.getLayerbyId(layerId);
        citydbLayer.zoomToStartPosition();
    }

    var layerlistpanel = document.getElementById("citydb_layerlistpanel")
    layerlistpanel.appendChild(layerOption);
}

function addEventListeners(layer) {

    function auxClickEventListener(object) {
        var objectId;
        var targetEntity;
        if (layer instanceof CitydbKmlLayer) {
            targetEntity = object.id;
            objectId = targetEntity.name;
        } else if (layer instanceof Cesium3DTilesDataLayer) {
            console.log(object);
            if (!(object._content instanceof Cesium.Batched3DModel3DTileContent))
                return;

            var featureArray = object._content._features;
            if (!Cesium.defined(featureArray))
                return;
            var objectId = featureArray[object._batchId].getProperty("id");
            if (!Cesium.defined(objectId))
                return;

            targetEntity = new Cesium.Entity({
                id: objectId
            });
            cesiumViewer.selectedEntity = targetEntity;
        }

        // Save this clicked object for later use (such as zooming using ID)
        clickedEntities[objectId] = targetEntity;

        return [objectId ,targetEntity];
    }

    layer.registerEventHandler("CLICK", function (object) {
        var res = auxClickEventListener(object);
        createInfoTable(res, layer);
    });

    layer.registerEventHandler("CTRLCLICK", function (object) {
        auxClickEventListener(object);
    });
}

function zoomToDefaultCameraPosition() {
    var deferred = Cesium.when.defer();
    var latitudeStr = urlController.getUrlParaValue('latitude', window.location.href, CitydbUtil);
    var longitudeStr = urlController.getUrlParaValue('longitude', window.location.href, CitydbUtil);
    var heightStr = urlController.getUrlParaValue('height', window.location.href, CitydbUtil);
    var headingStr = urlController.getUrlParaValue('heading', window.location.href, CitydbUtil);
    var pitchStr = urlController.getUrlParaValue('pitch', window.location.href, CitydbUtil);
    var rollStr = urlController.getUrlParaValue('roll', window.location.href, CitydbUtil);

    if (latitudeStr && longitudeStr && heightStr && headingStr && pitchStr && rollStr) {
        var cameraPostion = {
            latitude: parseFloat(latitudeStr),
            longitude: parseFloat(longitudeStr),
            height: parseFloat(heightStr),
            heading: parseFloat(headingStr),
            pitch: parseFloat(pitchStr),
            roll: parseFloat(rollStr)
        }
        return flyToCameraPosition(cameraPostion);
    } else {
        return zoomToDefaultCameraPosition_expired();
    }

    return deferred;
}

function zoomToDefaultCameraPosition_expired() {
    var deferred = Cesium.when.defer();
    var cesiumCamera = cesiumViewer.scene.camera;
    var latstr = urlController.getUrlParaValue('lat', window.location.href, CitydbUtil);
    var lonstr = urlController.getUrlParaValue('lon', window.location.href, CitydbUtil);

    if (latstr && lonstr) {
        var lat = parseFloat(latstr);
        var lon = parseFloat(lonstr);
        var range = 800;
        var heading = 6;
        var tilt = 49;
        var altitude = 40;

        var rangestr = urlController.getUrlParaValue('range', window.location.href, CitydbUtil);
        if (rangestr)
            range = parseFloat(rangestr);

        var headingstr = urlController.getUrlParaValue('heading', window.location.href, CitydbUtil);
        if (headingstr)
            heading = parseFloat(headingstr);

        var tiltstr = urlController.getUrlParaValue('tilt', window.location.href, CitydbUtil);
        if (tiltstr)
            tilt = parseFloat(tiltstr);

        var altitudestr = urlController.getUrlParaValue('altitude', window.location.href, CitydbUtil);
        if (altitudestr)
            altitude = parseFloat(altitudestr);

        var _center = Cesium.Cartesian3.fromDegrees(lon, lat);
        var _heading = Cesium.Math.toRadians(heading);
        var _pitch = Cesium.Math.toRadians(tilt - 90);
        var _range = range;
        cesiumCamera.flyTo({
            destination: Cesium.Cartesian3.fromDegrees(lon, lat, _range),
            orientation: {
                heading: _heading,
                pitch: _pitch,
                roll: 0
            },
            complete: function () {
                deferred.resolve("fly to the desired camera position");
            }
        });
    } else {
        // default camera postion
        deferred.resolve("fly to the default camera position");
    }
    return deferred;
}

function flyToCameraPosition(cameraPosition) {
    var deferred = Cesium.when.defer();
    var cesiumCamera = cesiumViewer.scene.camera;
    var longitude = cameraPosition.longitude;
    var latitude = cameraPosition.latitude;
    var height = cameraPosition.height;
    cesiumCamera.flyTo({
        destination: Cesium.Cartesian3.fromDegrees(longitude, latitude, height),
        orientation: {
            heading: Cesium.Math.toRadians(cameraPosition.heading),
            pitch: Cesium.Math.toRadians(cameraPosition.pitch),
            roll: Cesium.Math.toRadians(cameraPosition.roll)
        },
        complete: function () {
            deferred.resolve("fly to the desired camera position");
        }
    });
    return deferred;
}

// Creation of a scene link for sharing with other people..
function showSceneLink() {
    var tokens = {
        ionToken: ionToken,
        bingToken: bingToken
    }
    var sceneLink = urlController.generateLink(
        webMap,
        addWmsViewModel,
        addTerrainViewModel,
        addSplashWindowModel,
        tokens,
        signInController,
        googleClientId,
        splashController,
        cesiumViewer,
        Cesium
    );
    CitydbUtil.showAlertWindow("OK", "Scene Link", '<a href="' + sceneLink + '" style="color:#c0c0c0" target="_blank">' + sceneLink + '</a>');
}

// Clear Highlighting effect of all highlighted objects
function clearhighlight() {
    var layers = webMap._layers;
    for (var i = 0; i < layers.length; i++) {
        if (layers[i].active) {
            layers[i].unHighlightAllObjects();
        }
    }
    cesiumViewer.selectedEntity = undefined;
}

// hide the selected objects
function hideSelectedObjects() {
    var layers = webMap._layers;
    var objectIds;
    for (var i = 0; i < layers.length; i++) {
        if (layers[i].active) {
            objectIds = Object.keys(layers[i].highlightedObjects);
            layers[i].hideObjects(objectIds);
        }
    }
}

// show the hidden objects
function showHiddenObjects() {
    var layers = webMap._layers;
    for (var i = 0; i < layers.length; i++) {
        if (layers[i].active) {
            layers[i].showAllObjects();
        }
    }
}

function zoomToObjectById(gmlId, callBackFunc, errorCallbackFunc) {
    gmlId = gmlId.trim();
    var activeLayer = webMap._activeLayer;
    if (Cesium.defined(activeLayer)) {
        var cityobjectsJsonData = activeLayer.cityobjectsJsonData;
        if (!cityobjectsJsonData) {
            if (Cesium.defined(errorCallbackFunc)) {
                errorCallbackFunc.call(this);
            }
        } else {
            var obj = cityobjectsJsonData[gmlId];
        }
        if (obj) {
            var lon = (obj.envelope[0] + obj.envelope[2]) / 2.0;
            var lat = (obj.envelope[1] + obj.envelope[3]) / 2.0;
            flyToMapLocation(lat, lon, callBackFunc);
        } else {
            // TODO
            var thematicDataUrl = webMap.activeLayer.thematicDataUrl;
            webMap._activeLayer.dataSourceController.fetchData(gmlId, function (result) {
                if (!result) {
                    if (Cesium.defined(errorCallbackFunc)) {
                        errorCallbackFunc.call(this);
                    }
                } else {
                    var centroid = result["CENTROID"];
                    if (centroid) {
                        var res = centroid.match(/\(([^)]+)\)/)[1].split(",");
                        var lon = parseFloat(res[0]);
                        var lat = parseFloat(res[1]);
                        flyToMapLocation(lat, lon, callBackFunc);
                    } else {
                        if (Cesium.defined(errorCallbackFunc)) {
                            errorCallbackFunc.call(this);
                        }
                    }
                }
            }, 1000);

            // var promise = fetchDataFromGoogleFusionTable(gmlId, thematicDataUrl);
            // Cesium.when(promise, function (result) {
            //     var centroid = result["CENTROID"];
            //     if (centroid) {
            //         var res = centroid.match(/\(([^)]+)\)/)[1].split(",");
            //         var lon = parseFloat(res[0]);
            //         var lat = parseFloat(res[1]);
            //         flyToMapLocation(lat, lon, callBackFunc);
            //     } else {
            //         if (Cesium.defined(errorCallbackFunc)) {
            //             errorCallbackFunc.call(this);
            //         }
            //     }
            // }, function () {
            //     if (Cesium.defined(errorCallbackFunc)) {
            //         errorCallbackFunc.call(this);
            //     }
            // });
        }
    } else {
        if (Cesium.defined(errorCallbackFunc)) {
            errorCallbackFunc.call(this);
        }
    }
}

function flyToMapLocation(lat, lon, callBackFunc) {
    var cesiumWidget = webMap._cesiumViewerInstance.cesiumWidget;
    var scene = cesiumWidget.scene;
    var camera = scene.camera;
    var canvas = scene.canvas;
    var globe = scene.globe;
    var clientWidth = canvas.clientWidth;
    var clientHeight = canvas.clientHeight;
    camera.flyTo({
        destination: Cesium.Cartesian3.fromDegrees(lon, lat, 2000),
        complete: function () {
            var intersectedPoint = globe.pick(camera.getPickRay(new Cesium.Cartesian2(clientWidth / 2, clientHeight / 2)), scene);
            var terrainHeight = Cesium.Ellipsoid.WGS84.cartesianToCartographic(intersectedPoint).height;
            var center = Cesium.Cartesian3.fromDegrees(lon, lat, terrainHeight);
            var heading = Cesium.Math.toRadians(0);
            var pitch = Cesium.Math.toRadians(-50);
            var range = 100;
            camera.lookAt(center, new Cesium.HeadingPitchRange(heading, pitch, range));
            camera.lookAtTransform(Cesium.Matrix4.IDENTITY);
            if (Cesium.defined(callBackFunc)) {
                callBackFunc.call(this);
            }
        }
    })
}

function addNewLayer() {
    var _layers = new Array();
    var options = {
        url: addLayerViewModel.url.trim(),
        name: addLayerViewModel.name.trim(),
        layerDataType: addLayerViewModel.layerDataType.trim(),
        layerProxy: (addLayerViewModel.layerProxy === true),
        layerClampToGround: (addLayerViewModel.layerClampToGround === true),
        gltfVersion: addLayerViewModel.gltfVersion.trim(),
        thematicDataUrl: addLayerViewModel.thematicDataUrl.trim(),
        thematicDataSource: addLayerViewModel.thematicDataSource.trim(),
        tableType: addLayerViewModel.tableType.trim(),
        // googleSheetsApiKey: addLayerViewModel.googleSheetsApiKey.trim(),
        // googleSheetsRanges: addLayerViewModel.googleSheetsRanges.trim(),
        // googleSheetsClientId: addLayerViewModel.googleSheetsClientId.trim(),
        cityobjectsJsonUrl: addLayerViewModel.cityobjectsJsonUrl.trim(),
        minLodPixels: addLayerViewModel.minLodPixels,
        maxLodPixels: addLayerViewModel.maxLodPixels == -1 ? Number.MAX_VALUE : addLayerViewModel.maxLodPixels,
        maxSizeOfCachedTiles: addLayerViewModel.maxSizeOfCachedTiles,
        maxCountOfVisibleTiles: addLayerViewModel.maxCountOfVisibleTiles
    }

    // since Cesium 3D Tiles also require name.json in the URL, it must be checked first
    var layerDataTypeDropdown = document.getElementById("layerDataTypeDropdown");
    if (layerDataTypeDropdown.options[layerDataTypeDropdown.selectedIndex].value === 'Cesium 3D Tiles') {
        _layers.push(new Cesium3DTilesDataLayer(options));
    } else if (['kml', 'kmz', 'json', 'czml'].indexOf(CitydbUtil.get_suffix_from_filename(options.url)) > -1) {
        _layers.push(new CitydbKmlLayer(options));
    }

    loadLayerGroup(_layers);
}

function removeSelectedLayer() {
    var layer = webMap.activeLayer;
    if (Cesium.defined(layer)) {
        var layerId = layer.id;
        document.getElementById(layerId).remove();
        webMap.removeLayer(layerId);
        // update active layer of the globe webMap
        var webMapLayers = webMap._layers;
        if (webMapLayers.length > 0) {
            webMap.activeLayer = webMapLayers[0];
        } else {
            webMap.activeLayer = undefined;
        }
    }
}

function addWebMapServiceProvider() {
    var baseLayerPickerViewModel = cesiumViewer.baseLayerPicker.viewModel;
    var wmsProviderViewModel = new Cesium.ProviderViewModel({
        name: addWmsViewModel.name.trim(),
        iconUrl: addWmsViewModel.iconUrl.trim(),
        tooltip: addWmsViewModel.tooltip.trim(),
        creationFunction: function () {
            return new Cesium.WebMapServiceImageryProvider({
                url: new Cesium.Resource({url: addWmsViewModel.url.trim(), proxy: addWmsViewModel.proxyUrl.trim().length == 0 ? null : new Cesium.DefaultProxy(addWmsViewModel.proxyUrl.trim())}),
                layers: addWmsViewModel.layers.trim(),
                parameters: Cesium.queryToObject(addWmsViewModel.additionalParameters.trim())
            });
        }
    });
    baseLayerPickerViewModel.imageryProviderViewModels.push(wmsProviderViewModel);
    baseLayerPickerViewModel.selectedImagery = wmsProviderViewModel;
}

function removeImageryProvider() {
    var baseLayerPickerViewModel = cesiumViewer.baseLayerPicker.viewModel;
    var selectedImagery = baseLayerPickerViewModel.selectedImagery;
    baseLayerPickerViewModel.imageryProviderViewModels.remove(selectedImagery);
    baseLayerPickerViewModel.selectedImagery = baseLayerPickerViewModel.imageryProviderViewModels[0];
}

function addTerrainProvider() {
    var baseLayerPickerViewModel = cesiumViewer.baseLayerPicker.viewModel;
    var demProviderViewModel = new Cesium.ProviderViewModel({
        name: addTerrainViewModel.name.trim(),
        iconUrl: addTerrainViewModel.iconUrl.trim(),
        tooltip: addTerrainViewModel.tooltip.trim(),
        creationFunction: function () {
            return new Cesium.CesiumTerrainProvider({
                url: addTerrainViewModel.url.trim()
            });
        }
    })
    baseLayerPickerViewModel.terrainProviderViewModels.push(demProviderViewModel);
    baseLayerPickerViewModel.selectedTerrain = demProviderViewModel;
}

function removeTerrainProvider() {
    var baseLayerPickerViewModel = cesiumViewer.baseLayerPicker.viewModel;
    var selectedTerrain = baseLayerPickerViewModel.selectedTerrain;
    baseLayerPickerViewModel.terrainProviderViewModels.remove(selectedTerrain);
    baseLayerPickerViewModel.selectedTerrain = baseLayerPickerViewModel.terrainProviderViewModels[0];
}

function createScreenshot() {
    cesiumViewer.render();
    var imageUri = cesiumViewer.canvas.toDataURL();
    var imageWin = window.open("");
    imageWin.document.write("<html><head>" +
        "<title>" + imageUri + "</title></head><body>" +
        '<img src="' + imageUri + '"width="100%">' +
        "</body></html>");
    return imageWin;
}

function printCurrentview() {
    var imageWin = createScreenshot();
    imageWin.document.close();
    imageWin.focus();
    imageWin.print();
    imageWin.close();
}

function toggleShadows() {
    cesiumViewer.shadows = !cesiumViewer.shadows;
    if (!cesiumViewer.shadows) {
        cesiumViewer.terrainShadows = Cesium.ShadowMode.DISABLED;
    }
}

function toggleTerrainShadows() {
    if (cesiumViewer.terrainShadows == Cesium.ShadowMode.ENABLED) {
        cesiumViewer.terrainShadows = Cesium.ShadowMode.DISABLED;
    } else {
        cesiumViewer.terrainShadows = Cesium.ShadowMode.ENABLED;
        if (!cesiumViewer.shadows) {
            CitydbUtil.showAlertWindow("OK", "Switching on terrain shadows now", 'Please note that shadows for 3D models will also be switched on.',
                function () {
                    toggleShadows();
                });
        }
    }
}

// source https://www.w3resource.com/javascript-exercises/javascript-regexp-exercise-9.php
function isValidUrl(str) {
    regexp =  /^(?:(?:https?|ftp):\/\/)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:\/\S*)?$/;
    return regexp.test(str);
}

function createInfoTable(res, citydbLayer) {
    var thematicDataSourceDropdown = document.getElementById("thematicDataSourceDropdown");
    var selectedThematicDataSource = thematicDataSourceDropdown.options[thematicDataSourceDropdown.selectedIndex].value;
    var gmlid = selectedThematicDataSource === "KML" ? res[1]._id : res[0];
    var cesiumEntity = res[1];

    var thematicDataUrl = citydbLayer.thematicDataUrl;
    cesiumEntity.description = "Loading feature information...";
    cesiumEntity._cia_context = this.cia_context;

    citydbLayer.dataSourceController.fetchData(gmlid, function (kvp) {
        if (!kvp) {
            cesiumEntity.description = 'No feature information found';
        } else {
            console.log(kvp);
            var html = '<table class="cesium-infoBox-defaultTable" style="font-size:10.5pt"><tbody>';
            for (var key in kvp) {
                var iValue = kvp[key];
                // check if this value is a valid URL
                if (isValidUrl(iValue)) {
                    iValue = '<a href="' + iValue + '" target="_blank">' + iValue + '</a>';
                }
                html += '<tr><td>' + key + '</td><td>' + iValue + '</td></tr>';
            }
            html += '</tbody></table>';

            cesiumEntity.description = html;
        }
    }, 1000, cesiumEntity);

    // fetchDataFromGoogleFusionTable(gmlid, thematicDataUrl).then(function (kvp) {
    //     console.log(kvp);
    //     var html = '<table class="cesium-infoBox-defaultTable" style="font-size:10.5pt"><tbody>';
    //     for (var key in kvp) {
    //         html += '<tr><td>' + key + '</td><td>' + kvp[key] + '</td></tr>';
    //     }
    //     html += '</tbody></table>';
    //
    //     cesiumEntity.description = html;
    // }).otherwise(function (error) {
    //     cesiumEntity.description = 'No feature information found';
    // });
}

function fetchDataFromGoogleSpreadsheet(gmlid, thematicDataUrl) {
    var kvp = {};
    var deferred = Cesium.when.defer();

    var spreadsheetKey = thematicDataUrl.split("/")[5];
    var metaLink = 'https://spreadsheets.google.com/feeds/worksheets/' + spreadsheetKey + '/public/full?alt=json-in-script';

    Cesium.jsonp(metaLink).then(function (meta) {
        console.log(meta);
        var feedCellUrl = meta.feed.entry[0].link[1].href;
        feedCellUrl += '?alt=json-in-script&min-row=1&max-row=1';
        Cesium.jsonp(feedCellUrl).then(function (cellData) {
            var feedListUrl = meta.feed.entry[0].link[0].href;
            feedListUrl += '?alt=json-in-script&sq=gmlid%3D';
            feedListUrl += gmlid;
            Cesium.jsonp(feedListUrl).then(function (listData) {
                for (var i = 1; i < cellData.feed.entry.length; i++) {
                    var key = cellData.feed.entry[i].content.$t;
                    var value = listData.feed.entry[0]['gsx$' + key.toLowerCase().replace(/_/g, '')].$t;
                    kvp[key] = value;
                }
                deferred.resolve(kvp);
            }).otherwise(function (error) {
                deferred.reject(error);
            });
        }).otherwise(function (error) {
            deferred.reject(error);
        });
    }).otherwise(function (error) {
        deferred.reject(error);
    });

    return deferred.promise;
}

function fetchDataFromGoogleFusionTable(gmlid, thematicDataUrl) {
    var kvp = {};
    var deferred = Cesium.when.defer();

    var tableID = urlController.getUrlParaValue('docid', thematicDataUrl, CitydbUtil);
    var sql = "SELECT * FROM " + tableID + " WHERE GMLID = '" + gmlid + "'";
    var apiKey = "AIzaSyAm9yWCV7JPCTHCJut8whOjARd7pwROFDQ";
    var queryLink = "https://www.googleapis.com/fusiontables/v2/query";
    new Cesium.Resource({url: queryLink, queryParameters: {sql: sql, key: apiKey}}).fetch({responseType: 'json'}).then(function (data) {
        console.log(data);
        var columns = data.columns;
        var rows = data.rows;
        for (var i = 0; i < columns.length; i++) {
            var key = columns[i];
            var value = rows[0][i];
            kvp[key] = value;
        }
        console.log(kvp);
        deferred.resolve(kvp);
    }).otherwise(function (error) {
        deferred.reject(error);
    });
    return deferred.promise;
}

function showInExternalMaps() {
    var mapOptionList = document.getElementById('citydb_showinexternalmaps');
    var selectedIndex = mapOptionList.selectedIndex;
    mapOptionList.selectedIndex = 0;

    var selectedEntity = cesiumViewer.selectedEntity;
    if (!Cesium.defined(selectedEntity))
        return;

    var selectedEntityPosition = selectedEntity.position;
    var wgs84OCoordinate;

    if (!Cesium.defined(selectedEntityPosition)) {
        var boundingSphereScratch = new Cesium.BoundingSphere();
        cesiumViewer._dataSourceDisplay.getBoundingSphere(selectedEntity, false, boundingSphereScratch);
        wgs84OCoordinate = Cesium.Ellipsoid.WGS84.cartesianToCartographic(boundingSphereScratch.center);
    } else {
        wgs84OCoordinate = Cesium.Ellipsoid.WGS84.cartesianToCartographic(selectedEntityPosition._value);

    }
    var lat = Cesium.Math.toDegrees(wgs84OCoordinate.latitude);
    var lon = Cesium.Math.toDegrees(wgs84OCoordinate.longitude);
    var mapLink = "";

    switch (selectedIndex) {
        case 1:
            //mapLink = 'https://www.mapchannels.com/dualmaps7/map.htm?lat=' + lat + '&lng=' + lon + '&z=18&slat=' + lat + '&slng=' + lon + '&sh=-150.75&sp=-0.897&sz=1&gm=0&bm=2&panel=s&mi=1&md=0';
            //mapLink = 'https://www.google.com/maps/embed/v1/streetview?location=' + lat + ',' + lon + '&key=' + 'AIzaSyBRXHXasDb8PGOXCfQP7r7xQiAQXo3eIQs';
            //mapLink = 'https://maps.googleapis.com/maps/api/streetview?size=400x400&location=' + lat + ',' + lon + '&fov=90&heading=235&pitch=10' + '&key=AIzaSyBRXHXasDb8PGOXCfQP7r7xQiAQXo3eIQs';
            mapLink = 'https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=' + lat + ',' + lon;
            break;
        case 2:
            mapLink = 'https://www.openstreetmap.org/index.html?lat=' + lat + '&lon=' + lon + '&zoom=20';
            break;
        case 3:
            mapLink = 'https://www.bing.com/maps/default.aspx?v=2&cp=' + lat + '~' + lon + '&lvl=19&style=o';
            break;
        case 4:
            mapLink = 'https://www.mapchannels.com/dualmaps7/map.htm?x=' + lon + '&y=' + lat + '&z=16&gm=0&ve=4&gc=0&bz=0&bd=0&mw=1&sv=1&sva=1&svb=0&svp=0&svz=0&svm=2&svf=0&sve=1';
            break;
        default:
        //	do nothing...
    }

    window.open(mapLink);
}

function layerDataTypeDropdownOnchange() {
    var layerDataTypeDropdown = document.getElementById("layerDataTypeDropdown");
    if (layerDataTypeDropdown.options[layerDataTypeDropdown.selectedIndex].value !== "COLLADA/KML/glTF") {
        document.getElementById("gltfVersionDropdownRow").style.display = "none";
        document.getElementById("layerProxyAndClampToGround").style.display = "none";
    } else {
        document.getElementById("gltfVersionDropdownRow").style.display = "";
        document.getElementById("layerProxyAndClampToGround").style.display = "";
    }
    addLayerViewModel["layerDataType"] = layerDataTypeDropdown.options[layerDataTypeDropdown.selectedIndex].value;
}

function thematicDataSourceAndTableTypeDropdownOnchange() {
    if (webMap && webMap._activeLayer) {
        var thematicDataSourceDropdown = document.getElementById("thematicDataSourceDropdown");
        var selectedThematicDataSource = thematicDataSourceDropdown.options[thematicDataSourceDropdown.selectedIndex].value;

        var tableTypeDropdown = document.getElementById("tableTypeDropdown");
        var selectedTableType = tableTypeDropdown.options[tableTypeDropdown.selectedIndex].value;

        addLayerViewModel["thematicDataSource"] = selectedThematicDataSource;
        addLayerViewModel["tableType"] = selectedTableType;

        // if (selectedThematicDataSource == "GoogleSheets") {
        //     document.getElementById("rowGoogleSheetsApiKey").style.display = "table-row";
        //     document.getElementById("rowGoogleSheetsRanges").style.display = "table-row";
        //     document.getElementById("rowGoogleSheetsClientId").style.display = "table-row";
        // } else {
        //     document.getElementById("rowGoogleSheetsApiKey").style.display = "none";
        //     document.getElementById("rowGoogleSheetsRanges").style.display = "none";
        //     document.getElementById("rowGoogleSheetsClientId").style.display = "none";
        // }

        var options = getDataSourceControllerOptions(webMap._activeLayer);
        // Mashup Data Source Service
        webMap._activeLayer.dataSourceController = new DataSourceController(selectedThematicDataSource, signInController, options);
    }
}

function getDataSourceControllerOptions(layer) {
    var dataSourceUri = layer.thematicDataUrl === "" ? layer.url : layer.thematicDataUrl;
    var options = {
        // name: "",
        // type: "",
        // provider: "",
        uri: dataSourceUri,
        tableType: layer.tableType,
        thirdPartyHandler: {
            type: "Cesium",
            handler: layer ? layer._citydbKmlDataSource : undefined
        },
        // ranges: addLayerViewModel.googleSheetsRanges,
        // apiKey: addLayerViewModel.googleSheetsApiKey,
        // clientId: addLayerViewModel.googleSheetsClientId
        clientId: googleClientId ? googleClientId : "",
        proxyPrefix: layer.layerProxy ? CitydbUtil.getProxyPrefix(dataSourceUri) : ""
    };
    return options;
}

// Sign in utilities
var googleClientId = urlController.getUrlParaValue('googleClientId', window.location.href, CitydbUtil);
if (googleClientId) {
    var signInController = new SigninController(googleClientId);
}

// Mobile layouts and functionalities
var mobileController = new MobileController();
