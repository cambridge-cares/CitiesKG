#! /bin/bash

docker compose up -d --no-build
# Step 1: Initalize Access Agent by execute the upload the routing file in access_agent_setup directory
# Make sure that the routing information is correct in routing.json
# more information can be found in : https://github.com/cambridge-cares/TheWorldAvatar/tree/main/Agents/AccessAgent
#echo $?

#wait
sleep 5
cd ./access_agent_setup/
bash uploadRouting.sh

# Step 2: Download the singaporeEPSG4326 dataset from URL: https://drive.google.com/file/d/169-tc6nKTetAynpA36stfD6epmBFFF2_/view?usp=sharing
cd ..
#fileid="169-tc6nKTetAynpA36stfD6epmBFFF2_"
#filename="singaporeEPSG4326.zip"
#html=`curl -c ./cookie -s -L "https://drive.google.com/uc?export=download&id=${fileid}"`
#curl -Lb ./cookie "https://drive.google.com/uc?export=download&`echo ${html}|grep -Po '(confirm=[a-zA-Z0-9\-_]+)'`&id=${fileid}" -o ${filename}

#echo "Unzip the file and copy to 3dwebclient"
#tar -xvf singaporeEPSG4326.tar.gz -C ./3dwebclient

# Step 3: start the batch for blazegraph
#cd D:\blazegraphDB
#`cmd`
#Start_Blazegraph_with_default_settings.bat

