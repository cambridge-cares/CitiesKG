/*********************** Extension of script.js *******************************/

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

