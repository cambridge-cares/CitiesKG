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

const CONTEXT = "citieskg";
const CITY = (new URL(window.location.href).searchParams.get('city'));
const TOTAL_GFA_KEY = 'TotalGFA';
const MAX_CAP = 'max_cap';
const MIN_CAP = 'min_cap';
let input_parameters;
let selectedDevType;
let click_counter = 0;
let area_counter = 0;
let current_zoom_area;
const cameraPositions = {
	'Singapore_River_Valley': {latitude:  1.275, longitude: 103.84, height: 3000, heading: 360, pitch: -60, roll: 356 },
	'Punggol_Digital_District' : {latitude: 1.400, longitude: 103.911385, height: 3000, heading: 360, pitch: -60, roll: 356},
	'Paya_Lebar_Air_Base': {latitude: 1.325, longitude: 103.906240, height: 3000, heading: 360, pitch: -60, roll: 356},
	'Woodlands_Centre': {latitude:  1.425, longitude: 103.789, height: 3000, heading: 360, pitch: -60, roll: 356 }}

let requestCounter = 0;

/**
 * Build SPARQL to retrieve the dropdown list for given predicate
 * @param {String} predicate - 'allowsUse' or 'allowsProgramme'
 * @param {String} may_predicate - 'mayAllowUse' or 'mayAllowProgramme'
 * @return {String} SPARQL string
 */
function buildDropdownQuery(predicate, may_predicate){
	return "PREFIX zo:<http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#> "
			+ "SELECT DISTINCT ?g WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/> "
			+ "{ ?zone zo:" + predicate + " | zo:" + may_predicate+ " ?g . } }"
}

/**
 * Check if the Key predicate already exists in the local storage
 * @param {String} predicate - 'allowsUse' or 'allowsProgramme'
 * @return {Boolean} true or false
 */
function ifDropdownElementExists(predicate){
	return localStorage.getItem(predicate) != null;
}

/**
 * Process and Store the results retrieved from the TWA into localStorage when it does not exist yet
 * @param {String} predicate - 'allowsUse' or 'allowsProgramme'
 * @param {JSON} results - JsonObject from CIA
 */
function storeDropdownElements(predicate, results){
	let resultsLength = results.length;
	let localData = results[0]["g"];
	for (let index = 1; index < resultsLength; ++index) {
		localData = localData.concat('\r',results[index]["g"]);
	}
	localStorage.setItem(predicate, localData);
}

/**
 * Retrieves the dropdownElements either from localStorage or TWA via access agent by sending a SPARQL
 * @param {String} predicate - 'allowsUse' or 'allowsProgramme'
 * @param {String} may_predicate - 'mayAllowUse' or 'mayAllowProgramme'
 * @param {String} element_type - 'use_element' or 'programme_element'
 * @param {String} dropdown_type - '#choose_uses' or '#choose_programmes'
 */
function getDropdownElements(predicate, may_predicate, element_type, dropdown_type) {

	// check if the dropdownElement list already exists in the LocalStorage
	// Warning: In order to get any update in TWA effective on WMC, the local storage need to be deleted.
	if (ifDropdownElementExists(dropdown_type)){
		let dropdownData = localStorage.getItem(dropdown_type);
		let checkbox_lines = [];
		let results = dropdownData.split('\r');
		for (let index in results) {
			let checkbox_line = removePrefix(results[index]);
			checkbox_lines.push(checkbox_line);
		}
		checkbox_lines.sort();
		for (let index in checkbox_lines) {
			appendElement(checkbox_lines[index], predicate, element_type, dropdown_type)
		}
	} else {
		$.ajax({
			url:"http://www.theworldavatar.com:83/access-agent/access",
			//url:"http://localhost:48888/access-agent/access",
			type: 'POST',
			data: JSON.stringify({targetresourceiri:CONTEXT + "-" + CITY , sparqlquery: buildDropdownQuery(predicate, may_predicate)}),
			//data: JSON.stringify({targetresourceiri:"http://localhost:48888/test" , sparqlquery: buildDropdownQuery(predicate, may_predicate)}),
			dataType: 'json',
			contentType: 'application/json',
			success: function(data, status_message, xhr){
				console.log(data["result"])
				let results = JSON.parse(data["result"]);
				let checkbox_lines = [];
				storeDropdownElements(dropdown_type, results);
				for (let index in results) {
					let checkbox_line = removePrefix(results[index]["g"]);
					checkbox_lines.push(checkbox_line);
				}
				checkbox_lines.sort();
				for (let index in checkbox_lines) {
					appendElement(checkbox_lines[index], predicate, element_type, dropdown_type)
				}
			},
			error: function(XMLHttpRequest, textStatus, errorThrown) {
				alert("Status: " + textStatus); alert("Error: " + errorThrown);
			}
		});
	}
}

/**
 * Create the checkbox for dropdown list and checkboxes
 * @param {String} line - checkbox name
 * @param {String} predicate - 'allowsUse' or 'allowsProgramme'
 * @param {String} element_type - 'use_element' or 'programme_element'
 * @param {String} dropdown_type - '#choose_uses' or '#choose_programmes', jQuery identifier of HTML elements
 */
function appendElement(line, predicate, element_type, dropdown_type){
	let some_element =
			"<div class='" + predicate + "'>" +
			"<div id='" + line + "' class='checkbox'>" +
			"<input type='checkbox' onchange='updateGfaRows()'></div>" +
			"<div class=" + element_type + ">" + line + "</div>" +
			"</div>";
	$(dropdown_type).append(some_element)
}

/**
 * Modify the human readable sentence for the input selection
 * @param {Boolean} programme_bool -
 * @param {Boolean} use_bool -
 * @param {Boolean} programmeGFA_bool -
 * @param {Boolean} useGFA_bool -
 * @param {String} sqm -
 * @param {String} sentence -
 * @param {Object} programmes -
 * @param {Object} uses -
 * @param {String} final_sentence - input final_sentence
 * @return {String} Modified final_sentence
 */
function processQuerySentence(programme_bool, use_bool, programmeGFA_bool, useGFA_bool, sqm, sentence, programmes, uses, final_sentence) {
	if(programme_bool) {
		for (let [programme, programmeGFAvalue] of Object.entries(programmes)) {
			if (programmeGFA_bool) {
				if (programmeGFAvalue === ""){
					programmeGFAvalue = "any";
				}
				sentence += programmeGFAvalue + sqm + " of " + "<b>" + programme + "</b>" + " (or more) and ";
			}
			else {
				sentence +=  "<b>" + programme + "</b>" + " and ";
			}
		}
		final_sentence = sentence.slice(0, -5) + ".";

	}
	else if (use_bool) {
		for (let [use, useGFAvalue] of Object.entries(uses)) {
			if (useGFA_bool) {
				if (useGFAvalue === ""){
					useGFAvalue = "any";
				}
				sentence += useGFAvalue + sqm + " of " + "<b>" + use + "</b>"+ " (or more) and ";
			}
			else {
				sentence += "<b>" + use + "</b>"+ " and ";
			}
		}
		final_sentence = sentence.slice(0, -5) + ".";
	}
	else {
		final_sentence = "Find plots that could allow...";
	}
	return final_sentence;
}

/**
 * Validate the GFA-related inputs. For NAN or negative numbers, it throws error
 * @return {Boolean} True or False
 */
function validateGFAInputs(){
	let inputs = document.getElementsByClassName('text_input');

	for (let index in inputs){
		let inputNumber = Number(inputs[index].firstChild.value);  // if letters, the conversion will return NaN
		if (Number.isNaN(inputNumber) || inputNumber < 0){
			throwNotification();
			return false;
		}
	}
	return true;
}

/**
 * Create human readable QuerySentence from all explicit inputs
 *
 */
function getQuerySentence(){
	let sentence = "Find plots that could allow...";
	let final_sentence = 'Find plots that could allow...';
	let sentence_ormore = "Find plots that could allow ";
	const sqm = " sqm";
	document.getElementsByClassName('querySentence')[0].textContent = final_sentence;

	let inputs = document.getElementsByClassName('text_gfa');

	let uses = {};
	let programmes = {};
	let totalGFA_input;
	let programmeGFA_bool = false;
	let useGFA_bool = false;

	for (let i = 0; i < inputs.length; i++) {
		let inputs_item = inputs.item(i).firstChild;
		let sibling = inputs.item(i).nextElementSibling.firstChild;
		let checkbox = document.getElementById(inputs.item(i).firstChild.textContent);
		if (checkbox !== null) {
			let inputs_item_onto_class = inputs_item.textContent;
			if (checkbox.parentElement.className === USE_PREDICATE) {
				uses[inputs_item_onto_class] = sibling.value;
				if (!useGFA_bool && !(sibling.value === "")) {
					useGFA_bool = true;
				}
			} else {
				programmes[inputs_item_onto_class] = sibling.value;
				if (!programmeGFA_bool && !(sibling.value === "")) {
					programmeGFA_bool =  true;
				}
			}
		} else {
			totalGFA_input = sibling.value;
		}
	}
	let programme_bool = Object.keys(programmes).length > 0;
	let use_bool = Object.keys(uses).length > 0;

	if (!(totalGFA_input === '')) {
		if (programme_bool || use_bool) {
			sentence = sentence_ormore + totalGFA_input + sqm + " development, combining ";
			final_sentence = processQuerySentence(programme_bool, use_bool, programmeGFA_bool, useGFA_bool, sqm, sentence, programmes, uses, final_sentence);
		} else {
			final_sentence = sentence_ormore + totalGFA_input + sqm + " (or more) of something?";
		}
	} else {
		sentence = sentence_ormore;
		if(programmeGFA_bool || useGFA_bool){
			final_sentence = processQuerySentence(programme_bool, use_bool, programmeGFA_bool, useGFA_bool, sqm,sentence, programmes, uses, final_sentence);
		} else {
			final_sentence = final_sentence = processQuerySentence(programme_bool, use_bool, programmeGFA_bool, useGFA_bool, sqm, sentence, programmes, uses, final_sentence);
		}
	}

	if (document.getElementById('CapType').value === 'max_cap') {
		final_sentence = final_sentence.slice(0,5) + "the 10 largest plots (by GFA) " + final_sentence.slice(11);
	}
	if (document.getElementById('CapType').value === 'min_cap') {
		final_sentence = final_sentence.slice(0,5) + "the 10 smallest plots (by GFA) " + final_sentence.slice(11);
	}
	document.getElementsByClassName('querySentence')[0].innerHTML = final_sentence;
}


/**
 * Restrict the input possiblity for GFA rows
 * @param {String} input - input value for the field
 */
function restrictInput(input){
	input.value = input.value.replace(/[^0-9.]/g, '').replace(/(\..*)\./g, '$1');
}

/**
 * Update the GFA selection rows based on the selection of the checkboxes
 *
 */
function updateGfaRows(){
	let parent = document.getElementById("assignGFA");
	let children = Array.from(parent.children);
	for (let i = 0; i < children.length; i++) {
		let child = children[i];
		if (child.className === 'gfa') {
			let checkbox_id = child.firstChild.textContent;
			let checkbox = document.getElementById(checkbox_id).firstChild;
			let text_input = child.childNodes[1].firstChild;
			if (!checkbox.checked || (text_input.value === "")) {
				parent.removeChild(child);
			}
		}
	}
	let checkboxes = document.getElementsByClassName('checkbox')
	let text_inputs = Array.from(document.getElementsByClassName('text_gfa'));
	for (let i = 0; i < checkboxes.length; i++) {
		checkbox = checkboxes.item(i).firstChild;
		if (checkbox.checked) {
			checkbox_id = checkboxes.item(i).id.toString();
			let exists = false;
			for (let i = 0; i < text_inputs.length; i++) {
				var checkbox_label = text_inputs[i].firstChild.textContent.toString();
				if (checkbox_label === checkbox_id) {
					exists = true;
					break;
				}
			}
			if (!exists) {
				let clicked_element =
						"<div class='gfa'>" +
						"<div class='text_gfa' style='width: 180px'>" + checkboxes.item(i).id + "</div>" +
						"<div class='text_input' title='Only positive integer is allowed'>" +
				    "<input type='text' maxLength='5' size='3' oninput='restrictInput(this)'></div>" +
						"<hr size='1'>" +
					  "</div>" ;
				$("#assignGFA").append(clicked_element)

			}
		}
	}
	getQuerySentence();
}


/**
 * Remove IRI Prefix from the result
 *
 */
function removePrefix(result){
	let element = result.split("#")[1]
	element = element.replace(/([A-Z])/g, " $1").trim();
	return element
}


/**
 * Get Example Query for button "Try Example Query"
 *
 */
function getExampleParams() {
	resetAllInputs();
	if(click_counter === 6) {
		click_counter = 0;
	}
	var query_example = [{TotalGFA:'', allowsUse: {ParkUse: ''}, min_cap: 'false', max_cap: 'false'},
		{TotalGFA:'', allowsProgramme: {Flat: '', Clinic:''}, min_cap: 'false', max_cap: 'false'},
		{TotalGFA:'', allowsProgramme: {PrintingPress: '', Gym:'', FoodLaboratory:''}, min_cap: 'false', max_cap: 'false'},
		{TotalGFA:'', allowsProgramme: {Condominium: ''}, min_cap: 'true', max_cap: 'false'},
		{TotalGFA:'', allowsProgramme: {Clinic: '100', Flat:'2000', Mall:'1000'}, min_cap: 'false', max_cap: 'false'},
		{TotalGFA:'', allowsProgramme: {Bank: '50000', Bar:'50000', BookStore:'50000'}, min_cap: 'false', max_cap: 'true'}];

	switch (click_counter){
		case 0:
			input_parameters = query_example[0];
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find plots that could allow " + "<b>" + "ParkUse" + "</b>" + ".";
			break;
		case 1:
			input_parameters = query_example[1];
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find plots that could allow " + "<b>" + "Flat" + "</b>" + " and " + "<b>" + "Clinic" + "</b>"+ ".";
			break;
		case 2:
			input_parameters = query_example[2];
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find plots that could allow " + "<b>" + "PrintingPress" + "</b>" + " and " + "<b>" + "Gym" + "</b>" + " and " + "<b>" + "FoodLaboratory" + "</b>" + ".";
			break;
		case 3:
			input_parameters = query_example[3];
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find the 10 smallest plots (by GFA) that could allow " + "<b>" + "Condominium" + "</b>" + ".";
			break;
		case 4:
			input_parameters = query_example[4];
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find plots that could allow " + "100 sqm of "+ "<b>" + "Clinic" + "</b>" + " (or more) and  2000 sqm of " + "<b>" + "Flat" + "</b>" +
					" (or more) and  1000 sqm of " + "<b>" + "Mall" + "</b>" + " (or more).";
			break;
		case 5:
			input_parameters = query_example[5];
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find the 10 smallest plots (by GFA) that could allow " + "50000 sqm of "+ "<b>" + "Bank" + "</b>" + " (or more) and 50000 sqm of " +
					"<b>" + "Bar" + "</b>" + " (or more) and  50000 sqm of " + "<b>" + "BookStore" + "</b>" + " (or more).";

			break;
	}
	click_counter += 1;
	console.log(input_parameters);
	getValidPlots();
}


/**
 * Get pre-defined example query for button "Try Example Query"
 *
 */
function addFiltering(input_parameters){
	var  filteringType = document.getElementById("CapType").value;
	//console.log(filteringType);
	switch (filteringType) {
		case "max_cap":
			input_parameters[MAX_CAP] = 'true';
			input_parameters[MIN_CAP] = 'false';
			break;
		case "min_cap":
			input_parameters[MAX_CAP] = 'false';
			input_parameters[MIN_CAP] = 'true';
			break;
		case "no_cap":
			input_parameters[MAX_CAP] = 'false';
			input_parameters[MIN_CAP] = 'false';
			break;
	}
}

/**
 * Get all input parameters, triggered by the submit button "Search now"
 *
 */
function getInputParams() {
	let parameters = {};
	let text_inputs = document.getElementsByClassName('text_gfa');
	let onto_use = {};
	let onto_programme = {};

	for (let i = 0; i < text_inputs.length; i++) {
		let text_item = text_inputs.item(i).firstChild;
		let sibling = text_inputs.item(i).nextElementSibling.firstChild;
		let checkbox = document.getElementById(text_inputs.item(i).firstChild.textContent);
		if (checkbox !== null) {
			var text_item_onto_class = text_item.textContent.replaceAll(" ", "");
			if (checkbox.parentElement.className === USE_PREDICATE) {
				onto_use[text_item_onto_class] = sibling.value;
			} else {
				onto_programme[text_item_onto_class] = sibling.value;
			}
		} else {
			parameters[TOTAL_GFA_KEY] = sibling.value;
		}
	}
	if (Object.keys(onto_use).length > 0) {
		parameters[USE_PREDICATE] = onto_use;
	}
	if (Object.keys(onto_programme).length > 0) {
		parameters[PROGRAMME_PREDICATE] = onto_programme;
	}
	input_parameters = parameters;
	if ((Object.keys(onto_use).length !== 0) && (Object.keys(onto_programme).length !== 0)) {
		throwNotification();
	}
	addFiltering(input_parameters);
	console.log(input_parameters);
	getValidPlots();
	resetAllInputs();
}

/**
 * Get all input parameters, triggered by the submit button "Search now"
 *
 */
function getValidPlots(){
	const iri = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/";
	$.ajax({
		url: "http://localhost:8080/agents/cityobjectinformation",
		type: 'POST',
		data: JSON.stringify({'iris': [iri], 'context': {"http://www.theworldavatar.com:83/access-agent/access": input_parameters}}),
		dataType: 'json',
		contentType: 'application/json',
		success: function (data) { //function (data, status_message, xhr)
			console.log(data["http://www.theworldavatar.com:83/access-agent/access"]["filtered"]);
			console.log(data["http://www.theworldavatar.com:83/access-agent/access"]["filteredCounts"]);
			processCIAResult(data);
		}
	});
}


/**
 * Throw error notification if the inputs or input combinations are wrong
 *
 */
function throwNotification() {
	let popup = document.createElement('div');
	popup.className = 'pop_up_box';

	let mark = document.createElement('div');
	mark.className = 'mark';
	mark.innerHTML = "!";

	let explanation =  document.createElement('div');
	explanation.className = 'alert';
	explanation.innerText = "Some of the inputs are wrong. Suggestions how to fix: "
			+ "\n 1) choose uses or programmes but not both.\n "
			+ "2) use only digits for GFA inputs. \n";

	popup.appendChild(mark);
	popup.appendChild(explanation);
	document.body.appendChild(popup);
	setTimeout(function() {
		$('.pop_up_box').hide() }, 6000);
}

/**
 * Define the UI behavior based on the selected development type
 *
 */
function showChooseDevType(){
	let developmentType = document.getElementById("DevelopmentType");
	resetAllInputs();

	switch (developmentType.value){
		case "OntozoningUses":
			document.getElementById("UsesBox").style.display="block";
			document.getElementById("ProgrammesBox").style.display="None";
			document.getElementById("GfaBox").style.display="None";
			document.getElementById('CapBox').style.display='None';
			break;
		case "OntozoingProgrammes":
			document.getElementById("UsesBox").style.display="None";
			document.getElementById("ProgrammesBox").style.display="block";
			document.getElementById("GfaBox").style.display="None";
			document.getElementById('CapBox').style.display='None';
			break;
		case "GfaUses":
			document.getElementById("UsesBox").style.display="block";
			document.getElementById("ProgrammesBox").style.display="None";
			document.getElementById("GfaBox").style.display="block";
			document.getElementById('CapBox').style.display='block';
			break;
		case "GfaProgrammes":
			document.getElementById("UsesBox").style.display="None";
			document.getElementById("ProgrammesBox").style.display="block";
			document.getElementById("GfaBox").style.display="block";
			document.getElementById('CapBox').style.display='block';
			break;
		case "ViewAll":
			document.getElementById("UsesBox").style.display="block";
			document.getElementById("ProgrammesBox").style.display="block";
			document.getElementById("GfaBox").style.display="block";
			document.getElementById('CapBox').style.display='block';
			break;
	}
	document.getElementById('searchButton').style.display='block';
}

/**
 * Sidebar for the demonstrator selection
 *
 */
function chooseDemoType(obj) {
	// reset all nav links color
	let navLinks = document.getElementsByClassName("navLink");
	for (let i = 0; i < navLinks.length; i++){
		navLinks[i].style.color = "#818181";
	}

	let selectedDemo = obj.id;
	obj.style.color = "white";
	document.getElementById("demoTitle").innerText = obj.innerText;

	switch (selectedDemo) {
		case "PPF":
			document.getElementById("demo").style.display = "inline-block";
			break;
		case "demo2":
			document.getElementById("demo").style.display = "None";
			break;
		case "demo3":
			document.getElementById("demo").style.display = "None";
			break;
	}
}


/**
 * Reset all input selection
 * Note: The querySentence should be preserved and visible after the submission.
 * Example query will not affect the checkboxes but the checkboxes will affect the querySentence
 */
function resetAllInputs(){

	// Save a copy of the last selected querySentence before cleaning the checkboxes
	let lastQuerySentence = document.getElementsByClassName('querySentence')[0].innerHTML;

	// Clear all the existing checkboxes
	let demoToolbox = document.getElementById("demo");
	//var checkboxes = toolbox.getElementsByClassName('checkbox');
	let checkedCheckboxes = demoToolbox.querySelectorAll('input[type="checkbox"]:checked');
	checkedCheckboxes.forEach((checkbox) => {
		checkbox.checked = false;
		updateGfaRows();
	});

	let totalGfaInput = document.getElementById("totalGFA");
	totalGfaInput.value = '';

	let capInput = document.getElementById('CapType');
	capInput.value = 'no_cap';

	document.getElementById('choose_programmes').style.display="None";
	document.getElementById('choose_uses').style.display="None";
	document.getElementById('assignGFA').style.display="None";

	// Keep the lastQuerySentence till new inputs are used
	document.getElementsByClassName('querySentence')[0].innerHTML = lastQuerySentence;
}