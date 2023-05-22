


function createCard(){

}



function createTable(){
    let comparisonContent = document.getElementsById('comparisonContent');
    let FirstTable = document.createElement("table");
    FirstTable.style.width = "500px";
    FirstTable.className = "table table-bordered";
    FirstTable.innerHTML = '<thead><tr>\
      <th scope="col">#</th>\
      <th scope="col">First</th>\
      <th scope="col">Last</th>\
      <th scope="col">Handle</th>\
    </tr></thead>\
    <tbody><tr>\
      <th scope="row">1</th>\
      <td>Mark</td>\
      <td>Otto</td>\
      <td>@mdo</td>\
    </tr><tr>\
      <th scope="row">2</th>\
      <td>Jacob</td>\
      <td>Thornton</td>\
      <td>@fat</td>\
    </tr><tr>\
      <th scope="row">3</th>\
      <td colspan="2">Larry the Bird</td>\
      <td>@twitter</td>\
    </tr></tbody>\
    ';
    comparisonContent.appendChild(FirstTable);
}




function createIndexPanel(){
    let indexTable = document.createElement("table");
    indexTable.className = "table table-hover";
    indexTable.innerHTML = '\
    <thead><tr><th scope="col">**</th></tr></thead>\
    <tbody><tr><th scope="row">GFA</th></tr>\
    <tr><th scope="row">MixedUse</th></tr>\
    <tr><th scope="row">Solar potential</th></tr>\
    <tr><th scope="row">Number of Parks</th></tr>\
    </tbody>\
    ';
}