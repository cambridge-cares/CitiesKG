

/**
 * Forward the post request with json object to server side and handle the response*/
///url: "http://www.theworldavatar.com/agents/cityobjectinformation",
function SendPostRequestToServer (relativeUrl, jsonObject, HandleResponse) {
    jQuery.ajax({
        url: relativeUrl,
        type: 'POST',
        data: JSON.stringify(jsonObject),
        dataType: 'json',
        contentType: 'application/json',
        success: function (data) {
            console.log("success: ", data);
            HandleResponse(data);
        },
        error: function(XMLHttpRequest, textStatus, errorThrown) {
            alert("Status: " + textStatus); alert("Error: " + errorThrown);
        }
    });
}