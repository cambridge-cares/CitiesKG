package uk.ac.cam.cares.twa.cities.tasks.geo.test;

import junit.framework.TestCase;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.geo.GeometryType;
import uk.ac.cam.cares.twa.cities.models.geo.SurfaceGeometry;
import uk.ac.cam.cares.twa.cities.tasks.geo.MultiSurfaceThematicisationTask;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@SuppressWarnings("unchecked")
public class MultiSurfaceThematicisationTaskTest extends TestCase {

  public void testTryClassifyGeometries() throws NoSuchFieldException, IllegalAccessException {
    GeometryType.setSourceCrsName("EPSG:27700");
    /*            root
     *           /    \
     *      walls      roofAndGround
     *      /   \       /     \     \
     *  wall1  wall2  roofs ground1 ground2
     *               /    \
     *           roof1   roof2
     */
    SurfaceGeometry root = new SurfaceGeometry();
    SurfaceGeometry walls = new SurfaceGeometry();
    SurfaceGeometry wall1 = new SurfaceGeometry();
    SurfaceGeometry wall2 = new SurfaceGeometry();
    SurfaceGeometry roofAndGround = new SurfaceGeometry();
    SurfaceGeometry roofs = new SurfaceGeometry();
    SurfaceGeometry roof1 = new SurfaceGeometry();
    SurfaceGeometry roof2 = new SurfaceGeometry();
    SurfaceGeometry ground1 = new SurfaceGeometry();
    SurfaceGeometry ground2 = new SurfaceGeometry();
    wall1.setGeometryType(new GeometryType("0#0#0#0#0#1#1#0#1#1#0#0#0#0#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    wall2.setGeometryType(new GeometryType("0#1#0#1#1#0#1#1#1#0#1#1#0#1#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof1.setGeometryType(new GeometryType("0#0#1#1#0#1#1#1#1#0#1#1#0#0#1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof2.setGeometryType(new GeometryType("0#0#2#1#0#2#1#1#2#0#1#2#0#0#2", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground1.setGeometryType(new GeometryType("0#0#0#0#1#0#1#1#0#1#0#0#0#0#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground2.setGeometryType(new GeometryType("0#0#-1#0#1#-1#1#1#-1#1#0#-1#0#0#-1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    root.getSurfaceGeometries().addAll(Arrays.asList(walls, roofAndGround));
    walls.getSurfaceGeometries().addAll(Arrays.asList(wall1, wall2));
    roofAndGround.getSurfaceGeometries().addAll(Arrays.asList(roofs, ground1, ground2));
    roofs.getSurfaceGeometries().addAll(Arrays.asList(roof1, roof2));

    // Override pull method.
    SurfaceGeometry mockedRoot = Mockito.spy(root);
    Mockito.doNothing().when(mockedRoot).pullAll(Mockito.anyString(), Mockito.anyInt());

    // Collect field access
    Field topLevelThematicGeometriesField = MultiSurfaceThematicisationTask.class.getDeclaredField("topLevelThematicGeometries");
    topLevelThematicGeometriesField.setAccessible(true);
    Field bottomLevelThematicGeometriesField = MultiSurfaceThematicisationTask.class.getDeclaredField("bottomLevelThematicGeometries");
    bottomLevelThematicGeometriesField.setAccessible(true);
    Field mixedGeometriesField = MultiSurfaceThematicisationTask.class.getDeclaredField("mixedGeometries");
    mixedGeometriesField.setAccessible(true);

    // Test bucketing is correct
    MultiSurfaceThematicisationTask task = new MultiSurfaceThematicisationTask(mockedRoot, 1, 1, "");
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
    assertEquals(root, mixeds.get(0));
    assertEquals(roofAndGround, mixeds.get(1));
    // Check not flipped
    assertEquals(Boolean.FALSE, task.flipped);

    // Test version with inverted winding order

    roof1.setGeometryType(new GeometryType("0#0#1#0#1#1#1#1#1#1#0#1#0#0#1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof2.setGeometryType(new GeometryType("0#0#2#0#1#2#1#1#2#1#0#2#0#0#2", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground1.setGeometryType(new GeometryType("0#0#0#1#0#0#1#1#0#0#1#0#0#0#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground2.setGeometryType(new GeometryType("0#0#-1#1#0#-1#1#1#-1#0#1#-1#0#0#-1", "http://localhost/blazegraph/literals/POLYGON-3-15"));

    task = new MultiSurfaceThematicisationTask(mockedRoot, 1, 1, "");
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
    assertEquals(root, mixeds.get(0));
    assertEquals(roofAndGround, mixeds.get(1));
    // Check is flipped
    assertEquals(Boolean.TRUE, task.flipped);

    // Test version with ambiguous winding order: order is actually inverted, but no flip, so reversed expectations

    roof1.setGeometryType(new GeometryType("0#0#1#0#1#1#1#1#1#1#0#1#0#0#1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof2.setGeometryType(new GeometryType("0#0#2#0#1#2#1#1#2#1#0#2#0#0#2", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground1.setGeometryType(new GeometryType("0#0#1#1#0#1#1#1#1#0#1#1#0#0#1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground2.setGeometryType(new GeometryType("0#0#2#1#0#2#1#1#2#0#1#2#0#0#2", "http://localhost/blazegraph/literals/POLYGON-3-15"));

    task = new MultiSurfaceThematicisationTask(mockedRoot, 1, 1, "");
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
    assertEquals(root, mixeds.get(0));
    assertEquals(roofAndGround, mixeds.get(1));
    // Check is flipped
    assertNull(task.flipped);

  }

  public void testImplementChangesAndPush() {
    GeometryType.setSourceCrsName("EPSG:27700");
    /*            root
     *           /    \
     *      walls      roofAndGround
     *      /   \       /     \     \
     *  wall1  wall2  roofs ground1 ground2
     *               /    \
     *           roof1   roof2
     */
    SurfaceGeometry root = Mockito.spy(new SurfaceGeometry());
    SurfaceGeometry walls = Mockito.spy(new SurfaceGeometry());
    SurfaceGeometry wall1 = Mockito.spy(new SurfaceGeometry());
    SurfaceGeometry wall2 = Mockito.spy(new SurfaceGeometry());
    SurfaceGeometry roofAndGround = Mockito.spy(new SurfaceGeometry());
    SurfaceGeometry roofs = Mockito.spy(new SurfaceGeometry());
    SurfaceGeometry roof1 = Mockito.spy(new SurfaceGeometry());
    SurfaceGeometry roof2 = Mockito.spy(new SurfaceGeometry());
    SurfaceGeometry ground1 = Mockito.spy(new SurfaceGeometry());
    SurfaceGeometry ground2 = Mockito.spy(new SurfaceGeometry());
    root.setIri("root_uuid", "http://localhost:9999/blazegraph/namespace/test/");
    walls.setIri("walls_uuid", "http://localhost:9999/blazegraph/namespace/test/");
    wall1.setIri("wall1_uuid", "http://localhost:9999/blazegraph/namespace/test/");
    wall2.setIri("wall2_uuid", "http://localhost:9999/blazegraph/namespace/test/");
    roofAndGround.setIri("roofandground_uuid", "http://localhost:9999/blazegraph/namespace/test/");
    roofs.setIri("roofs_uuid", "http://localhost:9999/blazegraph/namespace/test/");
    roof1.setIri("roof1_uuid", "http://localhost:9999/blazegraph/namespace/test/");
    roof2.setIri("roof2_uuid", "http://localhost:9999/blazegraph/namespace/test/");
    ground1.setIri("ground1_uuid", "http://localhost:9999/blazegraph/namespace/test/");
    ground2.setIri("ground2_uuid", "http://localhost:9999/blazegraph/namespace/test/");
    wall1.setGeometryType(new GeometryType("0#0#0#0#0#1#1#0#1#1#0#0#0#0#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    wall2.setGeometryType(new GeometryType("0#1#0#1#1#0#1#1#1#0#1#1#0#1#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof1.setGeometryType(new GeometryType("0#0#1#1#0#1#1#1#1#0#1#1#0#0#1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    roof2.setGeometryType(new GeometryType("0#0#2#1#0#2#1#1#2#0#1#2#0#0#2", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground1.setGeometryType(new GeometryType("0#0#0#0#1#0#1#1#0#1#0#0#0#0#0", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    ground2.setGeometryType(new GeometryType("0#0#-1#0#1#-1#1#1#-1#1#0#-1#0#0#-1", "http://localhost/blazegraph/literals/POLYGON-3-15"));
    root.getSurfaceGeometries().addAll(Arrays.asList(walls, roofAndGround));
    walls.getSurfaceGeometries().addAll(Arrays.asList(wall1, wall2));
    roofAndGround.getSurfaceGeometries().addAll(Arrays.asList(roofs, ground1, ground2));
    roofs.getSurfaceGeometries().addAll(Arrays.asList(roof1, roof2));

    // Override pull method.
    Mockito.doNothing().when(root).pullAll(Mockito.anyString(), Mockito.anyInt());

    MultiSurfaceThematicisationTask task = new MultiSurfaceThematicisationTask(root, 1, 1, "");
    task.call();
    try(MockedStatic<AccessAgentCaller> mock = Mockito.mockStatic(AccessAgentCaller.class)) { // disables executions
      task.call();
    }

    Mockito.verify(root).queueDeletionUpdate();
    Mockito.verify(roofAndGround).queueDeletionUpdate();
    Mockito.verify(walls, Mockito.never()).queueDeletionUpdate();
    Mockito.verify(roofs, Mockito.never()).queueDeletionUpdate();
    Mockito.verify(wall1, Mockito.never()).queueDeletionUpdate();
    Mockito.verify(wall2, Mockito.never()).queueDeletionUpdate();
    Mockito.verify(roof1, Mockito.never()).queueDeletionUpdate();
    Mockito.verify(roof2, Mockito.never()).queueDeletionUpdate();
    Mockito.verify(ground1, Mockito.never()).queueDeletionUpdate();
    Mockito.verify(ground2, Mockito.never()).queueDeletionUpdate();

    // Roof tree
    assertNull(roofs.getParentId());
    assertEquals(2, roofs.getSurfaceGeometries().size());
    assertSame(roof1, roofs.getSurfaceGeometries().get(0));
    assertSame(roof2, roofs.getSurfaceGeometries().get(1));
    assertEquals(roofs.getIri(), roof1.getRootId());
    assertEquals(roofs.getIri(), roof2.getRootId());
    assertEquals(0, roof1.getSurfaceGeometries().size());
    assertEquals(0, roof2.getSurfaceGeometries().size());
    // Wall tree
    assertNull(walls.getParentId());
    assertEquals(2, walls.getSurfaceGeometries().size());
    assertSame(wall1, walls.getSurfaceGeometries().get(0));
    assertSame(wall2, walls.getSurfaceGeometries().get(1));
    assertEquals(walls.getIri(), wall1.getRootId());
    assertEquals(walls.getIri(), wall2.getRootId());
    assertEquals(0, wall1.getSurfaceGeometries().size());
    assertEquals(0, wall2.getSurfaceGeometries().size());
    // Ground tree
    assertEquals(ground1.getIri(), ground1.getRootId());
    assertEquals(ground2.getIri(), ground2.getRootId());
    assertEquals(0, ground1.getSurfaceGeometries().size());
    assertEquals(0, ground2.getSurfaceGeometries().size());
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
