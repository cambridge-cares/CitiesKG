var CONTEXT = "citieskg";
var CITY = (new URL(window.location.href).searchParams.get('city'));
var TOTAL_GFA_KEY = 'TotalGFA';
var MAX_CAP = 'max_cap';
var MIN_CAP = 'min_cap';
var input_parameters;
var selectedDevType;
var click_counter = 0;

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
		final_sentence = "Find me plots that could allow...";
	}
	return final_sentence;
}

function getQuerySentence(){
	var sentence = "Find me plots that could allow...";
	var final_sentence = 'Find me plots that could allow...';
	var sentence_ormore = "Find me plots that could allow ";
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
		final_sentence = final_sentence.slice(0,8) + "the 10 smallest plots (by GFA) " + final_sentence.slice(14);
	}
	if (document.getElementById('CapType').value == 'min_cap') {
		final_sentence = final_sentence.slice(0,8) + "the 10 smallest plots (by GFA) " + final_sentence.slice(14);
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

	if(click_counter === 3) {
		click_counter = 0;
	}
	var query_example = [
			{TotalGFA:'', allowsProgramme: {Clinic: '', Flat: ''}, min_cap: 'false', max_cap: 'false'},
		{TotalGFA:'', allowsProgramme: {Clinic: '300', Flat: '2000'}, min_cap: 'false', max_cap: 'false'},
		{TotalGFA:'10000', allowsProgramme: {Library: ''}, min_cap: 'false', max_cap: 'true'}];
	switch (click_counter){
		case 0:
			input_parameters = query_example[0];
			console.log(query_example[0]);
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find me plots that could allow " + "<b>" + "Clinic" + "</b>" + " and " + "<b>" + "Flat" + "</b>" + ".";
			break;
		case 1:
			input_parameters = query_example[1];
			console.log(query_example[1]);
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find me plots that could allow " + "300 sqm of "+ "<b>" + "Clinic" + "</b>" + " (or more) and  2000 sqm " + "<b>" + "Flat" + "</b>" + " (or more).";
			break;
		case 2:
			input_parameters = query_example[2];
			console.log(query_example[2]);
			document.getElementsByClassName('querySentence')[0].innerHTML =
					"Find me the 10 smallest plots (by GFA) that could allow " + "10000 sqm development containing "+ "<b>" + "Library" + "</b>" + ".";
			break;
	}
	click_counter += 1;
	console.log(click_counter);
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
			//showResultWindow(selectedDevType, data);
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
	explanation.innerHTML = "Choose uses or programmes but not both! In the future, programmes will be filtered based on chosen uses.";

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
	selectedDevType = developmentType.value;
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
}

