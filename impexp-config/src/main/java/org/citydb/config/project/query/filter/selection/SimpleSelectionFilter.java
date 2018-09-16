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
package org.citydb.config.project.query.filter.selection;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.citydb.config.project.query.filter.selection.comparison.LikeOperator;
import org.citydb.config.project.query.filter.selection.id.ResourceIdOperator;
import org.citydb.config.project.query.filter.selection.spatial.BBOXOperator;
import org.citydb.config.project.query.filter.selection.spatial.SimpleBBOXMode;

@XmlType(name="SimpleSelectionType", propOrder={
		"gmlIdFilter",
		"gmlNameFilter",
		"bboxFilter"
})
public class SimpleSelectionFilter {
	@XmlAttribute(required = true)
	private SimpleBBOXMode bboxMode = SimpleBBOXMode.BBOX;
	@XmlElement(name = "gmlIds", required = true)
	private ResourceIdOperator gmlIdFilter;
	@XmlElement(name = "gmlName", required = true)
	private LikeOperator gmlNameFilter;
	@XmlElement(name = "bbox", required = true)
	private BBOXOperator bboxFilter;

	public SimpleSelectionFilter() {
		gmlIdFilter = new ResourceIdOperator();
		gmlNameFilter = new LikeOperator();
		bboxFilter = new BBOXOperator();
	}
	
	public ResourceIdOperator getGmlIdFilter() {
		return gmlIdFilter;
	}
	
	public boolean isSetGmlIdFilter() {
		return gmlIdFilter != null;
	}

	public void setGmlIdFilter(ResourceIdOperator gmlIdFilter) {
		this.gmlIdFilter = gmlIdFilter;
	}

	public LikeOperator getGmlNameFilter() {
		return gmlNameFilter;
	}
	
	public boolean isSetGmlNameFilter() {
		return gmlNameFilter != null;
	}

	public void setGmlNameFilter(LikeOperator gmlNameFilter) {
		this.gmlNameFilter = gmlNameFilter;
	}

	public BBOXOperator getBboxFilter() {
		return bboxFilter;
	}
	
	public boolean isSetBboxFilter() {
		return bboxFilter != null;
	}

	public void setBboxFilter(BBOXOperator bboxFilter) {
		this.bboxFilter = bboxFilter;
	}

	public SimpleBBOXMode getBboxMode() {
		return bboxMode;
	}

	public void setBboxMode(SimpleBBOXMode bboxMode) {
		this.bboxMode = bboxMode;
	}
	
}
