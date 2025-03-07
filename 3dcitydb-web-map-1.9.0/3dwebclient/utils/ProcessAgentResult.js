/*
 * The implementation is part of the development of
 * Cities Knowledge Graph
 * https://fcl.ethz.ch/research/research-projects/cities-knowledge-graph.html
 * of Singapore ETH center
 *
 * Credit to 3DCityDB-Web-Map: http://www.3dcitydb.org/
 *
 * Copyright 2020 - 2023
 * Singapore ETH Center
 * and
 * Cambridge centre for Advanced Research and Education in Singapore LTD.
 *
 * This work is jointly developed by the following research partners:
 * Singapore ETH Center
 * and
 * Cambridge centre for Advanced Research and Education in Singapore LTD.
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


/*********************** Extension of script.js *******************************/


/************************ Process Query Results ******************************/
//Shiying: highlight multiple cityobjects, create customDatasource, pinCityobjects
function processFilteredObjects(cityObjectsArray, colorStr){  // citydbKmlLayer object, list of files in the folder--> get the summaryfile
    //var cityObjectsArray = ["UUID_fddf5c91-cdd6-436a-95e6-aa1fa199b75d", "UUID_e5779fd5-ea90-4d2c-9a0a-cf7f46e5aad3"];
    //var cityObjectsArray = ["http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_fddf5c91-cdd6-436a-95e6-aa1fa199b75d/", "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_e5779fd5-ea90-4d2c-9a0a-cf7f46e5aad3/", "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_b6f4d0de-cf5c-4917-aba0-c1a91fa4960b/"];

    // UUID_fddf5c91-cdd6-436a-95e6-aa1fa199b75d - inside
    // UUID_b6f4d0de-cf5c-4917-aba0-c1a91fa4960b - outside of the scene

    let currentLayer = webMap.activeLayer;
    let filteredResult= {};
    let highlightColor = currentLayer._highlightColor; // new Cesium.Color(16/255, 77/255, 151/255, 1.0);

    for (let i = 0; i < cityObjectsArray.length; i++) {
        let strArray = cityObjectsArray[i].split("/");
        let gmlid = strArray[strArray.length-2];
        filteredResult[gmlid] = highlightColor; // new Cesium.Color(65/255, 168/255, 255/255, 0.8);
    }

    filteredObjects = filteredResult;
    pinHighlightObjects(cityObjectsArray, colorStr);
    //highlightFilteredObj(filteredObjects);
    return filteredObjects;
}

function highlightFilteredObj(filteredObjects){
    let currentLayer = webMap.activeLayer;
    currentLayer.unHighlightAllObjects();
    if (filteredObjects !== undefined){
        currentLayer.highlight(filteredObjects);
    }
}

function pinHighlightObjects(cityObjectsArray, hexColorString){
    let gmlidArray = [];

    for (let i = 0; i < cityObjectsArray.length; i++) {
        let strArray = cityObjectsArray[i].split("/");
        let gmlid = strArray[strArray.length-2];
        gmlidArray.push(gmlid);
    }

    // get geolocation
    let currentLayer = webMap._activeLayer;
    let testcityobjectsJsonData;
    if (Cesium.defined(currentLayer)) {
        testcityobjectsJsonData = currentLayer.cityobjectsJsonData;
    }

    let dataUnifier = dataSourcePrefix + _customDataSourceCounter;
    let customDataSource = new Cesium.CustomDataSource(dataUnifier);
    //var currentLayer = webMap.activeLayer;

    for (let i = 0; i < gmlidArray.length; i++){
        let obj = testcityobjectsJsonData[gmlidArray[i]];
        let id = gmlidArray[i];
        let lon = (obj.envelope[0] + obj.envelope[2]) / 2.0;
        let lat = (obj.envelope[1] + obj.envelope[3]) / 2.0;
        //console.log(gmlidArray[i] + ": " + lon + ", " + lat);

        addPoint(customDataSource, id, lat, lon, currentLayer ,hexColorString);
    }
    //addEventListeners(customDataSource);
    customDataSourceMap.set(dataUnifier, customDataSource);
    cesiumViewer.dataSources.add(customDataSource);

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

    let developmentType = document.getElementById("DevelopmentType");
    title.innerHTML = "Search " + developmentType.options[developmentType.selectedIndex].text;
    if (title.innerHTML === "Search Search plots...") {
        title.innerHTML = "Search example query"
    }

    title.style.fontWeight = "bold";
    title.style.fontSize = "12px";
    infoText.appendChild(title);
    infoText.appendChild(document.createElement("br"));

    //{"allowsProgramme":{"Cafe":"","StudentRunBusiness":""},"TotalGFA":""}
    let filteredCount = resultjson["http://www.theworldavatar.com:83/access-agent/access"]["filteredCounts"];
    let inputs = resultjson["context"]["http://www.theworldavatar.com:83/access-agent/access"];
    let allowOption;
    let allowsObjects = [];
    let allowsGFA = [];
    if (Object.keys(inputs).includes("allowsProgramme")){
        allowsObjects = Object.keys(inputs["allowsProgramme"]);
        allowsGFA = Object.values(inputs["allowsProgramme"]);
        allowOption = "allowsProgramme:";
    } else if (Object.keys(inputs).includes("allowsUse")){
        allowsObjects = Object.keys(inputs["allowsUse"]);
        allowsGFA = Object.values(inputs["allowsUse"]);
        allowOption = "allowsUse:";
    }
    let allowOpts = document.createElement("span");
    allowOpts.textContent = allowOption;
    infoText.appendChild(allowOpts);
    infoText.appendChild(document.createElement("br"));

    let allowOptText = document.createElement("span");
    let allowsContent = "";
    for (i = 0; i < allowsObjects.length; ++i){
        if (allowsGFA[i] !== ""){
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
        let totalGFA = document.createElement("span");
        let totalGFAValue = inputs["TotalGFA"];
        totalGFA.textContent = "Total GFA: " + totalGFAValue + " sqm";
        infoText.appendChild(totalGFA);
        infoText.appendChild(document.createElement("br"));
    }

    let objCount = document.createElement("span");
    objCount.textContent = "# search results: " + filteredCount + " plots";
    infoText.appendChild(document.createElement("br"));
    infoText.appendChild(objCount);
    infoText.appendChild(document.createElement("br")); // Ayda: swapped white line positions according Pieter's instructions.

    // static part: measure the impact of the demo
    let docCount = document.createElement("span");
    if (ifGFA){
        docCount.innerHTML = "The search replaces manually <br>checking 38 regulatory documents per plot";
    }else {
        docCount.innerHTML = "The search replaces manually <br>checking 17 regulatory documents per plot";

    }
    infoText.appendChild(document.createElement("br"));
    infoText.appendChild(docCount);

    return infoText;
}


/************************ Process CIA Agent Results **************************************/

function showResultWindow(resultJson, colorStr){

    let resultBoxTitle = document.getElementById("resultBox-title");
    resultBoxTitle.style.visibility = "visible";
    let resultBox = document.getElementById("resultBox-iframe");
    resultBox.style.visibility = "visible";
    let resultBoxContent = document.createElement("div");
    resultBoxContent.style.display = "block";
    let listItem = document.createElement("li");
    listItem.id = dataSourcePrefix + _customDataSourceCounter;
    //listItem.title = colorStr;

    listItem.appendChild(processInfoContext(resultJson));
    // <input type='checkbox' onchange='updateGfaRows()'>

    let checkbox = document.createElement("input");
    checkbox.type = 'checkbox';
    checkbox.className = 'ciaResults';
    checkbox.checked = true;
    checkbox.onchange = function (){
        updateSelectedDataSources();
    };
    listItem.appendChild(checkbox);

    let colorbox = document.createElement('div');
    colorbox.className = "colorbox";
    colorbox.style = "width: 12px; height: 12px; display: inline-block;";
    colorbox.style.backgroundColor = colorStr;
    colorbox.title = colorStr;
    listItem.appendChild(colorbox);
    let closeButton = document.createElement("span");
    closeButton.className = "close";
    closeButton.textContent = "x";
    closeButton.onclick = function (event){
        let targetId = event.currentTarget.parentNode.id;
        //var targetUnusedColor = event.currentTarget.parentNode.title;
        let targetUnusedColor = event.currentTarget.parentNode.getElementsByClassName("colorbox")[0].title;  // can store the color in the title of li item. which is the description of the corresponding color
        removeDataSourceById(targetId, targetUnusedColor);
    }
    listItem.appendChild(closeButton);
    resultBoxContent.appendChild(listItem);
    resultBox.appendChild(resultBoxContent);

    let closebtns = document.getElementsByClassName("close");


    for (let i = 0; i < closebtns.length; i++) {
        closebtns[i].addEventListener("click", function() {
            console.log("this has been removed: " + this.parentNode.id);
            this.parentNode.remove();
            if (closebtns.length === 0){
                var resultBoxTitle = document.getElementById("resultBox-title");
                resultBoxTitle.style.visibility = "hidden";
            }
        });
    }
}

// When click on the checkbox, it will set the datasource visible
function updateSelectedDataSources(){ 
    // Only remove ciaResults DataSources, so it does not affect demo2
    for (let [key, value] of customDataSourceMap){
        if (key.includes('ciaResult')){
            cesiumViewer.dataSources.remove(value);
        }
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
    let iriArr = [];
    let highlightedObjects = webMap.getAllHighlighted3DObjects();
    let centroids = [];
    for (let i = 0; i < highlightedObjects.length; i++) {
        let entity = highlightedObjects[i][0];
        console.log(entity.polygon);
        let positions = entity.polygon.hierarchy._value.positions;
        let center = Cesium.BoundingSphere.fromPoints(positions).center;
        console.log(center);
        centroids.push(center);
        let iri = entity._iriPrefix + entity._name;
        iri = iri.endsWith('/') ? iri : iri + '/';
        iri = iri.replace(new RegExp('_(\\w*Surface)'), '');
        iriArr.push(iri);
    }
    console.log(centroids);

    let labelText = '';
    let label = cesiumViewer.entities.add({
        name: 'Distance label',
        position: getMidpoint(centroids[0], centroids[1]),
        label: {
            font : 'bold 22px arial',
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
        success: function(data){
            console.log(data["distances"]);
            var distance = Math.round(data["distances"][0]);
            label.label.text = distance.toString() + " " + "m";
        }
    });

}

function getMidpoint(point1, point2) {
    let geodesic = new Cesium.EllipsoidGeodesic();
    let scratch = new Cesium.Cartographic();

    let point1cart = Cesium.Cartographic.fromCartesian(point1);
    let point2cart = Cesium.Cartographic.fromCartesian(point2);

    geodesic.setEndPoints(point1cart, point2cart);
    let midpointCartographic = geodesic.interpolateUsingFraction(0.5, scratch);
    return Cesium.Cartesian3.fromRadians(midpointCartographic.longitude, midpointCartographic.latitude);

}

