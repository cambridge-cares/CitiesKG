const viewer = new Cesium.Viewer('cesiumContainer', cesiumViewerOptions);
const handler = new Cesium.ScreenSpaceEventHandler(viewer.canvas);



function createPoint(worldPosition) {
  const point = viewer.entities.add({
    position: worldPosition,
    point: {
      color: Cesium.Color.WHITE,
      pixelSize: 5,
      heightReference: Cesium.HeightReference.CLAMP_TO_GROUND,
    },
  });
  return point;
}

handler.setInputAction(function (event) {
  // We use `viewer.scene.globe.pick here instead of `viewer.camera.pickEllipsoid` so that
  // we get the correct point when mousing over terrain.
  const ray = viewer.camera.getPickRay(event.position);
  const earthPosition = viewer.scene.globe.pick(ray, viewer.scene);
  // `earthPosition` will be undefined if our mouse is not over the globe.
  if (Cesium.defined(earthPosition)) {
    if (activeShapePoints.length === 0) {
      floatingPoint = createPoint(earthPosition);
      activeShapePoints.push(earthPosition);
      const dynamicPositions = new Cesium.CallbackProperty(function () {
        if (drawingMode === "polygon") {
          return new Cesium.PolygonHierarchy(activeShapePoints);
        }
        return activeShapePoints;
      }, false);
      activeShape = drawShape(dynamicPositions);
    }
    activeShapePoints.push(earthPosition);
    createPoint(earthPosition);
  }
}, Cesium.ScreenSpaceEventType.LEFT_CLICK);

function drawShape(positionData) {
  let shape;
  if (drawingMode === "line") {
    shape = viewer.entities.add({
      polyline: {
        positions: positionData,
        clampToGround: true,
        width: 3,
      },
    });
  } else if (drawingMode === "polygon") {
    shape = viewer.entities.add({
      polygon: {
        hierarchy: positionData,
        material: new Cesium.ColorMaterialProperty(
            Cesium.Color.WHITE.withAlpha(0.7)
        ),
      },
    });
  }
  return shape;
}
let activeShapePoints = [];
let activeShape;
let floatingPoint;
// Redraw the shape so it's not dynamic and remove the dynamic shape.
function terminateShape() {
  activeShapePoints.pop();
  drawShape(activeShapePoints);
  viewer.entities.remove(floatingPoint);
  viewer.entities.remove(activeShape);
  floatingPoint = undefined;
  activeShape = undefined;
  activeShapePoints = [];
}
handler.setInputAction(function (event) {
  terminateShape();
}, Cesium.ScreenSpaceEventType.RIGHT_CLICK);
