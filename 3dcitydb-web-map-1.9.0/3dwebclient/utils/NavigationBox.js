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



// This file contains the implementation of the Navigation and Introduction box on the right-hand side
/*************************************************** Bottom Navigation to Key Growth Area ****************************************/

/**
 * Set Area Counter
 * @param {String} direction - "backward" or "forward"
 *
 */
function setAreaCounter(direction){
    switch (direction) {
        case "backward":
            area_counter += -1;
            if (area_counter === -1) {
                area_counter = 3;
            }
            current_zoom_area = Object.keys(cameraPositions)[area_counter];
            break;
        case "forward":
            area_counter += 1;
            if (area_counter === 4) {
                area_counter = 0;
            }
            current_zoom_area = Object.keys(cameraPositions)[area_counter];
            break;
        default:
            current_zoom_area = Object.keys(cameraPositions)[0];
            break;
    }
    console.log(current_zoom_area);
}


/**
 * Zoom to the key growth areas
 * @param {String} direction - "backward" or "forward"
 *
 */
function zoomToKeyGrowthAreas(direction) {
    resetAllInputs();
    setAreaCounter(direction);
    let zoomed_area = document.getElementById('area');
    let zoomed_area_name = Object.keys(cameraPositions)[area_counter];
    let zoomed_camera_position = cameraPositions[zoomed_area_name]
    let current_url = new URL(window.location.href);

    for (const coord of ['longitude', 'latitude']) {
        if (current_url.searchParams.has(coord)) {
            current_url.searchParams.set(coord, zoomed_camera_position[coord]);
        }
        else {
            current_url.searchParams.append(coord, zoomed_camera_position[coord]);
        }
    }

    if (current_url.searchParams.has('region')) {
        current_url.searchParams.set('region', zoomed_area_name);
    }
    else {
        current_url.searchParams.append('region', zoomed_area_name);
    }

    history.replaceState({}, '', current_url.href.toString())
    zoomed_area.innerHTML = zoomed_area_name.replaceAll('_',' ');
    flyToCameraPosition(zoomed_camera_position);
}


/*************************************************** Side Bar for Demo ****************************************/

/**
 * Open the side bar
 *
 */
function openSidebar() {
    if (document.getElementById("mySidebar").style.width === "150px"){
        document.getElementById("mySidebar").style.width = "0";
        document.getElementById("ProgrammaticPlotFinder").style.marginLeft= "0";
    }else{
        document.getElementById("mySidebar").style.width = "150px";
        document.getElementById("ProgrammaticPlotFinder").style.marginLeft = "150px";
    }
}

/**
 * Close the side bar
 *
 */
function closeSidebar() {
    document.getElementById("mySidebar").style.width = "0";
    document.getElementById("ProgrammaticPlotFinder").style.marginLeft= "0";
}

/*************************************************** Introduction Box ****************************************/

/**
 * Define disclaimer button
 *
 */
function addDisclaimerButton(){

    let toolbar = document.getElementsByClassName('cesium-viewer-toolbar');  // HTMLCollection; has to use item(0) to add an additional icon

    let wrapper = document.createElement('span');
    wrapper.className = 'cesium-navigationHelpButton-wrapper';
    toolbar.item(0).appendChild(wrapper);

    var instructionIcon = document.createElement('img');
    instructionIcon.src = '../3dwebclient/utils/image-source/infoIcon.png';
    instructionIcon.style = "width:29px; height:29px;";

    var disclaimerButton = document.createElement('button');
    disclaimerButton.type = 'button';
    disclaimerButton.className = 'cesium-button cesium-toolbar-button cesium-navigation-help-button';
    disclaimerButton.setAttribute('data-bind', 'attr: { title: tooltip }');
    disclaimerButton.appendChild(instructionIcon);

    wrapper.appendChild(disclaimerButton);

    createTapMenu();

    disclaimerButton.onclick = function(){
        //instructionContainer.classList.add('cesium-navigation-help-visible');
        if (instructionContainer.style.display !== "none"){
            instructionContainer.style.display = "none";
        }else{
            instructionContainer.style.display = "block";
        }
    };

}

/**
 * Define Tap menu for the introduction box on the right-hand side
 *
 */
function createTapMenu(){
    // Define the instruction DIV
    var instructionContainer = document.getElementById('instructionContainer');
    instructionContainer.className = 'cesium-navigation-help cesium-navigation-help-visible';
    instructionContainer.setAttribute('data-bind', 'css: { "cesium-navigation-help-visible" : showInstructions}');
    instructionContainer.style = 'width: 25%; max-width: 430px; height: auto; top: 43px; right: 8px; filter:none; z-index:99999; display: block';

    // By default: descriptionTap is selected
    let descriptionTap = createTapButton("Description");
    descriptionTap.onclick = function (event) {
        activeTap(event, 'Description');
    }
    descriptionTap.className = descriptionTap.className.replace("cesium-navigation-button-unselected", "cesium-navigation-button-selected");

    let terminologyTap = createTapButton("Terminology");
    terminologyTap.onclick = function(event){
        activeTap(event, 'Terminology');
    };

    let disclaimerTap = createTapButton("Disclaimer");
    disclaimerTap.onclick = function(event){
        activeTap(event, 'Disclaimer');
    };

    instructionContainer.appendChild(descriptionTap);
    instructionContainer.appendChild(terminologyTap);
    instructionContainer.appendChild(disclaimerTap);

    // add Description content
    let descriptionContent = document.createElement("div");
    descriptionContent.id = "Description";
    descriptionContent.className = 'cesium-navigation-help-instructions tabcontent';
    descriptionContent.style.display = "block";
    descriptionContent.innerHTML = '\
            <div class="cesium-navigation-help-zoom" style="padding: 15px 5px 20px 5px; text-align: center; color: #ffffff">Programmatic Plot Finder</div>\
            <hr width=50% style="margin-top: -10px; border-color: grey;">\
            <div class="infobox_content">The Programmatic Plot Finder is the first demonstrator of the Cities Knowledge \
            Graph project. It enables searching for plots that allow: </div>\
            <div class="infobox_content">1. particular combinations of land uses or programmes (essentially more specific \
            land uses)</div>\
            <div class="infobox_content">2. particular amounts, or gross floor areas (GFAs) of these combinations. \
            The unit of GFA is square meters (sqm), see following table for more details.</div>\
            <div class="cesium-navigation-help-zoom" style="padding: 15px 5px 20px 5px; text-align: center; color: #ffffff">Example GFA Values</div>\
            <hr width=50% style="margin-top: -10px; border-color: grey;">\
            <table id=exampleGFA style="color: #ffffff; margin: auto;">\
            <tr><th style="width:50%">Name</th><th style="width:50%">GFA (sqm)</th></tr>\
            <tr><td>Takashimaya</td><td>164,600</td></tr>\
						<tr><td>Singapore National Gallery</td><td>64,000</td></tr>\
						<tr><td>Vivocity</td><td>136,200</td></tr>\
			      </table>\
	';
    instructionContainer.appendChild(descriptionContent);

    // add Terminology content
    let terminologyContent = document.createElement("div");
    terminologyContent.id = "Terminology";
    terminologyContent.className = 'cesium-navigation-help-instructions tabcontent';
    terminologyContent.style.display = "none";
    terminologyContent.innerHTML = '\
            <div class="cesium-navigation-help-zoom" style="padding: 15px 5px 20px 5px; text-align: center; color: #ffffff">Terminology</div>\
            <hr width="50%" style="margin-top: -10px; border-color: grey;">\
            <div class="infobox_content">\
            	<span style="font-weight:bold; font-style: italic; color: ">Gross Floor Area (GFA)</span>\
							<p>In Singapore, authorities define GFA as ‘the total area of covered floor space measured between the centre line of party walls, including the thickness of external \
							walls but excluding voids’. </p></div>\
            <div class="infobox_content">\
            	<span style="font-weight:bold; font-style: italic;">Land Uses</span>\
            	<p>Broad land use categories that are allowed in a specific zoning type. Land uses may have further regulations attached, e.g. maximum use quantums. For example, \
            	a land use might be ‘Commercial or Hotel Use in Commercial Zone’. This land use is broad in the sense that a commercial use encompasses more specific uses like ‘restaurant’, \
            	‘bar’ and ‘bookstore.’ This land use is necessary to describe at this level of detail because in Singapore, a minimum use quantum of 60% applies to it, meaning that \
            	at least 60% of the GFA of a plot zoned as Commercial must be devoted to a mix of different commercial or hotel uses.</p></div>\
  					<div class="infobox_content">\
  						<span style="font-weight:bold; font-style: italic;">Programmes</span>\
  						<p>More specific land uses that are allowed in one or more broader land use categories. For example, ‘restaurant’, ‘bar’ and ‘bookstore’ are programmes. No use quantums \
  						are attached to programmes.</p></div>\
						<div style="padding: 10px 5px 5px 5px;"><img src="../3dwebclient/utils/image-source/class_diagram.png" style="width: 100%;"/></div>\
						';
    instructionContainer.appendChild(terminologyContent);

    // add Disclaimer content
    let disclaimerContent = document.createElement("div");
    disclaimerContent.id = "Disclaimer";
    disclaimerContent.className = 'cesium-navigation-help-instructions tabcontent';
    disclaimerContent.style.display = "none";
    disclaimerContent.innerHTML = '\
            <div class="cesium-navigation-help-zoom" style="padding: 15px 5px 20px 5px; text-align: center; color: #ffffff">Disclaimer</div>\
            <hr width="50%" style="margin-top: -10px; border-color: grey;">\
            <div class="infobox_content">Plot search by allowable programmes and uses is based on the Master Plan 2019 written \
            statement and does not integrate other regulations that may affect allowable uses or programmes.</div>\
            <div class="infobox_content">Allowable GFA values were calculated without modelling rules for mixed-use zoning types, \
            such as Education or Institution, Commercial & Residential, White or plots in strata landed housing.</div>\
  					<div class="infobox_content">The current GFA model represents buildable space up to Level of Detail (LoD) \
  					1. The integration of higher LoD regulatory data will improve the accuracy of buildable space and the allowable GFA values.</div>\
	';
    instructionContainer.appendChild(disclaimerContent);

    return instructionContainer;
}


/**
 * Create Tap Button based on the given button name
 * @param {String} buttonName - the name of the tab button
 *
 */
function createTapButton(buttonName){
    let menuTapButton = document.createElement('button');
    //menuTapButton.id = buttonName;
    menuTapButton.type = 'button';
    menuTapButton.className = 'cesium-navigation-button cesium-navigation-button-unselected tablinks';
    menuTapButton.style = "height: 38px; width: 33.33%; font-size: 15px;";
    //menuTapButton.setAttribute('data-bind', 'click: showClick, css: {"cesium-navigation-button-selected": !_touch, "cesium-navigation-button-unselected": _touch}');
    menuTapButton.appendChild(document.createTextNode(buttonName));
    return menuTapButton;
}

/**
 * Create Tap Button based on the given button name
 * @param {MouseEvent} evt - mouse click event
 * @param {String} tapName - the name of the tap
 *
 */
function activeTap(evt, tapName) {
    let i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName("tabcontent");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }
    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
        tablinks[i].className = tablinks[i].className.replace("cesium-navigation-button-selected", "cesium-navigation-button-unselected");
    }
    document.getElementById(tapName).style.display = "block";
    evt.currentTarget.className += " active";
    evt.currentTarget.className = evt.currentTarget.className.replace("cesium-navigation-button-unselected", "cesium-navigation-button-selected");
}