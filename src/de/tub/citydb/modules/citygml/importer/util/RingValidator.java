package de.tub.citydb.modules.citygml.importer.util;

import java.util.List;

import org.citygml4j.model.gml.GMLClass;
import org.citygml4j.model.gml.geometry.primitives.DirectPositionList;
import org.citygml4j.model.gml.geometry.primitives.LinearRing;

import de.tub.citydb.config.internal.Internal;
import de.tub.citydb.log.Logger;
import de.tub.citydb.util.Util;

public class RingValidator {
	private final Logger LOG = Logger.getInstance();

	public boolean validate(LinearRing ring, String parentGmlId) {
		if (ring.hasLocalProperty(Internal.GEOMETRY_INVALID))
			return false;

		List<Double> coords = ring.toList3d();

		if (coords == null || coords.isEmpty()) {
			StringBuilder msg = new StringBuilder(Util.getGeometrySignature(
					GMLClass.LINEAR_RING, 
					parentGmlId));
			msg.append(": Linear ring contains less than 4 coordinates. Skipping invalid ring.");
			LOG.error(msg.toString());
			
			ring.setLocalProperty(Internal.GEOMETRY_INVALID, "Too few coordinates");			
			return false;
		}
		
		// check closedness
		if (!isClosed(coords, ring)) {
			StringBuilder msg = new StringBuilder(Util.getGeometrySignature(
					GMLClass.LINEAR_RING, 
					parentGmlId));
			msg.append(": Linear ring is not closed. Appending first coordinate to fix it.");
			LOG.warn(msg.toString());
		}
		
		// check for minimum number of coordinates
		if (coords.size() / 3 < 4) {
			StringBuilder msg = new StringBuilder(Util.getGeometrySignature(
					GMLClass.LINEAR_RING, 
					parentGmlId));
			msg.append(": Linear ring contains less than 4 coordinates. Skipping invalid ring.");
			LOG.error(msg.toString());
			
			ring.setLocalProperty(Internal.GEOMETRY_INVALID, "Too few coordinates");			
			return false;
		}
		
		return true;
	}
	
	private boolean isClosed(List<Double> coords, LinearRing ring) {
		Double x = coords.get(0);
		Double y = coords.get(1);
		Double z = coords.get(2);

		int nrOfPoints = coords.size();

		if (!x.equals(coords.get(nrOfPoints - 3)) ||
				!y.equals(coords.get(nrOfPoints - 2)) ||
				!z.equals(coords.get(nrOfPoints - 1))) {
			// repair unclosed ring...
			coords.add(x);
			coords.add(y);
			coords.add(z);

			DirectPositionList posList = new DirectPositionList();
			posList.setValue(coords);
			ring.setPosList(posList);

			ring.unsetCoord();
			ring.unsetCoordinates();
			ring.unsetPosOrPointPropertyOrPointRep();
			
			return false;
		}
		
		return true;
	}

}
