


function createCard(){

}



function createTable(){
    let comparisonPanel = document.getElementsById('comparisonPanel');


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