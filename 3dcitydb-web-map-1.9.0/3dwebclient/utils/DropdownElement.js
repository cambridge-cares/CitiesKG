function buildOntoQuery(object){
	query = "PREFIX zo: <http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#> SELECT DISTINCT ?s WHERE { ?s rdfs:subClassOf zo:" + object + " }"
	return query
}

function getDropdownElements(object, selection_element) {
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
				checkbox_lines.push(removePrefix(checkbox_line))				
			}
			console.log(checkbox_lines)			
		}
    });

}
function removePrefix(result){
	return result.split("#")[1]	
}