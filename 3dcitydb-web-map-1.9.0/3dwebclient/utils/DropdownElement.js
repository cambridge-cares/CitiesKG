function buildOntoQuery(object){
	query = "PREFIX zo: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#> SELECT DISTINCT ?s WHERE { ?s rdfs:subClassOf zo:" + object + " }"
	return query
}

function getDropdownElements(object, element_type, dropdown_type) {
	$.ajax({
		url:"http://localhost:9999/blazegraph/namespace/kb/sparql",
		type: 'POST',
		data: buildOntoQuery(object),
		contentType: 'application/sparql-query',
		beforeSend: function(xhr) {
			xhr.setRequestHeader("Accept", "application/json");
		},
		success: function(data, status_message, xhr){
			const checkbox_lines = [];
			var results = data["results"]["bindings"];
			for (index in results){
				var checkbox_line = results[index]["s"]["value"];
				appendElement(removePrefix(checkbox_line), element_type, dropdown_type)
			}
		}
    });
}

function appendElement(line,element_type, dropdown_type){
	var some_element =
			"<div>" +
			"<div id='check' class='checkbox'><input type='checkbox' value="+ line + "></div>" +
			"<div class=" + element_type + ">" + line + "</div>" +
			"</div>";
		$(dropdown_type).append(some_element)
}

function removePrefix(result){
	var element = result.split("#")[1]
	element = element.match(/[A-Z][a-z]+|[0-9]+/g).join(" ")
	return element
}