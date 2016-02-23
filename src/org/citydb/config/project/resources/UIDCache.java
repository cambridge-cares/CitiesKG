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
package org.citydb.config.project.resources;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="UIDCacheType", propOrder={
		"feature",
		"geometry"		
})
public class UIDCache {
	@XmlElement(required=true)
	private UIDCacheConfig feature;
	@XmlElement(required=true)
	private UIDCacheConfig geometry;

	public UIDCache() {
		feature = new UIDCacheConfig();
		geometry = new UIDCacheConfig();
	}

	public UIDCacheConfig getFeature() {
		return feature;
	}

	public void setFeature(UIDCacheConfig feature) {
		if (feature != null)
			this.feature = feature;
	}

	public UIDCacheConfig getGeometry() {
		return geometry;
	}

	public void setGeometry(UIDCacheConfig geometry) {
		if (geometry != null)
			this.geometry = geometry;
	}

}
