/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2018
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.citydb.query.filter.selection.operator.spatial;

import javax.measure.unit.Dimension;

public class Distance {
	private double value;
	private DistanceUnit unit;

	public Distance(double value, DistanceUnit unit) {
		this.value = value;
		setUnit(unit);
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public boolean isSetUnit() {
		return unit != null;
	}

	public boolean isLinearUnit() {
		return unit != null && unit.toUnit().getDimension() == Dimension.LENGTH;
	}

	public boolean isAngularUnit() {
		return unit != null && unit.toUnit().getDimension() == Dimension.NONE;
	}

	public DistanceUnit getUnit() {
		return unit;
	}

	public void setUnit(DistanceUnit unit) {
		if (unit != null && (unit.toUnit().getDimension() == Dimension.LENGTH || unit.toUnit().getDimension() == Dimension.NONE))
			this.unit = unit;
	}

}
