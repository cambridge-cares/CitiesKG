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

        if (jQuery.isArray(response) && response.length > 0) {
            if (jQuery.isArray(response[0]) && response[0].length > 0) {
                if (jQuery.isArray(response[0][0]) && response[0][0].length > 0) {
                    var data = response[0][0][0];
                    for (var key in data) {
                        if (key == null || key == 'context') {
                            continue;
                        }
                        else if (key === "genericAttributeIris" || key === "externalReferencesIris"){
                            continue;
                        }
                        else if (key === "genericAttributes"){
                            this.genAttrKeysManager(data[key],result);
                        }
                        else if (key === "externalReferences"){
                            this.extRefKeysManager(data[key], result);
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

    KMLDataSource.prototype.queryUsingId = function (id, callback, limit, clickedObject) {
        console.log(clickedObject);

        // REQUEST FOR CityInformationAgent.
        var iri = clickedObject._iriPrefix + clickedObject._name;
        iri = iri.endsWith('/') ? iri : iri + '/';

        jQuery.ajax({
            url: "http://localhost:8080/agents/cityobjectinformation",
            type: 'POST',
            data: JSON.stringify({iris: [iri]}),
            dataType: 'json',
            contentType: 'application/json',
            success: function (data, status_message, xhr) {
                console.log(data["cityobjectinformation"]);
                callback(data["cityobjectinformation"]);
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
