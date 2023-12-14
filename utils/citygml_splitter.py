import argparse


class CityGMLProcessor(object):
    """
    A class to process CityGML files.

    ...

    Attributes
    ----------
    model_start : str
        CityModel opening XML tag
    obj_cl_tag : str
        cityObjectMember closing XML tag
    model_cl_tag : str
        CityModel closing XML tag
    part_prefix : str
        prefix for file parts
    part_ext : str
       CityGML file extension

    Methods
    -------
    split(argv):
        Split CityGML files into chunks with desired number of cityObjectMember objects
    """

    def __init__(self):
        self.model_start = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n' \
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
        self.obj_cl_tag = '</cityObjectMember>'
        self.model_cl_tag = '</CityModel>\n'
        self.part_prefix = 'file_part_'
        self.part_ext = '.gml'

    def split(self, argv):
        """Split CityGML files into chunks

        Input arguments:
        filename -- name of the CityGML file to split
        num_objects -- number of cityObjectMember objects in each part
        """
        count = 0
        file_count = 1
        threshold = int(argv.num_objects)
        with open(argv.filename) as f:
            current_file = ""

            # loop through file
            for line in f:
                current_file = current_file + line
                if self.obj_cl_tag in line:
                    # increment counter for each cityObjectMember tag found
                    count = count + 1

                if count == threshold:
                    current_file = current_file + self.model_cl_tag
                    # write new file when the target number of cityObjectMember objects is reached
                    with open(self.part_prefix + str(file_count) + self.part_ext, 'w') as split:
                        split.write(current_file)
                    file_count = file_count + 1
                    current_file = self.model_start
                    count = 0

        # write file with remaining cityObjectMember objects
        if count > 0:
            with open(self.part_prefix + str(file_count) + self.part_ext, 'w') as split:
                split.write(current_file)


def main(argv):
    try:
        processor = CityGMLProcessor()
        processor.split(argv)
    except IOError:
        print('Error while processing file: ' + argv.filename)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    # add arguments to the parser
    parser.add_argument("filename")
    parser.add_argument("num_objects")

    # parse the arguments
    args = parser.parse_args()
    main(args)
