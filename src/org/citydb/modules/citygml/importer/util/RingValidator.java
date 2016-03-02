/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * (C) 2013 - 2016,
 * Chair of Geoinformatics,
 * Technische Universitaet Muenchen, Germany
 * http://www.gis.bgu.tum.de/
 * 
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 * 
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Muenchen <http://www.moss.de/>
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 */
package org.citydb.modules.citygml.importer.util;

import java.util.List;

import org.citydb.config.internal.Internal;
import org.citydb.log.Logger;
import org.citydb.util.Util;
import org.citygml4j.model.gml.GMLClass;
import org.citygml4j.model.gml.geometry.primitives.DirectPositionList;
import org.citygml4j.model.gml.geometry.primitives.LinearRing;

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
