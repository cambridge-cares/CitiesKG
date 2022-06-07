import os, re
os.chdir('C:/USERS/LLOW01/Desktop/Python test folder') #Amend this line to your local folder with the GML files to be merged

try:
    os.remove('output.gml') #Delete the output file, 'output.gml', if it is in the folder. This allows the script to be run multiple times in the same directory
except OSError:
    pass

directory = os.listdir('C:/USERS/LLOW01/Desktop/Python test folder') #Amend this line to your local folder with the GML files to be merged

with open(directory[0], 'r') as first_file: #Open first file
    data = first_file.readlines()
pos2 = data.index('</CityModel>\n') #Find the </CityModel> line
with open('output.gml','w') as outfile: #Note the 'w' flag, which erases everything in output.gml. The later loops use 'a' for append
    outfile.writelines(data[:pos2]) #And write everything up to </CityModel> into output.gml

for file in directory[1:-1]: #Open every file except the first and last files
    with open(file,'r+') as open_file:
        data = open_file.readlines()
    pos = data.index('<cityObjectMember>\n') #Find the position of <cityObjectMember>
    pos2 = data.index('</CityModel>\n') #And the position of </CityModel>
    with open('output.gml','a') as outfile:
        outfile.writelines(data[pos:pos2]) #Write everything between these two positions into output.gml

with open(directory[-1], 'r') as last_file: #Open last file
    data = last_file.readlines()
pos = data.index('<cityObjectMember>\n') #Find the position of <cityObjectMember>
with open('output.gml','a') as outfile:
    outfile.writelines(data[pos:]) #Write everything after that position into output.gml