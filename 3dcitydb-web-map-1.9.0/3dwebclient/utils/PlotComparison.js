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

function startComparison(){
    console.log("startComparison clicked!");
    console.log(selectedPlotsId);
    //createTable();
    if (selectedPlotsId.length == 0){
        window.alert("No plot is selected for comparison !!");
    }
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