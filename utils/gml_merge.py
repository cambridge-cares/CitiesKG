#GML files come with a header and a footer. To merge them, we need to remove all the headers and footers from all but two files (the first and the last files, which need to keep the header and footer respectively).
#For the GML files merged by the script below, the headers are 15 lines and the footers are six lines. If this is not the case for your files, you will have to amend the numbers.


import os, re
directory = os.listdir('C:/USERS/LLOW01/Desktop/Python test folder') #Amend this line and the one below to your local folder with all the GML files to be merged
os.chdir('C:/USERS/LLOW01/Desktop/Python test folder')
with open('output_file.gml', 'w') as outfile: #The final generated file name is 'output_file.gml'
    with open(directory[0],'r+') as first_file: #Select the first file only, the one for which we only delete the footer but not the header
        lines = first_file.readlines()
        first_file.seek(0)
        first_file.truncate()
        first_file.writelines(lines[:-6]) #Remove last six lines
    with open(directory[-1],'r+') as last_file: #Select the last file only, the one for which we only delete the header but not the footer
        lines = last_file.readlines()
        last_file.seek(0)
        last_file.truncate()
        last_file.writelines(lines[15:]) #Remove first fifteen lines
    for file in directory[1:-1]: #This excludes the first and last files in directory
        with open(file,'r+') as open_file:
            lines = open_file.readlines()
            open_file.seek(0)
            open_file.truncate()
            open_file.writelines(lines[15:-6]) #Remove the first 15 lines and the last six lines of each GML file
    for file in directory: #Write everything to output_file.gml
        with open(file,'r+') as infile:
            outfile.write(infile.read())