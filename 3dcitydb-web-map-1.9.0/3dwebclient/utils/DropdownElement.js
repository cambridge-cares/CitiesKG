var CONTEXT = "citieskg";
var CITY = (new URL(window.location.href).searchParams.get('city'));
var TOTAL_GFA_KEY = 'TotalGFA';
var input_parameters;


function buildQuery(predicate){
	query = "PREFIX zo:<http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#> "
			+ "SELECT DISTINCT ?g WHERE { GRAPH <http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/> "
			+ "{ ?zone zo:" + predicate + " ?g . } }"
	return query
}

function getDropdownElements(predicate, element_type, dropdown_type) {
	$.ajax({
		url:"http://www.theworldavatar.com:83/access-agent/access",
		type: 'POST',
		data: JSON.stringify({targetresourceiri:CONTEXT + "-" + CITY , sparqlquery: buildQuery(predicate)}),
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
			"<input type='checkbox' onchange='updateGfaRows()' onblur='getInputParams()'/></div>" +
			"<div class=" + element_type + ">" + line + "</div>" +
			"</div>";
	$(dropdown_type).append(some_element)
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
						"<div class='text_gfa' style='width: 180px'>" + checkboxes.item(
								i).id + "</div>" +
						"<div class='text_input'><input type='text' maxLength='5' size='3' onblur='getInputParams()'/></div>" +
						"<hr size='1'>" +
					  "</div>" ;
				$("#assignGFA").append(clicked_element)
			}
		}
	}
}

function removePrefix(result){
	var element = result.split("#")[1]
	element = element.match(/[A-Z][a-z]+|[0-9]+/g).join(" ")
	return element
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
		parameters[PROGRAMME_PREDICATE] =  onto_programme;
	}
	input_parameters = parameters;
	 if ((Object.keys(onto_use).length != 0) && (Object.keys(onto_programme).length != 0)) {
		 throwNotification();
	 }
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
			highlightMultipleObjects(data["http://www.theworldavatar.com:83/access-agent/access"]["filtered"]);

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

function showChooseDev(){

	var developmentType = document.getElementById("developmentType");
	//console.log(event.target.id + "is clicked");
	if (developmentType.value == "OntozoningUses"){
		document.getElementById("programme_dropdown").style.display="None";
	}
	//document.getElementById("favourite").value = mylist.options[mylist.selectedIndex].text;
}


