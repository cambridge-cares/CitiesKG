"""
create_cea_workflow replaces generic path names in workflow file with actual file paths of cea project
"""

import argparse
import os
import yaml


def write_workflow_file(workflow_file, filepath):
    """
    :param workflow_file: input workflow file to be modified

    :param filepath: file path of the cea project
    """
    a = "directory_path"
    b = "scenario_path"
    c = "filepath_zone"
    d = "filepath_typology"

    z = filepath
    y = filepath+os.sep+"testProject"+os.sep+"testScenario"
    x = filepath+os.sep+"zone.shp"
    w = filepath+os.sep+"typology.dbf"

    find_and_replace = {a: z, b: y, c: x, d: w}

    with open(workflow_file) as stream:
        data = yaml.safe_load(stream)

    for i in data:
        for j in i:
            if j == "parameters":
                for key in i[j]:
                    if isinstance(i[j][key], str):
                        i[j][key] = find_and_replace.get(i[j][key], i[j][key])

    with open(output_yaml, 'wb') as stream:
        yaml.safe_dump(data, stream, default_flow_style=False,
                       explicit_start=False, allow_unicode=True, encoding='utf-8')


def main(argv):

    try:
        write_workflow_file(argv.workflow_file, argv.cea_file_path)
    except IOError:
        print('Error while processing file: ' + argv.worflow_file)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()

    # add arguments to the parser
    parser.add_argument("workflow_file")
    parser.add_argument("cea_file_path")

    # parse the arguments
    args = parser.parse_args()
    main(args)
