// global variables
let selectedPlotsId = [];


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
}



function showSelection() { 
    console.log("showSelection clicked!");
    // Clear the list of "selected plots" before showing selected list
    var selectedPlotsList = document.getElementById("selectedPlotsList");
    selectedPlotsList.innerHTML = '';

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
        selectedPlotsList.appendChild(option);
        selectedPlotsId.push(highlightedObjects[i]);
    }
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

    openBottomNav();
    
    setTableVisibility(selectedPlotsId.length, "grid");
    
    // Alert
    //if (selectedPlotsId.length == 0){
    //    window.alert("No plot is selected for comparison !!");
    //}
    
}

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






function createCard(){

}



function createTable(){
    let comparisonContent = document.getElementsById('comparisonContent');
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




function createIndexPanel(){
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