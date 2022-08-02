package uk.ac.cam.cares.twa.cities.tasks.geo.test;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.ogm.models.ModelContext;
import uk.ac.cam.cares.ogm.models.geo.GeometryType;
import uk.ac.cam.cares.ogm.models.geo.SurfaceGeometry;
import uk.ac.cam.cares.twa.cities.tasks.geo.MultiSurfaceThematicisationTask;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MultiSurfaceThematicisationTaskTest{

  static Field contextField;
  static Field rootsField;
  static ThematicSurfaceDiscoveryAgent.Params params = new ThematicSurfaceDiscoveryAgent.Params(
      "",
      "",
      new boolean[]{true, true, true, true},
      1,
      ThematicSurfaceDiscoveryAgent.Mode.RESTRUCTURE
  );

  static {
    try {
      contextField = MultiSurfaceThematicisationTask.class.getDeclaredField("context");
      contextField.setAccessible(true);
      rootsField = MultiSurfaceThematicisationTask.class.getDeclaredField("roots");
      rootsField.setAccessible(false);
      rootsField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public ModelContext makeMockContext() {
    ModelContext mockContext = Mockito.spy(params.makeContext());
    Mockito.doReturn(new ArrayList<SurfaceGeometry>()).when(mockContext).pullAllWhere(Mockito.any(), Mockito.any());
    Mockito.doNothing().when(mockContext).pushAllChanges();
    return mockContext;
  }

  @Test
  public void testTryClassifyGeometries() throws NoSuchFieldException, IllegalAccessException {

    ModelContext context = makeMockContext();
    
    GeometryType.setSourceCrsName("EPSG:27700");
    /*            root
     *           /    \
     *      walls      roofAndGround
     *      /   \       /     \     \
     *  wall1  wall2  roofs ground1 ground2
     *               /    \
     *           roof1   roof2
     */
    String prefix = "http://localhost:9999/blazegraph/namespace/test/surfacegeometry/";
    SurfaceGeometry root = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "root_uuid"));
    SurfaceGeometry walls = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "walls_uuid"));
    SurfaceGeometry wall1 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "wall1_uuid"));
    SurfaceGeometry wall2 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "wall2_uuid"));
    SurfaceGeometry roofAndGround = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "roofandground_uuid"));
    SurfaceGeometry roofs = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "roofs_uuid"));
    SurfaceGeometry roof1 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "roof1_uuid"));
    SurfaceGeometry roof2 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "roof2_uuid"));
    SurfaceGeometry ground1 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "ground1_uuid"));
    SurfaceGeometry ground2 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "ground2_uuid"));
    wall1.setGeometryType(new GeometryType("0#0#0#0#0#1#1#0#1#1#0#0#0#0#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    wall2.setGeometryType(new GeometryType("0#1#0#1#1#0#1#1#1#0#1#1#0#1#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof1.setGeometryType(new GeometryType("0#0#1#1#0#1#1#1#1#0#1#1#0#0#1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof2.setGeometryType(new GeometryType("0#0#2#1#0#2#1#1#2#0#1#2#0#0#2", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground1.setGeometryType(new GeometryType("0#0#0#0#1#0#1#1#0#1#0#0#0#0#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground2.setGeometryType(new GeometryType("0#0#-1#0#1#-1#1#1#-1#1#0#-1#0#0#-1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    root.getChildGeometries().addAll(Arrays.asList(walls, roofAndGround));
    walls.getChildGeometries().addAll(Arrays.asList(wall1, wall2));
    roofAndGround.getChildGeometries().addAll(Arrays.asList(roofs, ground1, ground2));
    roofs.getChildGeometries().addAll(Arrays.asList(roof1, roof2));

    // Collect field access
    Field topLevelThematicGeometriesField = MultiSurfaceThematicisationTask.class.getDeclaredField("topLevelThematicGeometries");
    topLevelThematicGeometriesField.setAccessible(true);
    Field bottomLevelThematicGeometriesField = MultiSurfaceThematicisationTask.class.getDeclaredField("bottomLevelThematicGeometries");
    bottomLevelThematicGeometriesField.setAccessible(true);
    Field mixedGeometriesField = MultiSurfaceThematicisationTask.class.getDeclaredField("mixedGeometries");
    mixedGeometriesField.setAccessible(true);

    // Test bucketing is correct
    MultiSurfaceThematicisationTask task = new MultiSurfaceThematicisationTask(1, params, "");
    contextField.set(task, context);
    rootsField.set(task, new SurfaceGeometry[]{root});
    task.call();
    List<List<SurfaceGeometry>> topLevels = (List<List<SurfaceGeometry>>) topLevelThematicGeometriesField.get(task);
    List<List<SurfaceGeometry>> bottomLevels = (List<List<SurfaceGeometry>>) bottomLevelThematicGeometriesField.get(task);
    List<SurfaceGeometry> mixeds = (List<SurfaceGeometry>) mixedGeometriesField.get(task);
    // Check top levels
    assertEquals(1, topLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).size());
    assertSame(roofs, topLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).get(0));
    assertEquals(1, topLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).size());
    assertSame(walls, topLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).get(0));
    assertEquals(2, topLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).size());
    assertSame(ground1, topLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(0));
    assertSame(ground2, topLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(1));
    // check bottom levels
    assertEquals(2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).size());
    assertSame(roof1, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).get(0));
    assertSame(roof2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).get(1));
    assertEquals(2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).size());
    assertSame(wall1, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).get(0));
    assertSame(wall2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).get(1));
    assertEquals(2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).size());
    assertSame(ground1, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(0));
    assertSame(ground2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(1));
    // Check mixed
    assertEquals(2, mixeds.size());
    assertEquals(roofAndGround, mixeds.get(0));
    assertEquals(root, mixeds.get(1));
    // Check not flipped
    assertEquals(Boolean.FALSE, task.flipped);

    // Test version with inverted winding order

    roof1.setGeometryType(new GeometryType("0#0#1#0#1#1#1#1#1#1#0#1#0#0#1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof2.setGeometryType(new GeometryType("0#0#2#0#1#2#1#1#2#1#0#2#0#0#2", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground1.setGeometryType(new GeometryType("0#0#0#1#0#0#1#1#0#0#1#0#0#0#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground2.setGeometryType(new GeometryType("0#0#-1#1#0#-1#1#1#-1#0#1#-1#0#0#-1", "http://localhost/blazegraph/literals/POLYGON-3-15"));

    task = new MultiSurfaceThematicisationTask(1, params, "");
    contextField.set(task, makeMockContext());
    rootsField.set(task, new SurfaceGeometry[]{root});
    task.call();
    topLevels = (List<List<SurfaceGeometry>>) topLevelThematicGeometriesField.get(task);
    bottomLevels = (List<List<SurfaceGeometry>>) bottomLevelThematicGeometriesField.get(task);
    mixeds = (List<SurfaceGeometry>) mixedGeometriesField.get(task);
    // Check top levels
    assertEquals(1, topLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).size());
    assertSame(roofs, topLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).get(0));
    assertEquals(1, topLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).size());
    assertSame(walls, topLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).get(0));
    assertEquals(2, topLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).size());
    assertSame(ground1, topLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(0));
    assertSame(ground2, topLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(1));
    // check bottom levels
    assertEquals(2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).size());
    assertSame(roof1, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).get(0));
    assertSame(roof2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).get(1));
    assertEquals(2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).size());
    assertSame(wall1, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).get(0));
    assertSame(wall2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).get(1));
    assertEquals(2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).size());
    assertSame(ground1, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(0));
    assertSame(ground2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(1));
    // Check mixed
    assertEquals(2, mixeds.size());
    assertEquals(roofAndGround, mixeds.get(0));
    assertEquals(root, mixeds.get(1));
    // Check is flipped
    assertEquals(Boolean.TRUE, task.flipped);

    // Test version with ambiguous winding order: order is actually inverted, but no flip, so reversed expectations

    roof1.setGeometryType(new GeometryType("0#0#1#0#1#1#1#1#1#1#0#1#0#0#1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof2.setGeometryType(new GeometryType("0#0#2#0#1#2#1#1#2#1#0#2#0#0#2", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground1.setGeometryType(new GeometryType("0#0#1#1#0#1#1#1#1#0#1#1#0#0#1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground2.setGeometryType(new GeometryType("0#0#2#1#0#2#1#1#2#0#1#2#0#0#2", "http://localhost/blazegraph/literals/POLYGON-3-15"));

    task = new MultiSurfaceThematicisationTask(1, params, "");
    contextField.set(task, makeMockContext());
    rootsField.set(task, new SurfaceGeometry[]{root});
    task.call();
    topLevels = (List<List<SurfaceGeometry>>) topLevelThematicGeometriesField.get(task);
    bottomLevels = (List<List<SurfaceGeometry>>) bottomLevelThematicGeometriesField.get(task);
    mixeds = (List<SurfaceGeometry>) mixedGeometriesField.get(task);
    // Check top levels
    // Check top levels
    assertEquals(2, topLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).size());
    assertSame(ground1, topLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).get(0));
    assertSame(ground2, topLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).get(1));
    assertEquals(1, topLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).size());
    assertSame(walls, topLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).get(0));
    assertEquals(1, topLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).size());
    assertSame(roofs, topLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(0));
    // check bottom levels
    assertEquals(2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).size());
    assertSame(ground1, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).get(0));
    assertSame(ground2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.ROOF.index).get(1));
    assertEquals(2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).size());
    assertSame(wall1, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).get(0));
    assertSame(wall2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.WALL.index).get(1));
    assertEquals(2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).size());
    assertSame(roof1, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(0));
    assertSame(roof2, bottomLevels.get(MultiSurfaceThematicisationTask.Theme.GROUND.index).get(1));
    // Check mixed
    assertEquals(2, mixeds.size());
    assertEquals(roofAndGround, mixeds.get(0));
    assertEquals(root, mixeds.get(1));
    // Check is flipped
    assertNull(task.flipped);

  }

  @Test
  public void testImplementChangesAndPush() throws IllegalAccessException, URISyntaxException {

    ModelContext context = makeMockContext();

    GeometryType.setSourceCrsName("EPSG:27700");
    /*            root
     *           /    \
     *      walls      roofAndGround
     *      /   \       /     \     \
     *  wall1  wall2  roofs ground1 ground2
     *               /    \
     *           roof1   roof2
     */
    String prefix = "http://localhost:9999/blazegraph/namespace/test/surfacegeometry/";
    SurfaceGeometry root = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "root_uuid"));
    SurfaceGeometry walls = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "walls_uuid"));
    SurfaceGeometry wall1 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "wall1_uuid"));
    SurfaceGeometry wall2 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "wall2_uuid"));
    SurfaceGeometry roofAndGround = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "roofandground_uuid"));
    SurfaceGeometry roofs = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "roofs_uuid"));
    SurfaceGeometry roof1 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "roof1_uuid"));
    SurfaceGeometry roof2 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "roof2_uuid"));
    SurfaceGeometry ground1 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "ground1_uuid"));
    SurfaceGeometry ground2 = Mockito.spy(context.createNewModel(SurfaceGeometry.class, prefix + "ground2_uuid"));
    wall1.setGeometryType(new GeometryType("0#0#0#0#0#1#1#0#1#1#0#0#0#0#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    wall2.setGeometryType(new GeometryType("0#1#0#1#1#0#1#1#1#0#1#1#0#1#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof1.setGeometryType(new GeometryType("0#0#1#1#0#1#1#1#1#0#1#1#0#0#1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof2.setGeometryType(new GeometryType("0#0#2#1#0#2#1#1#2#0#1#2#0#0#2", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground1.setGeometryType(new GeometryType("0#0#0#0#1#0#1#1#0#1#0#0#0#0#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground2.setGeometryType(new GeometryType("0#0#-1#0#1#-1#1#1#-1#1#0#-1#0#0#-1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    walls.setCityObjectId(new URI("http://dummyiri"));
    roofs.setCityObjectId(new URI("http://dummyiri"));
    ground1.setCityObjectId(new URI("http://dummyiri"));
    ground2.setCityObjectId(new URI("http://dummyiri"));
    root.getChildGeometries().addAll(Arrays.asList(walls, roofAndGround));
    walls.getChildGeometries().addAll(Arrays.asList(wall1, wall2));
    roofAndGround.getChildGeometries().addAll(Arrays.asList(roofs, ground1, ground2));
    roofs.getChildGeometries().addAll(Arrays.asList(roof1, roof2));

    Mockito.doNothing().when(context).pushAllChanges();

    MultiSurfaceThematicisationTask task = new MultiSurfaceThematicisationTask(1, params, "");
    contextField.set(task, makeMockContext());
    rootsField.set(task, new SurfaceGeometry[]{root});
    task.call();
    task.call();

    Mockito.verify(root).delete(true);
    Mockito.verify(roofAndGround).delete(true);
    Mockito.verify(walls, Mockito.never()).delete(true);
    Mockito.verify(roofs, Mockito.never()).delete(true);
    Mockito.verify(wall1, Mockito.never()).delete(true);
    Mockito.verify(wall2, Mockito.never()).delete(true);
    Mockito.verify(roof1, Mockito.never()).delete(true);
    Mockito.verify(roof2, Mockito.never()).delete(true);
    Mockito.verify(ground1, Mockito.never()).delete(true);
    Mockito.verify(ground2, Mockito.never()).delete(true);

    // Roof tree
    assertNull(roofs.getParentId());
    assertEquals(2, roofs.getChildGeometries().size());
    assertSame(roof1, roofs.getChildGeometries().get(0));
    assertSame(roof2, roofs.getChildGeometries().get(1));
    assertEquals(roofs.getId(), roof1.getRootId());
    assertEquals(roofs.getId(), roof2.getRootId());
    assertEquals(0, roof1.getChildGeometries().size());
    assertEquals(0, roof2.getChildGeometries().size());
    // Wall tree
    assertNull(walls.getParentId());
    assertEquals(2, walls.getChildGeometries().size());
    assertSame(wall1, walls.getChildGeometries().get(0));
    assertSame(wall2, walls.getChildGeometries().get(1));
    assertEquals(walls.getId(), wall1.getRootId());
    assertEquals(walls.getId(), wall2.getRootId());
    assertEquals(0, wall1.getChildGeometries().size());
    assertEquals(0, wall2.getChildGeometries().size());
    // Ground tree
    assertEquals(ground1.getId(), ground1.getRootId());
    assertEquals(ground2.getId(), ground2.getRootId());
    assertEquals(0, ground1.getChildGeometries().size());
    assertEquals(0, ground2.getChildGeometries().size());
    // Check created thematic surfaces
    assertNotNull(roofs.getCityObjectId());
    assertEquals(roofs.getCityObjectId(), roof1.getCityObjectId());
    assertEquals(roofs.getCityObjectId(), roof2.getCityObjectId());
    assertNotNull(walls.getCityObjectId());
    assertEquals(walls.getCityObjectId(), wall1.getCityObjectId());
    assertEquals(walls.getCityObjectId(), wall2.getCityObjectId());
    assertNotNull(ground1.getCityObjectId());
    assertNotNull(ground2.getCityObjectId());
    assertNotEquals(ground1.getCityObjectId(), ground2.getCityObjectId());
    assertNotEquals(ground1.getCityObjectId(), roofs.getCityObjectId());
    assertNotEquals(ground1.getCityObjectId(), walls.getCityObjectId());

  }

}
