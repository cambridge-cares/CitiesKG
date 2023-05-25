// global variables
let selectedPlotsId = [];
let colorSpaceForPlots = ["#845EC2", "#D65DB1", "#FFC75F", "#0081CF", "#00C9A7"];
let labelForPlots = ["A", "B", "C", "D","E"];
let selectedPlotsUnifier = "SSS_selectedPlots";

/*
function listHighlightedObjects() {
    var highlightingListElement = document.getElementById("selectedPlotsList");

    emptySelectBox(highlightingListElement, function() {
        var highlightedObjects = webMap.getAllHighlightedObjects();
        for (var i = 0; i < highlightedObjects.length; i++) {
            var option = document.createElement("option");
            option.text = highlightedObjects[i];
            highlightingListElement.add(option);
            highlightingListElement.selectedIndex = 0;
        }
    });
}*/



function showSelection() { 
    console.log("showSelection clicked!");
    // Clear the list of "selected plots" before showing selected list
    var selectedPlotsList = document.getElementById("selectedPlotsList");
    selectedPlotsList.innerHTML = '';
    for (let [key, value] of customDataSourceMap){
        if (key == selectedPlotsUnifier){
            cesiumViewer.dataSources.remove(value);
            customDataSourceMap.delete(key);
        }
    }

    var highlightedObjects = webMap.getAllHighlightedObjects(); // this will return the selected objects even the plots are unloaded. 
    
    var selectedTitle = document.getElementById("selectplots-title");
    selectedTitle.style.visibility = "visible";
    var selectedPlotsList = document.getElementById("selectedPlotsList");
    selectedPlotsList.style.visibility = "visible";
    selectedPlotsList.style.background = "#edffff";
    
    // always empty the global variable selectedPlotsId before filling in
    selectedPlotsId = [];

    for (var i = 0; i < highlightedObjects.length; i++) {
        var option = document.createElement("option");
        option.style.color = "black";
        option.style.padding = "5px 10px 5px 10px";
        option.innerHTML = highlightedObjects[i];
        option.style.color = colorSpaceForPlots[i];
        selectedPlotsList.appendChild(option);
        selectedPlotsId.push(highlightedObjects[i]);
    }

    pinSelectedPlots(selectedPlotsId);
    //highlight will be visible when the plot is visible again. 
    //listHighlightedObjects();
   
    //console.log(highlightedObjects);
}

function clearSelection() {
    clearhighlight();
    var selectedPlotsList = document.getElementById("selectedPlotsList");
    selectedPlotsList.innerHTML = '';
    console.log("clearSelection clicked!")
    selectedPlotsId=[];
    
    for (let [key, value] of customDataSourceMap){
        if (key == selectedPlotsUnifier){
            cesiumViewer.dataSources.remove(value);
            customDataSourceMap.delete(key);
        }
    }
}
function setTableVisibilityAndTitle (selectedPlots, status){
    for (var i = 1; i < selectedPlots.length+1; i++) {
        let tableVisibility = document.getElementById("table"+ i);
        tableVisibility.style.display = status;
        //tableVisibility.getElementsByClassName("whole-row").style.setProperty("color", colorSpaceForPlots[i-1]);
        //tableVisibility.style.backgroundColor = colorSpaceForPlots[i-1];
        let selected = '#table'+ i +' .whole-row';
        document.querySelector(selected).style.setProperty("background-color", colorSpaceForPlots[i-1]);    
    }
}

function setTableVisibility(numberOfTable, status){
    for (var i = 1; i < numberOfTable+1; i++) {
        let tableVisibility = document.getElementById("table"+ i);
        tableVisibility.style.display = status;   
    }
}

function startComparison(){
    console.log("startComparison clicked!");
    console.log(selectedPlotsId);
    
    // Reset the comparison panel 
    setTableVisibility(5, "none");
    // TODO: Empty the content of all the table
    resetTableContentAll();

    openBottomNav();
    
    // Set corresponding table visible
    setTableVisibility(selectedPlotsId.length, "grid");

    // Copy table template and change the ID
    createTableContent(selectedPlotsId);

    setPlotIdintoTable(selectedPlotsId);
    //queryDistanceFilter(selectedPlotsId);

    // Alert
    //if (selectedPlotsId.length == 0){
    //    window.alert("No plot is selected for comparison !!");
    //} 
}

/** 
function queryDistanceFilter(selectedPlotsId){
    for (var i = 0; i < selectedPlotsId.length; i++) {

    const iri = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/" + selectedPlotsId[i] +"/";
        $.ajax({
            url: "http://localhost:8080/agents/cityobjectinformation",
            type: 'POST',
            data: JSON.stringify({'iris': [iri], 'searchDistance': 500),
            dataType: 'json',
            contentType: 'application/json',
            success: function (data) { //function (data, status_message, xhr)
                //console.log(data["http://www.theworldavatar.com:83/access-agent/access"]["filtered"]);
                //console.log(data["http://www.theworldavatar.com:83/access-agent/access"]["filteredCounts"]);
                console.log(data["allowableUSEandGFA"]);
            }
        });
    }
}

*/





function resetTableContentAll(){
    for (var i = 1; i < 6; i++){
        document.getElementById("table"+i).innerHTML = '';
    }
}



function createTableContent(plotIdList){

    const table_template = document.getElementById("table-template");
    for (var i = 1; i < plotIdList.length+1; i++){
        document.getElementById("table"+i).appendChild(table_template.content.cloneNode(true));
        //console.log(document.getElementById("table"+i));
    }
}


// set plotId and background color
function setPlotIdintoTable(selectedPlots){
    for (var i = 1; i < selectedPlots.length+1; i++) {
        let tableVisibility = document.getElementById("table"+ i);
        let selected = '#table'+ i +' .item4';
        if (tableVisibility.style.display == "grid"){
            let selectedSpan = document.querySelector(selected).firstChild;
            document.querySelector(selected).style.setProperty("background-color", colorSpaceForPlots[i-1]);
            selectedSpan.innerHTML = selectedSpan.innerHTML.replace('{plotid}', selectedPlots[i-1]); 
        }
    }
}




// Control the bottom comparison panel
function expandPanel(){
    let expandArrow = document.getElementById("expandArrow");
    expandArrow.classList.toggle("arrowActive");

    if (document.getElementById("comparisonPanel").style.height === "300px"){
        (document.getElementById("comparisonPanel").style.height === "30px")
        
    }else{
        document.getElementById("comparisonPanel").style.height = "300px";
        
    }
}

function openBottomNav() {
    document.getElementById("comparisonPanel").style.height = "50%";
    //document.getElementById("main").style.marginBottom = "250px";
}
  
function closeBottomNav() {
    document.getElementById("comparisonPanel").style.height = "0";
    //document.getElementById("main").style.marginLeft= "0";
}


// Draw the points on the selected plots
function pinSelectedPlots(cityObjectsArray){

    // get geolocation
    var currentLayer = webMap._activeLayer;
    var testcityobjectsJsonData;
    if (Cesium.defined(currentLayer)) {
        testcityobjectsJsonData = currentLayer.cityobjectsJsonData;
    }


    var customDataSource = new Cesium.CustomDataSource(selectedPlotsUnifier);
    var currentLayer = webMap.activeLayer;

    for (let i = 0; i < cityObjectsArray.length; i++){
        var obj = testcityobjectsJsonData[cityObjectsArray[i]];
        var id = cityObjectsArray[i];
        var lon = (obj.envelope[0] + obj.envelope[2]) / 2.0;
        var lat = (obj.envelope[1] + obj.envelope[3]) / 2.0;
        //console.log(gmlidArray[i] + ": " + lon + ", " + lat);

        //pinPoint(customDataSource, id, lat, lon, currentLayer, colorSpaceForPlots[i]);
        addPin(customDataSource, id, lat, lon, currentLayer, colorSpaceForPlots[i], labelForPlots[i]);
    }
    //addEventListeners(customDataSource);
    customDataSourceMap.set(selectedPlotsUnifier, customDataSource);
    cesiumViewer.dataSources.add(customDataSource);
}

function addPin(customDataSource, pointId, lat, long, parentLayer ,hexColorString, label) {
    const pinBuilder = new Cesium.PinBuilder();
    customDataSource.entities.add({
        position: Cesium.Cartesian3.fromDegrees(long, lat, 5),
        id: pointId,
        billboard: {
            image: pinBuilder.fromText(label, Cesium.Color.fromCssColorString(hexColorString), 48).toDataURL(),
            verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
        },
        layerId: parentLayer.id,
        name: pointId,
        iriPrefix: parentLayer._citydbKmlDataSource._iriPrefix,});
}

// Unused: as addPin is implemneted
function pinPoint(customDataSource, pointId, lat, long, parentLayer ,hexColorString){

    customDataSource.entities.add({
        position: Cesium.Cartesian3.fromDegrees(long, lat, 50),
        id: pointId,
        point: {
            pixelSize: 15,
            color: Cesium.Color.fromCssColorString(hexColorString),
            outlineColor: Cesium.Color.BLACK,
            outlineWidth:1,
        },
        layerId: parentLayer.id,
        name: pointId,
        iriPrefix: parentLayer._citydbKmlDataSource._iriPrefix,
    });
}



function createTable_backup(){
    let comparisonContent = document.getElementById('comparisonContent');
    let FirstTable = document.createElement("table");
    FirstTable.style.width = "500px";
    FirstTable.className = "table table-bordered";
    FirstTable.innerHTML = '<thead><tr>\
      <th scope="col">#</th>\
      <th scope="col">First</th>\
      <th scope="col">Last</th>\
      <th scope="col">Handle</th>\
    </tr></thead>\
    <tbody><tr>\
      <th scope="row">1</th>\
      <td>Mark</td>\
      <td>Otto</td>\
      <td>@mdo</td>\
    </tr><tr>\
      <th scope="row">2</th>\
      <td>Jacob</td>\
      <td>Thornton</td>\
      <td>@fat</td>\
    </tr><tr>\
      <th scope="row">3</th>\
      <td colspan="2">Larry the Bird</td>\
      <td>@twitter</td>\
    </tr></tbody>\
    ';
    comparisonContent.appendChild(FirstTable);
}




function createIndexPanel_backup(){
    let indexTable = document.createElement("table");
    indexTable.className = "table table-hover";
    indexTable.innerHTML = '\
    <thead><tr><th scope="col">**</th></tr></thead>\
    <tbody><tr><th scope="row">GFA</th></tr>\
    <tr><th scope="row">MixedUse</th></tr>\
    <tr><th scope="row">Solar potential</th></tr>\
    <tr><th scope="row">Number of Parks</th></tr>\
    </tbody>\
    ';
}