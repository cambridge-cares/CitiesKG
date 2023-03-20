/*********************** Extension of script.js *******************************/


/************************ Process Query Results ******************************/
//Shiying: highlight multiple cityobjects, create customDatasource, pinCityobjects
function processFilteredObjects(cityObjectsArray, colorStr){  // citydbKmlLayer object, list of files in the folder--> get the summaryfile
    //var cityObjectsArray = ["UUID_fddf5c91-cdd6-436a-95e6-aa1fa199b75d", "UUID_e5779fd5-ea90-4d2c-9a0a-cf7f46e5aad3"];
    //var cityObjectsArray = ["http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_fddf5c91-cdd6-436a-95e6-aa1fa199b75d/", "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_e5779fd5-ea90-4d2c-9a0a-cf7f46e5aad3/", "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_b6f4d0de-cf5c-4917-aba0-c1a91fa4960b/"];

    // UUID_fddf5c91-cdd6-436a-95e6-aa1fa199b75d - inside
    // UUID_b6f4d0de-cf5c-4917-aba0-c1a91fa4960b - outside of the scene

    var currentLayer = webMap.activeLayer;
    var filteredResult= {};
    var highlightColor = currentLayer._highlightColor; // new Cesium.Color(16/255, 77/255, 151/255, 1.0);

    for (let i = 0; i < cityObjectsArray.length; i++) {
        var strArray = cityObjectsArray[i].split("/");
        var gmlid = strArray[strArray.length-2];
        filteredResult[gmlid] = highlightColor; // new Cesium.Color(65/255, 168/255, 255/255, 0.8);
    }

    filteredObjects = filteredResult;
    pinHighlightObjects(cityObjectsArray, colorStr);
    //highlightFilteredObj(filteredObjects);
    return filteredObjects;
}

function highlightFilteredObj(filteredObjects){
    var currentLayer = webMap.activeLayer;
    currentLayer.unHighlightAllObjects();
    if (filteredObjects != undefined){
        currentLayer.highlight(filteredObjects);
    }
}

function pinHighlightObjects(cityObjectsArray, hexColorString){
    var gmlidArray = [];

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

    var dataUnifier = dataSourcePrefix + _customDataSourceCounter;
    var customDataSource = new Cesium.CustomDataSource(dataUnifier);
    var currentLayer = webMap.activeLayer;

    for (let i = 0; i < gmlidArray.length; i++){
        var obj = testcityobjectsJsonData[gmlidArray[i]];
        var id = gmlidArray[i];
        var lon = (obj.envelope[0] + obj.envelope[2]) / 2.0;
        var lat = (obj.envelope[1] + obj.envelope[3]) / 2.0;
        //console.log(gmlidArray[i] + ": " + lon + ", " + lat);

        addPoint(customDataSource, id, lat, lon, currentLayer ,hexColorString);
    }
    //addEventListeners(customDataSource);
    customDataSourceMap.set(dataUnifier, customDataSource);
    cesiumViewer.dataSources.add(customDataSource);

    //var testdataSource = customDataSource;
    //var testentity = testdataSource.getEntitiesById('UUID_3422c065-c104-4a48-9536-0f30ff96c670');
    //console.log(testentity);

    /**
     // In order to only show the current dataSource, we need to remove the previous first.
     for (let [key, value] of customDataSourceMap){
        console.log(key + " : " + cesiumViewer.dataSources.remove(value));
    }**/
}

function addPoint(customDataSource, pointId, lat, long, parentLayer ,hexColorString){

    customDataSource.entities.add({
        position: Cesium.Cartesian3.fromDegrees(long, lat, 5),
        id: pointId,
        point: {
            pixelSize: 10,
            color: Cesium.Color.fromCssColorString(hexColorString),
            outlineColor: Cesium.Color.BLACK,
            outlineWidth:1,
        },
        layerId: parentLayer.id,
        name: pointId,
        iriPrefix: parentLayer._citydbKmlDataSource._iriPrefix,
    });
}


/************************ Query Result Windows ******************************/
// create the summary text for PPF result box
// resultjson is json object

function processInfoContext(resultjson){
    let ifGFA = false;
    var infoText = document.createElement("div");
    var title = document.createElement("span");

    var developmentType = document.getElementById("DevelopmentType");
    title.innerHTML = "Search " + developmentType.options[developmentType.selectedIndex].text;
    if (title.innerHTML === "Search Search plots...") {
        title.innerHTML = "Search example query"
    }

    title.style.fontWeight = "bold";
    title.style.fontSize = "12px";
    infoText.appendChild(title);
    infoText.appendChild(document.createElement("br"));

    //{"allowsProgramme":{"Cafe":"","StudentRunBusiness":""},"TotalGFA":""}
    var filteredCount = resultjson["http://www.theworldavatar.com:83/access-agent/access"]["filteredCounts"];
    var inputs = resultjson["context"]["http://www.theworldavatar.com:83/access-agent/access"];
    var allowOption;
    var allowsObjects = [];
    var allowsGFA = [];
    if (Object.keys(inputs).includes("allowsProgramme")){
        allowsObjects = Object.keys(inputs["allowsProgramme"]);
        allowsGFA = Object.values(inputs["allowsProgramme"]);
        allowOption = "allowsProgramme:";
    } else if (Object.keys(inputs).includes("allowsUse")){
        allowsObjects = Object.keys(inputs["allowsUse"]);
        allowsGFA = Object.values(inputs["allowsUse"]);
        allowOption = "allowsUse:";
    }
    var allowOpts = document.createElement("span");
    allowOpts.textContent = allowOption;
    infoText.appendChild(allowOpts);
    infoText.appendChild(document.createElement("br"));

    var allowOptText = document.createElement("span");
    var allowsContent = "";
    for (i = 0; i < allowsObjects.length; ++i){
        if (allowsGFA[i] != ""){
            ifGFA = true;
            allowsContent += allowsObjects[i] + ": " + allowsGFA[i] + " sqm" + "<br />";
        }else{
            allowsContent += allowsObjects[i] + "<br/>";
        }

    }
    allowOptText.innerHTML = allowsContent;
    infoText.appendChild(allowOptText);
    //infoText.appendChild(document.createElement("br"));

    if (Object.keys(inputs).includes("TotalGFA") && inputs["TotalGFA"] > 0){
        ifGFA = true;
        var totalGFA = document.createElement("span");
        var totalGFAValue = inputs["TotalGFA"];
        totalGFA.textContent = "Total GFA: " + totalGFAValue + " sqm";
        infoText.appendChild(totalGFA);
        infoText.appendChild(document.createElement("br"));
    }

    var objCount = document.createElement("span");
    objCount.textContent = "# search results: " + filteredCount + " plots";
    infoText.appendChild(document.createElement("br"));
    infoText.appendChild(objCount);
    infoText.appendChild(document.createElement("br")); // Ayda: swapped white line positions according Pieter's instructions.

    // static part: measure the impact of the demo
    var docCount = document.createElement("span");
    if (ifGFA){
        docCount.innerHTML = "The search replaces manually checking <br /> 38 regulatory documents";
    }else {
        docCount.innerHTML = "The search replaces manually checking <br /> 17 regulatory documents ";
    }
    infoText.appendChild(document.createElement("br"));
    infoText.appendChild(docCount);
    var testURL = document.createElement("a");
    testURL.href = "http://www.theworldavatar.com:83/citieskg/#explore";
    testURL.target = "_blank";
    testURL.text = "http://www.theworldavatar.com:83/citieskg/#explore";
    testURL.onclick
    infoText.appendChild(testURL);
    return infoText;
}






/************************ Process CIA Agent Results **************************************/

function showResultWindow(resultJson, colorStr){

    var resultBoxTitle = document.getElementById("resultBox-title");
    resultBoxTitle.style.visibility = "visible";
    var resultBox = document.getElementById("resultBox-iframe");
    resultBox.style.visibility = "visible";
    var resultBoxContent = document.createElement("div");
    resultBoxContent.style.display = "block";
    var listItem = document.createElement("li");
    listItem.id = dataSourcePrefix + _customDataSourceCounter;
    //listItem.title = colorStr;

    listItem.appendChild(processInfoContext(resultJson));
    // <input type='checkbox' onchange='updateGfaRows()'>

    var checkbox = document.createElement("input");
    checkbox.type = 'checkbox';
    checkbox.className = 'ciaResults';
    checkbox.checked = true;
    checkbox.onchange = function (){
        updateSelectedDataSources();
    };
    listItem.appendChild(checkbox);

    var colorbox = document.createElement('div');
    colorbox.className = "colorbox";
    colorbox.style = "width: 10px; height: 10px; display: inline-block;";
    colorbox.style.backgroundColor = colorStr;
    colorbox.title = colorStr;
    listItem.appendChild(colorbox);
    var closeButton = document.createElement("span");
    closeButton.className = "close";
    closeButton.textContent = "x";
    closeButton.onclick = function (event){
        var targetId = event.currentTarget.parentNode.id;
        //var targetUnusedColor = event.currentTarget.parentNode.title;
        var targetUnusedColor = event.currentTarget.parentNode.getElementsByClassName("colorbox")[0].title;  // can store the color in the title of li item. which is the description of the corresponding color
        removeDataSourceById(targetId, targetUnusedColor);
    }
    listItem.appendChild(closeButton);
    resultBoxContent.appendChild(listItem);
    resultBox.appendChild(resultBoxContent);

    var closebtns = document.getElementsByClassName("close");
    var i;

    for (i = 0; i < closebtns.length; i++) {
        closebtns[i].addEventListener("click", function() {
            console.log("this has been removed: " + this.parentNode.id);
            this.parentNode.remove();
        });
    }
}

function updateSelectedDataSources(){
    // remove all DataSources
    for (let [key, value] of customDataSourceMap){
        cesiumViewer.dataSources.remove(value);
    }
    var ciaResultsArray = document.getElementsByClassName('ciaResults');
    for (let i = 0; i < ciaResultsArray.length; ++i){
        if (ciaResultsArray[i].checked){
            cesiumViewer.dataSources.add(customDataSourceMap.get(ciaResultsArray[i].parentNode.id));
        }
    }
}

function removeDataSourceById(datasourceId, unusedColor){
    var targetDataSource = customDataSourceMap.get(datasourceId);
    console.log("The unused color is ", unusedColor);
    insertUnusedColor(unusedColor);
    customDataSourceMap.delete(datasourceId);
    console.log("dataSource " + datasourceId + " has been removed: " + cesiumViewer.dataSources.remove(targetDataSource));
}

function processCIAResult(CIAdata){
    var hexColorString = pickColorFromArray(); // generateLightColorHex(); generateColorHex();
    showResultWindow(CIAdata, hexColorString);
    processFilteredObjects(CIAdata["http://www.theworldavatar.com:83/access-agent/access"]["filtered"], hexColorString);
    _customDataSourceCounter++;
}

/****** Part of the Result Window: random color generation **/


/** generate color for the pin point **/
let initColorsArr = ["#0096FF", "#FFA500", "#00FFC8", "#FF1694", "#E5DE00", "#009F30", "#964B00"];
let colorsArr = initColorsArr;
function pickColorFromArray(){
    var pickedColor;
    if(colorsArr.length > 0){
        var j = Math.floor(Math.random() * (colorsArr.length));
        pickedColor = colorsArr[j];
        colorsArr.splice(j, 1);
    }
    return pickedColor;
}

function insertUnusedColor(hexColorStr){
    colorsArr.splice(colorsArr.length-1, 0, hexColorStr);
}

function generateColorHex(){
    let color = "#";
    color += Math.floor(Math.random()*16777215).toString(16);
    return color;
}

function generateLightColorHex() {
    let color = "#";
    for (let i = 0; i < 3; i++)
        color += ("0" + Math.floor(((1 + Math.random()) * Math.pow(16, 2)) / 2).toString(16)).slice(-2);
    return color;
}


/************************ Distance Agent **************************************/

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

