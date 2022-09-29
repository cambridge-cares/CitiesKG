var CONTEXT = "citieskg";
var CITY = (new URL(window.location.href).searchParams.get('city'));
var TOTAL_GFA_KEY = 'TotalGFA';
var MAX_CAP = 'max_cap';
var MIN_CAP = 'min_cap';
var input_parameters;
var selectedDevType;
var click_counter = 0;
var area_counter = 0;

function buildQuery(predicate){
	query = "PREFIX zo:<http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#> "
			+ "SELECT DISTINCT ?g WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/> "
			+ "{ ?zone zo:" + predicate + " ?g . } }"
	return query
}

function getDropdownElements(predicate, element_type, dropdown_type) {
	$.ajax({
		url:"http://www.theworldavatar.com:83/access-agent/access",
		//url:"http://localhost:48888/access-agent/access",
		type: 'POST',
		data: JSON.stringify({targetresourceiri:CONTEXT + "-" + CITY , sparqlquery: buildQuery(predicate)}),
		//data: JSON.stringify({targetresourceiri:"http://localhost:48888/test" , sparqlquery: buildQuery(predicate)}),
		dataType: 'json',
		contentType: 'application/json',
		success: function(data, status_message, xhr){
			var results = JSON.parse(data["result"]);
			var checkbox_lines = [];
			for (let index in results) {
				var checkbox_line = removePrefix(results[index]["g"]);
				checkbox_lines.push(checkbox_line);
			}
			checkbox_lines.sort()
			for (let index in checkbox_lines) {
				appendElement(checkbox_lines[index], predicate, element_type, dropdown_type)
			}
		}
	});
}

function appendElement(line, predicate, element_type, dropdown_type){
	var some_element =
			"<div class='" + predicate + "'>" +
			"<div id='" + line + "' class='checkbox'>" +
			"<input type='checkbox' onchange='updateGfaRows()'></div>" +
			"<div class=" + element_type + ">" + line + "</div>" +
			"</div>";
	$(dropdown_type).append(some_element)
}

function processQuerySentence(programme_bool, use_bool, programmeGFA_bool, useGFA_bool, sqm, sentence, programmes, uses, final_sentence) {
	if(programme_bool) {
		for (const [programme, programmeGFAvalue] of Object.entries(programmes)) {
			if (programmeGFA_bool) {
				sentence += programmeGFAvalue + sqm + " of " + "<b>" + programme + "</b>" + " (or more) and ";
			}
			else {
				sentence +=  "<b>" + programme + "</b>" + " and ";
			}
		}
		final_sentence = sentence.slice(0, -5) + ".";

	}
	else if (use_bool) {
		for (const [use, useGFAvalue] of Object.entries(uses)) {
			if (useGFA_bool) {
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

function getQuerySentence(){
	var sentence = "Find plots that could allow...";
	var final_sentence = 'Find plots that could allow...';
	var sentence_ormore = "Find plots that could allow ";
	var sqm = " sqm";
	document.getElementsByClassName('querySentence')[0].textContent = final_sentence;

	var inputs = document.getElementsByClassName('text_gfa');
	var uses = {};
	var programmes = {};
	var totalGFA_input;
	var programmeGFA_bool = false;
	var useGFA_bool = false;

	for (let i = 0; i < inputs.length; i++) {
		var inputs_item = inputs.item(i).firstChild;
		var sibling = inputs.item(i).nextElementSibling.firstChild;
		var checkbox = document.getElementById(inputs.item(i).firstChild.textContent);
		if (checkbox !== null) {
			var inputs_item_onto_class = inputs_item.textContent;
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
	var programme_bool = Object.keys(programmes).length > 0;
	var use_bool = Object.keys(uses).length > 0;

	if (!(totalGFA_input === '')) {
		if (programme_bool || use_bool) {
			if(programmeGFA_bool || useGFA_bool) {
				sentence = sentence_ormore + totalGFA_input + sqm + " development, combining ";
				final_sentence = processQuerySentence(programme_bool, use_bool, programmeGFA_bool, useGFA_bool, sqm, sentence, programmes, uses, final_sentence);
			}
			else {
				sentence = sentence_ormore + totalGFA_input + sqm + " development, combining ";
				final_sentence = processQuerySentence(programme_bool, use_bool, programmeGFA_bool, useGFA_bool, sqm, sentence, programmes, uses, final_sentence);
			}
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

	if (document.getElementById('CapType').value == 'max_cap') {
		final_sentence = final_sentence.slice(0,5) + "the 10 largest plots (by GFA) " + final_sentence.slice(11);
	}
	if (document.getElementById('CapType').value == 'min_cap') {
		final_sentence = final_sentence.slice(0,5) + "the 10 smallest plots (by GFA) " + final_sentence.slice(11);
	}
	document.getElementsByClassName('querySentence')[0].innerHTML = final_sentence;
}

function updateGfaRows(){
	var parent = document.getElementById("assignGFA");
	var children = Array.from(parent.children);
	for (var i = 0; i < children.length; i++) {
		var child = children[i];
		if (child.className === 'gfa') {
			var checkbox_id = child.firstChild.textContent;
			var checkbox = document.getElementById(checkbox_id).firstChild;
			var text_input = child.childNodes[1].firstChild;
			if (!checkbox.checked || (text_input.value === "")) {
				parent.removeChild(child);
			}
		}
	}
	var checkboxes = document.getElementsByClassName('checkbox')
	var text_inputs = Array.from(document.getElementsByClassName('text_gfa'));
	for (let i = 0; i < checkboxes.length; i++) {
		checkbox = checkboxes.item(i).firstChild;
		if (checkbox.checked) {
			checkbox_id = checkboxes.item(i).id.toString();
			var exists = false;
			for (let i = 0; i < text_inputs.length; i++) {
				var checkbox_label = text_inputs[i].firstChild.textContent.toString();
				if (checkbox_label === checkbox_id) {
					exists = true;
					break;
				}
			}
			if (!exists) {
				var clicked_element =
						"<div class='gfa'>" +
						"<div class='text_gfa' style='width: 180px'>" + checkboxes.item(i).id + "</div>" +
						"<div class='text_input'><input type='text' maxLength='5' size='3'></div>" +
						"<hr size='1'>" +
					  "</div>" ;
				$("#assignGFA").append(clicked_element)

			}
		}
	}
	getQuerySentence();
}

function removePrefix(result){
	var element = result.split("#")[1]
	element = element.match(/[A-Z][a-z]+|[0-9]+/g).join(" ")
	return element
}

function getExampleParams() {
	resetAllInputs();
	if(click_counter === 6) {
		click_counter = 0;
	}
	var query_example = [{TotalGFA:'', allowsUse: {ParkUse: ''}, min_cap: 'false', max_cap: 'false'},
		{TotalGFA:'', allowsProgramme: {Flat: '', Clinic:''}, min_cap: 'false', max_cap: 'false'},
		{TotalGFA:'', allowsProgramme: {PrintingPress: '', Gym:'', FoodLaboratory:''}, min_cap: 'false', max_cap: 'false'},
		{TotalGFA:'', allowsProgramme: {Condominium: ''}, min_cap: 'false', max_cap: 'true'},
		{TotalGFA:'', allowsProgramme: {Clinic: '100', Flat:'2000', Mall:'1000'}, min_cap: 'true', max_cap: 'false'},
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
					"Find plots that could allow " + "3000 sqm of "+ "<b>" + "Flat" + "</b>" + " (or more) and  100 sqm of " + "<b>" + "GroceryStore" + "</b>" + " (or more).";
			break;
		case 4:
			input_parameters = query_example[4];
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find plots that could allow " + "100 sqm of "+ "<b>" + "Clinic" + "</b>" + " (or more) and  2000 sqm of " + "<b>" + "Flat" + "</b>" + " (or more) and  1000 sqm of " + "<b>" + "Mall" + "</b>" + " (or more).";
			break;
		case 5:
			input_parameters = query_example[5];
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find the 10 smallest plots (by GFA) that could allow " + "50000 sqm of "+ "<b>" + "Bank" + "</b>" + " (or more) and 50000 sqm of " + "<b>" + "Bar" + "</b>" + " (or more) and  50000 sqm of " + "<b>" + "BookStore" + "</b>" + " (or more).";

			break;
	}
	click_counter += 1;
	console.log(input_parameters);
	getValidPlots();
}

function addFiltering(input_parameters){
	var  filteringType = document.getElementById("CapType").value;
	console.log(filteringType);
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

function getInputParams() {
	var parameters = {};
	var text_inputs = document.getElementsByClassName('text_gfa');
	var onto_use = {};
	var onto_programme = {};

	for (let i = 0; i < text_inputs.length; i++) {
		var text_item = text_inputs.item(i).firstChild;
		var sibling = text_inputs.item(i).nextElementSibling.firstChild;
		var checkbox = document.getElementById(text_inputs.item(i).firstChild.textContent);
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
}

function getValidPlots(){
	var iri = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/";
	$.ajax({
		url: "http://localhost:8080/agents/cityobjectinformation",
		type: 'POST',
		data: JSON.stringify({'iris': [iri], 'context': {"http://www.theworldavatar.com:83/access-agent/access": input_parameters}}),
		dataType: 'json',
		contentType: 'application/json',
		success: function (data, status_message, xhr) {
			console.log(data["http://www.theworldavatar.com:83/access-agent/access"]["filtered"]);
			console.log(data["http://www.theworldavatar.com:83/access-agent/access"]["filteredCounts"]);
			showResultWindow(data);
			processFilteredObjects(data["http://www.theworldavatar.com:83/access-agent/access"]["filtered"]);
      		//pinHighlightObjects(data["http://www.theworldavatar.com:83/access-agent/access"]["filtered"]);

		}
	});
	//highlightMultipleObjects();
}

function throwNotification() {
	var popup = document.createElement('div');
	popup.className = 'pop_up_box';

	var mark = document.createElement('div');
	mark.className = 'mark';
	mark.innerHTML = "!";

	var explanation =  document.createElement('div');
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

function showChooseDevType(){
	var developmentType = document.getElementById("DevelopmentType");
	// Shiying: Add a cleaning function, clean the gfa dropdown and unselect everything
	resetAllInputs();
	//console.log(event.target.id + "is clicked");

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
			pinHighlightObjects();
	}
}

function chooseDemoType() {
	var demo_type =  document.getElementById("demo_type")
	switch (demo_type.value) {
		case "PPF":
			document.getElementById("demo").style.display = "inline-block";
			console.log(demo_type);
			break;
		case "demo2":
			document.getElementById("demo").style.display = "None";
			console.log(demo_type);
			break;
		case "demo3":
			document.getElementById("demo").style.display = "None";
			console.log(demo_type);
			break;
	}
}

function resetAllInputs(){
	var checkboxes = document.getElementsByClassName('checkbox');
	let checkedCheckboxes = document.querySelectorAll('input[type="checkbox"]:checked');
	checkedCheckboxes.forEach((checkbox) => {
		checkbox.checked = false;
		updateGfaRows();
	});

	var totalGfaInput = document.getElementById("totalGFA");
	//totalGfaInput.innerHTML = "";
	totalGfaInput.value = '';

	var capInput = document.getElementById('CapType');
	capInput.value = 'no_cap';

	var query_sentence = document.getElementById('querySentence');
	query_sentence.innerText = 'Find plots that could allow...';

	document.getElementById('choose_programmes').style.display="None";
	document.getElementById('choose_uses').style.display="None";
	document.getElementById('assignGFA').style.display="None";
}

function addDisclaimerButton(){
	var toolbar = document.getElementsByClassName('cesium-viewer-toolbar');  // HTMLCollection; has to use item(0) to add an additional icon

	var qMarkSymbol = document.createElementNS("http://www.w3.org/2000/svg", "svg");
	qMarkSymbol.setAttribute("class", "cesium-svgPath-svg");
	qMarkSymbol.setAttribute("width", "32");
	qMarkSymbol.setAttribute("height", "32");
	qMarkSymbol.setAttribute("viewBox", "0 0 32 32");
	qMarkSymbol.setAttribute("style", "none");
	qMarkSymbol.innerHTML = "<path d=" + "M16,1.466C7.973,1.466,1.466,7.973,1.466,16c0,8.027,6.507,14.534,14.534,14.534c8.027,0,14.534-6.507,14.534-14.534C30.534,7.973,24.027,1.466,16,1.466z" + "M17.328,24.371h-2.707v-2.596h2.707V24.371zM17.328,19.003v0.858h-2.707v-1.057c0-3.19,3.63-3.696,3.63-5.963c0-1.034-0.924-1.826-2.134-1.826c-1.254,0-2.354,0.924-2.354,0.924l-1.541-1.915c0,0,1.519-1.584,4.137-1.584c2.487,0,4.796,1.54,4.796,4.136C21.156,16.208,17.328,16.627,17.328,19.003z>";

	var disclaimerButton = document.createElement('button');
	disclaimerButton.type = 'button';
	disclaimerButton.className = 'cesium-button cesium-toolbar-button cesium-navigation-help-button';
	disclaimerButton.setAttribute('data-bind', 'attr: { title: tooltip }');
	disclaimerButton.appendChild(qMarkSymbol);
	
	
	toolbar.item(0).appendChild(disclaimerButton);

	var instructionContainer = document.createElement('div');
	instructionContainer.id = "instructionContainer";
	instructionContainer.className = 'cesium-navigation-help';
	instructionContainer.setAttribute('data-bind', 'css: { "cesium-navigation-help-visible" : showInstructions}');
	toolbar.item(0).appendChild(instructionContainer);

	var mouseButton = document.createElement('button');
	mouseButton.type = 'button';
	mouseButton.className = 'cesium-navigation-button cesium-navigation-button-left';
	mouseButton.setAttribute('data-bind', 'click: showClick, css: {"cesium-navigation-button-selected": !_touch, "cesium-navigation-button-unselected": _touch}');
	mouseButton.appendChild(document.createTextNode('Mouse'));

	var touchButton = document.createElement('button');
	touchButton.type = 'button';
	touchButton.className = 'cesium-navigation-button cesium-navigation-button-right';
	touchButton.setAttribute('data-bind', 'click: showTouch, css: {"cesium-navigation-button-selected": _touch, "cesium-navigation-button-unselected": !_touch}');
	touchButton.appendChild(document.createTextNode('Touch'));

	instructionContainer.appendChild(mouseButton);
	instructionContainer.appendChild(touchButton);

}

function zoomToKeyGrowthAreas() {
	resetAllInputs();
	if(area_counter === 3) {
		area_counter = 0;
	}

	switch (area_counter){
		case 0:
			document.getElementById('keyGrowthAreas').innerText = 'Singapore River Valley';
			var cameraPostion = {
				latitude:  1.279,
				longitude: 103.84,
				height: 2800,
				heading: 360,
				pitch: -60,
				roll: 356,
			}
			flyToCameraPosition(cameraPostion);
			break;
			document.getElementById('keyGrowthAreas').innerText = 'Punggol Digital District';
			var cameraPostion = {
				latitude:  1.408600,
				longitude: 103.911385,
				height: 2800,
				heading: 360,
				pitch: -60,
				roll: 356,
			}
			flyToCameraPosition(cameraPostion);
		case 2:
			document.getElementById('keyGrowthAreas').innerText = 'Paya Lebar Military Base'
			var cameraPostion = {
				latitude:  1.279,
				longitude: 103.84,
				height: 2800,
				heading: 360,
				pitch: -60,
				roll: 356,
			}
			flyToCameraPosition(cameraPostion);
	}
	area_counter += 1;

}

