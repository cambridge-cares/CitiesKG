model_start = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n' \
              '<CityModel xmlns:xal="urn:oasis:names:tc:ciq:xsdschema:xAL:2.0" ' \
              'xmlns:gml="http://www.opengis.net/gml" ' \
              'xmlns:wtr="http://www.opengis.net/citygml/waterbody/2.0" ' \
              'xmlns:app="http://www.opengis.net/citygml/appearance/2.0" ' \
              'xmlns:tex="http://www.opengis.net/citygml/texturedsurface/2.0" ' \
              'xmlns="http://www.opengis.net/citygml/2.0" ' \
              'xmlns:veg="http://www.opengis.net/citygml/vegetation/2.0" ' \
              'xmlns:dem="http://www.opengis.net/citygml/relief/2.0" ' \
              'xmlns:tran="http://www.opengis.net/citygml/transportation/2.0" ' \
              'xmlns:bldg="http://www.opengis.net/citygml/building/2.0" ' \
              'xmlns:grp="http://www.opengis.net/citygml/cityobjectgroup/2.0" ' \
              'xmlns:tun="http://www.opengis.net/citygml/tunnel/2.0" ' \
              'xmlns:frn="http://www.opengis.net/citygml/cityfurniture/2.0" ' \
              'xmlns:brid="http://www.opengis.net/citygml/bridge/2.0" ' \
              'xmlns:gen="http://www.opengis.net/citygml/generics/2.0" ' \
              'xmlns:xlink="http://www.w3.org/1999/xlink" ' \
              'xmlns:luse="http://www.opengis.net/citygml/landuse/2.0" ' \
              'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' \
              'xsi:schemaLocation="http://www.opengis.net/citygml/waterbody/2.0 ' \
              'http://schemas.opengis.net/citygml/waterbody/2.0/waterBody.xsd ' \
              'http://www.opengis.net/citygml/appearance/2.0 ' \
              'http://schemas.opengis.net/citygml/appearance/2.0/appearance.xsd ' \
              'http://www.opengis.net/citygml/texturedsurface/2.0 ' \
              'http://schemas.opengis.net/citygml/texturedsurface/2.0/texturedSurface.xsd ' \
              'http://www.opengis.net/citygml/vegetation/2.0 ' \
              'http://schemas.opengis.net/citygml/vegetation/2.0/vegetation.xsd ' \
              'http://www.opengis.net/citygml/relief/2.0 ' \
              'http://schemas.opengis.net/citygml/relief/2.0/relief.xsd ' \
              'http://www.opengis.net/citygml/transportation/2.0 ' \
              'http://schemas.opengis.net/citygml/transportation/2.0/transportation.xsd ' \
              'http://www.opengis.net/citygml/building/2.0 ' \
              'http://schemas.opengis.net/citygml/building/2.0/building.xsd ' \
              'http://www.opengis.net/citygml/cityobjectgroup/2.0 ' \
              'http://schemas.opengis.net/citygml/cityobjectgroup/2.0/cityObjectGroup.xsd ' \
              'http://www.opengis.net/citygml/tunnel/2.0 ' \
              'http://schemas.opengis.net/citygml/tunnel/2.0/tunnel.xsd ' \
              'http://www.opengis.net/citygml/cityfurniture/2.0 ' \
              'http://schemas.opengis.net/citygml/cityfurniture/2.0/cityFurniture.xsd ' \
              'http://www.opengis.net/citygml/bridge/2.0 ' \
              'http://schemas.opengis.net/citygml/bridge/2.0/bridge.xsd ' \
              'http://www.opengis.net/citygml/generics/2.0 ' \
              'http://schemas.opengis.net/citygml/generics/2.0/generics.xsd ' \
              'http://www.opengis.net/citygml/landuse/2.0 ' \
              'http://schemas.opengis.net/citygml/landuse/2.0/landUse.xsd">\n'

obj_cl_tag = '</cityObjectMember>'
model_cl_tag = '</CityModel>'
part_prefix = 'file_'
part_ext = '.gml'

count = 0
file_count = 1
with open('citygml.gml') as f:
    current_file = ""

    for line in f:
        current_file = current_file + line
        if obj_cl_tag in line:
            count = count + 1

        if count == 2500:
            current_file = current_file + model_cl_tag
            with open(part_prefix + str(file_count) + part_ext, 'w') as split:
                split.write(current_file)
            file_count = file_count + 1
            current_file = model_start
            count = 0

with open(part_prefix + str(file_count) + part_ext, 'w') as split:
    split.write(current_file)
