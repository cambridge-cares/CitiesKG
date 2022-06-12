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
			for (index in results){
				var checkbox_line = results[index]["g"];
				appendElement(removePrefix(checkbox_line), predicate, element_type, dropdown_type)
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
	while (parent.childElementCount > 1) {
		parent.removeChild(parent.lastChild);
	}
	var checkboxes = document.getElementsByClassName('checkbox')
	for (let i = 0; i < checkboxes.length; i++) {
		var checkbox = checkboxes.item(i).firstChild;
		if (checkbox.checked) {
			var clicked_element =
					"<div>" +
					"<div class='text_gfa' style='width: 180px'>" + checkboxes.item(i).id + "</div>" +
					"<div class='text_input'><input type='text' maxLength='5' size='3' onblur='getInputParams()'/></div>" +
					"</div>" +
					"<hr size='1'>";
			$("#assignGFA").append(clicked_element)
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
			var text_item_onto_class = text_item.textContent.replace(" ", "");
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
	getValidPlots();
}

function getValidPlots(){
	var iri = "http://localhost:8080/agents/cityobjectinformation";
	$.ajax({
		url: "http://localhost:8080/agents/cityobjectinformation",
		type: 'POST',
		data: JSON.stringify({'iris': [iri], 'filters': input_parameters}),
		dataType: 'json',
		contentType: 'application/json',
		success: function (data, status_message, xhr) {
			console.log(data["filtered_objects"]);
		}
	});
}