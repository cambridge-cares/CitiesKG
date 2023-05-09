var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var KMLDataSource = /** @class */ (function (_super) {
    __extends(KMLDataSource, _super);
    function KMLDataSource(signInController, options) {
        var _this = _super.call(this, signInController, options) || this;
        _this._useOwnKmlParser = false;
        return _this;
    }
    KMLDataSource.prototype.responseToKvp = function (response) {
        if (this._useOwnKmlParser) {
            return this.responseOwnToKvp(response);
        }
        if (this._thirdPartyHandler) {
            switch (this._thirdPartyHandler.type) {
                case ThirdPartyHandler.Cesium: {
                    // the handler is Cesium.KMLDataSource
                    return this.responseCesiumToKvp(response);
                    break;
                }
                default: {
                    // no valid handler found
                    return this.responseOwnToKvp(response);
                    break;
                }
            }
        }
    };

    //---Extended Web-Map-Client version---//


    KMLDataSource.prototype.responseCesiumToKvp = function (response) {
        // response is a list of JSON elements
        var result = new Map();

        var cityResponse = response["cityobjectinformation"];
        var contextResponse = response["context"] ? response[Object.keys(response["context"])[0]] : null;

        if (jQuery.isArray(cityResponse) && cityResponse.length > 0) {
            if (jQuery.isArray(cityResponse[0]) && cityResponse[0].length > 0) {
                if (jQuery.isArray(cityResponse[0][0]) && cityResponse[0][0].length > 0) {
                    var cityobjectdata = cityResponse[0][0][0];
                    for (var key in cityobjectdata) {
                        if (key == null || key == 'context') {
                            continue;
                        }
                        else if (key === "genericAttributeIris" || key === "externalReferencesIris"){
                            continue;
                        }
                        else if (key === "genericAttributes"){
                            this.genAttrKeysManager(cityobjectdata[key],result);
                        }
                        else if (key === "externalReferences"){
                            this.extRefKeysManager(cityobjectdata[key], result);
                        }
                        else if (key == 'updatingPerson'){
                            continue;
                        }
                        else {
                            result[key] = cityobjectdata[key];
                        }
                    }
                }
            }
        }

        if (jQuery.isArray(contextResponse) && contextResponse.length > 0) {
            if (jQuery.isArray(contextResponse[0]['ceaOutputs']) && contextResponse[0]['ceaOutputs'].length > 0) {
                var energydata = contextResponse[0]['ceaOutputs'][0];
                for (var key in energydata) {
                    if (key == null) {
                        continue;
                    } else {
                        result[key] = energydata[key];
                    }
                }
            }
            else if (jQuery.isArray(contextResponse[0]['chemplant']) && contextResponse[0]['chemplant'].length > 0){
                if (contextResponse[0]['chemplant'][0].length > 0){
                    var data = contextResponse[0]['chemplant'][0][0];
                }
                else {
                    var data = contextResponse[0]['chemplant'][0];
                }
                for (var key in data) {
                    if (key == null || key == 'context'){
                        continue;
                    }
                    for (var i in data[key]) {
                        if (jQuery.isArray(data[key]) && data[key].length > 0) {
                            this.additionalDataKeysManager(data[key], result);
                        }
                        else {
                            result[key] = data[key];
                        }
                    }
                }
            }
        }

        return result;
    };

    KMLDataSource.prototype.genAttrKeysManager = function (data, result) {
        for (var index in data) {
            var object = data[index];
            var name = object["attrName"];
            var value;
            var data_type = object["dataType"];
            switch (data_type) {
                case 2:
                    value = object["intVal"];
                    break;
                case 3:
                    value = object["realVal"];
                    break;
                case 4:
                    value = object["uriVal"];
                    break;
                default:
                    value = object["strVal"];
            }
            result[name] = value;
        }

    };

    KMLDataSource.prototype.extRefKeysManager = function (data, result) {
        for (var index in data){
            var object = data[index];
            var name = object["infoSys"];
            var value = object["URI"];
            result[name] = value;
        }
    };

    KMLDataSource.prototype.additionalDataKeysManager = function (data, result) {
        for (var index in data){
            for (var key in data[0]) {
                if (jQuery.isArray(data[0][key]) && data[0][key].length > 0){
                    var innerdata = data[0][key];
                    for (var name in innerdata[0]){
                        if (name == null || name == 'iri' || name == 'context') {
                            continue;
                        }
                        var value = innerdata[0][name];
                        if (jQuery.isArray(value) && value.length == 0){
                            continue;
                        }
                        if (value != null){
                            result[name] = value;
                        }
                    }
                }
                else if (key == null ||  key == 'iri' || key == 'context' || data[0][key].length == 0) {
                    continue;
                }
                else {
                    var value = data[0][key];
                    var name = key;
                    result[name] = value;
                }
            }
        }
    };

    KMLDataSource.prototype.contextManager = function(context) {
        switch (context) {
            case 'energy':
                //return 'http://localhost:58085/agents/cea/query';
                return 'http://theworldavatar.com:83/agents/cea/query';
                break;
            case 'chemplant':
                return 'http://localhost:1083/ontochemplant-agent/query';
                break;
            default:
                return '';
                break;
        }
    };

    KMLDataSource.prototype.queryUsingId = function (id, callback, limit, clickedObject) {
        console.log(clickedObject);

        // REQUEST FOR CityInformationAgent.
        var iri = clickedObject._iriPrefix + clickedObject._name;
        iri = iri.endsWith('/') ? iri : iri + '/';
        iri = iri.replace(new RegExp('_(\\w*Surface)'), '');

        var context_url = this.contextManager(clickedObject._cia_context);
        var context_obj = {};
        context_obj[context_url] = {};

        var cia_data = context_url ? {iris: [iri], context: context_obj} : {iris: [iri]};

        jQuery.ajax({
            url: "http://localhost:8080/agents/cityobjectinformation",
            //url: "http://www.theworldavatar.com/agents/cityobjectinformation",
            type: 'POST',
            data: JSON.stringify(cia_data),
            dataType: 'json',
            contentType: 'application/json',
            success: function (data, status_message, xhr) {
                console.log(data);
                callback(data);
            }});
    };


    //---Extended Web-Map-Client version---//


    KMLDataSource.prototype.responseOwnToKvp = function (response) {
        // response is a list of XML DOM element
        var result = new Map();
        /* read extended data, only works for the following structure
        <ExtendedData>
            <SchemaData schemaUrl="#some_schema">
                <SimpleData name="A">Text</SimpleData>
                <SimpleData name="B">Text</SimpleData>
            </SchemaData>
        </ExtendedData>
        TODO more general implementation?
         */
        for (var i = 0; i < response.length; i++) {
            var simpleData = response[i];
            result[simpleData.getAttribute('name')] = simpleData.textContent;
        }
        return result;
    };

    KMLDataSource.prototype.countFromResult = function (res) {
        return res.getSize();
    };
    KMLDataSource.prototype.deleteDataRecordUsingId = function (id) {
        // TODO
        return null;
    };
    KMLDataSource.prototype.fetchIdsFromResult = function (res) {
        // TODO
        return null;
    };
    KMLDataSource.prototype.insertDataRecord = function (record) {
        // TODO
        return null;
    };
    KMLDataSource.prototype.queryUsingIds = function (ids) {
        // TODO
        return null;
    };
    KMLDataSource.prototype.queryUsingNames = function (names, limit) {
        // TODO
        return null;
    };

    KMLDataSource.prototype.queryUsingIdCustom = function (id, callback, limit, clickedObject) {
        this._useOwnKmlParser = true;
        // read KML file
        var xhttp = new XMLHttpRequest();
        xhttp.onreadystatechange = function () {
            if (this.readyState == 4 && this.status == 200) {
                var xmlParser = new DOMParser();
                var xmlDoc = xmlParser.parseFromString(xhttp.responseText, "text/xml");
                var placemark = xmlDoc.getElementById(id);
                if (placemark == null) {
                    var placemarkNameSearch = clickedObject._name;
                    // the placemarks in the KML file probably do not have IDs
                    // search for its name values instead
                    var placemarks = xmlDoc.getElementsByTagName("Placemark");
                    for (var i = 0; i < placemarks.length; i++) {
                        var iPlacemark = placemarks[i];
                        var placemarkName = iPlacemark.getElementsByTagName("name")[0];
                        if (placemarkName != null && placemarkName.textContent === placemarkNameSearch) {
                            placemark = iPlacemark;
                            break;
                        }
                    }
                }
                var extendedData = placemark.getElementsByTagName('ExtendedData')[0];
                var schemaData = extendedData.getElementsByTagName('SchemaData')[0];
                var simpleDataList = schemaData.getElementsByTagName('SimpleData');
                // return XML DOM element
                callback(simpleDataList);
            }
        };
        // TODO enable proxy for other Data Sources?
        xhttp.open("GET", (this._uri.indexOf(this._proxyPrefix) >= 0 ? "" : this._proxyPrefix) + this._uri, true);
        xhttp.send();
    };
    KMLDataSource.prototype.queryUsingSql = function (sql, callback, limit, clickedObject) {
        // TODO
        return;
    };
    KMLDataSource.prototype.queryUsingTypes = function (types, limit) {
        // TODO
        return null;
    };
    KMLDataSource.prototype.sumFromResultByColIndex = function (res, colIndex) {
        // TODO
        return null;
    };
    KMLDataSource.prototype.sumFromResultByName = function (res, name) {
        // TODO
        return null;
    };
    KMLDataSource.prototype.updateDataRecordUsingId = function (id, newRecord) {
        // TODO
        return null;
    };
    Object.defineProperty(KMLDataSource.prototype, "useOwnKmlParser", {
        get: function () {
            return this._useOwnKmlParser;
        },
        set: function (value) {
            this._useOwnKmlParser = value;
        },
        enumerable: true,
        configurable: true
    });
    return KMLDataSource;
}(XMLDataSource));
