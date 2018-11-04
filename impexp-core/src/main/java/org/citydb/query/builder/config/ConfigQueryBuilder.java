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
package org.citydb.query.builder.config;

import org.citydb.config.geometry.BoundingBox;
import org.citydb.config.geometry.GeometryType;
import org.citydb.config.project.exporter.SimpleQuery;
import org.citydb.config.project.exporter.TilingOptions;
import org.citydb.config.project.kmlExporter.KmlTilingOptions;
import org.citydb.config.project.kmlExporter.SimpleKmlQuery;
import org.citydb.config.project.query.filter.selection.AbstractPredicate;
import org.citydb.config.project.query.filter.selection.comparison.LikeOperator;
import org.citydb.config.project.query.filter.selection.spatial.WithinOperator;
import org.citydb.config.project.query.filter.tiling.Tiling;
import org.citydb.config.project.kmlExporter.SimpleKmlQueryMode;
import org.citydb.config.project.query.simple.SimpleBBOXMode;
import org.citydb.config.project.query.simple.SimpleBBOXOperator;
import org.citydb.config.project.query.simple.SimpleSelectionFilter;
import org.citydb.database.adapter.AbstractDatabaseAdapter;
import org.citydb.database.schema.mapping.SchemaMapping;
import org.citydb.query.Query;
import org.citydb.query.builder.QueryBuildException;
import org.citydb.query.filter.FilterException;
import org.citydb.query.filter.projection.ProjectionFilter;
import org.citydb.query.filter.selection.Predicate;
import org.citydb.query.filter.selection.SelectionFilter;
import org.citydb.query.filter.selection.operator.logical.BinaryLogicalOperator;
import org.citydb.query.filter.selection.operator.logical.LogicalOperatorName;
import org.citydb.query.filter.type.FeatureTypeFilter;
import org.citydb.util.Util;
import org.citygml4j.model.module.citygml.CityGMLVersion;
import org.citygml4j.model.module.citygml.CoreModule;

import javax.xml.namespace.NamespaceContext;
import java.util.ArrayList;
import java.util.List;

public class ConfigQueryBuilder {
	private final SchemaMapping schemaMapping;
	private final AbstractDatabaseAdapter databaseAdapter;

	public ConfigQueryBuilder(SchemaMapping schemaMapping, AbstractDatabaseAdapter databaseAdapter) {
		this.schemaMapping = schemaMapping;
		this.databaseAdapter = databaseAdapter;
	}

	public Query buildQuery(org.citydb.config.project.query.Query queryConfig, NamespaceContext namespaceContext) throws QueryBuildException {
		Query query = new Query();

		// target SRS
		if (queryConfig.isSetTargetSRS())
			query.setTargetSRS(queryConfig.getTargetSRS());
		else
			query.setTargetSRS(databaseAdapter.getConnectionMetaData().getReferenceSystem());

		// feature type filter
		if (queryConfig.isSetFeatureTypeFilter() && !queryConfig.getFeatureTypeFilter().isEmpty()) {
			FeatureTypeFilterBuilder typeBuilder = new FeatureTypeFilterBuilder(query, schemaMapping);
			query.setFeatureTypeFilter(typeBuilder.buildFeatureTypeFilter(queryConfig.getFeatureTypeFilter()));
		} else {
			try {
				query.setFeatureTypeFilter(new FeatureTypeFilter(schemaMapping.getFeatureType("_CityObject", CoreModule.v2_0_0.getNamespaceURI())));
				query.setTargetVersion(CityGMLVersion.v2_0_0);
			} catch (FilterException e) {
				throw new QueryBuildException("Failed to build the export filter.", e);
			}
		}

		// counter filter
		if (queryConfig.isSetCounterFilter()) {
			try {
				CounterFilterBuilder counterFilterBuilder = new CounterFilterBuilder();
				query.setCounterFilter(counterFilterBuilder.buildCounterFilter(queryConfig.getCounterFilter()));
			} catch (FilterException e) {
				throw new QueryBuildException("Failed to build the export filter.", e);
			}
		}

		// lod filter
		if (queryConfig.isSetLodFilter()) {
			LodFilterBuilder lodFilterBuilder = new LodFilterBuilder();
			query.setLodFilter(lodFilterBuilder.buildLodFilter(queryConfig.getLodFilter()));
		}

		// projection filter
		if (queryConfig.isSetProjectionFilter()) {
			ProjectionFilterBuilder builder = new ProjectionFilterBuilder(schemaMapping);
			for (ProjectionFilter projectionFilter : builder.buildProjectionFilter(queryConfig.getProjectionFilter()))
				query.addProjectionFilter(projectionFilter);
		}

		// selection filter
		if (queryConfig.isSetSelectionFilter()) {
			AbstractPredicate predicate = queryConfig.getSelectionFilter().getPredicate();
			PredicateBuilder predicateBuilder = new PredicateBuilder(query, schemaMapping, namespaceContext, databaseAdapter);			
			query.setSelection(new SelectionFilter(predicateBuilder.buildPredicate(predicate)));
		}

		// appearance filter
		if (queryConfig.isSetAppearanceFilter()) {
			AppearanceFilterBuilder builder = new AppearanceFilterBuilder();
			query.setAppearanceFilter(builder.buildAppearanceFilter(queryConfig.getAppearanceFilter()));
		}

		// tiling
		if (queryConfig.isSetTiling()) {
			TilingFilterBuilder tilingFilterBuilder = new TilingFilterBuilder(databaseAdapter);
			query.setTiling(tilingFilterBuilder.buildTilingFilter(queryConfig.getTiling()));
		}

		return query;
	}

	public Query buildQuery(SimpleQuery queryConfig, NamespaceContext namespaceContext) throws QueryBuildException {
		// support for legacy CityGML export filter
		Query query = new Query();

		// CityGML version
		CityGMLVersion version = Util.toCityGMLVersion(queryConfig.getVersion()); 
		query.setTargetVersion(version);

		// target SRS
		if (queryConfig.isSetTargetSRS())
			query.setTargetSRS(queryConfig.getTargetSRS());
		else
			query.setTargetSRS(databaseAdapter.getConnectionMetaData().getReferenceSystem());

		PredicateBuilder predicateBuilder = new PredicateBuilder(query, schemaMapping, namespaceContext, databaseAdapter);

		// lod filter
		if (queryConfig.isUseLodFilter() && queryConfig.isSetLodFilter()) {
			LodFilterBuilder lodFilterBuilder = new LodFilterBuilder();
			query.setLodFilter(lodFilterBuilder.buildLodFilter(queryConfig.getLodFilter()));
		}

		// simple filter settings
		List<Predicate> predicates = new ArrayList<>();

		// feature type filter
		if (queryConfig.isUseTypeNames()) {
			if (queryConfig.isSetFeatureTypeFilter() && !queryConfig.getFeatureTypeFilter().isEmpty()) {
				FeatureTypeFilterBuilder featureTypeFilterBuilder = new FeatureTypeFilterBuilder(query, schemaMapping);
				query.setFeatureTypeFilter(featureTypeFilterBuilder.buildFeatureTypeFilter(queryConfig.getFeatureTypeFilter(), version));
			} else
				throw new QueryBuildException("The feature type filter must not be empty.");
		} else {
			try {
				query.setFeatureTypeFilter(new FeatureTypeFilter(schemaMapping.getFeatureType("_CityObject", CoreModule.v2_0_0.getNamespaceURI())));
			} catch (FilterException e) {
				throw new QueryBuildException("Failed to build the export filter.", e);
			}
		}

		if (queryConfig.isUseSelectionFilter() && queryConfig.isSetSelectionFilter()) {
			SimpleSelectionFilter selectionFilter = queryConfig.getSelectionFilter();

			if (!selectionFilter.isUseSQLFilter()) {
				// gml:id filter
				if (selectionFilter.isSetGmlIdFilter() && selectionFilter.getGmlIdFilter().isSetResourceIds())
					predicates.add(predicateBuilder.buildPredicate(selectionFilter.getGmlIdFilter()));

				// gml:name filter
				if (selectionFilter.isSetGmlNameFilter() && selectionFilter.getGmlNameFilter().isSetLiteral()) {
					LikeOperator gmlNameFilter = selectionFilter.getGmlNameFilter();
					gmlNameFilter.setLiteral(gmlNameFilter.getLiteral());
					gmlNameFilter.setValueReference("gml:name");
					predicates.add(predicateBuilder.buildPredicate(gmlNameFilter));
				}
			} else if (selectionFilter.getSQLFilter().isSetValue()) {
				// SQL filter
				SelectOperatorBuilder selectOperatorBuilder = new SelectOperatorBuilder();
				predicates.add(selectOperatorBuilder.buildSelectOperator(selectionFilter.getSQLFilter()));
			}
		}

		// counter filter
		if (queryConfig.isUseCountFilter() && queryConfig.isSetCounterFilter()) {
			try {
				CounterFilterBuilder counterFilterBuilder = new CounterFilterBuilder();
				query.setCounterFilter(counterFilterBuilder.buildCounterFilter(queryConfig.getCounterFilter()));
			} catch (FilterException e) {
				throw new QueryBuildException("Failed to build the export filter.", e);
			}
		}

		// bbox filter
		if (queryConfig.isUseBboxFilter() && queryConfig.isSetBboxFilter()) {
			if (!queryConfig.getBboxFilter().isSetEnvelope())
				throw new QueryBuildException("The bounding box filter requires an " + GeometryType.ENVELOPE + " as spatial operand.");

			// tiling
			if (queryConfig.isUseTiling()) {
				TilingOptions tilingOptions = queryConfig.getTilingOptions();

				Tiling tiling = new Tiling();
				tiling.setExtent((BoundingBox) queryConfig.getBboxFilter().getEnvelope());
				tiling.setRows(tilingOptions.getRows());
				tiling.setColumns(tilingOptions.getColumns());
				tiling.setTilingOptions(tilingOptions);

				TilingFilterBuilder tilingFilterBuilder = new TilingFilterBuilder(databaseAdapter);
				query.setTiling(tilingFilterBuilder.buildTilingFilter(tiling));
			}

			// bbox
			else {
				if (queryConfig.getBboxFilter().getBboxMode() == SimpleBBOXMode.BBOX) {
					SimpleBBOXOperator bbox = queryConfig.getBboxFilter();
					bbox.setValueReference("gml:boundedBy");
					predicates.add(predicateBuilder.buildPredicate(bbox));
				} else if (queryConfig.getBboxFilter().getBboxMode() == SimpleBBOXMode.WITHIN) {
					WithinOperator within = new WithinOperator();
					within.setValueReference(queryConfig.getBboxFilter().getValueReference());
					within.setSpatialOperand(queryConfig.getBboxFilter().getEnvelope());
					predicates.add(predicateBuilder.buildPredicate(within));
				}
			}
		}

		if (!predicates.isEmpty()) {
			try {
				BinaryLogicalOperator predicate = new BinaryLogicalOperator(LogicalOperatorName.AND, predicates);
				query.setSelection(new SelectionFilter(predicate));
			} catch (FilterException e) {
				throw new QueryBuildException("Failed to build the export filter.", e);
			}
		}

		return query;
	}

	public Query buildQuery(SimpleKmlQuery queryConfig, NamespaceContext namespaceContext) throws QueryBuildException {
		// support for legacy KML export filter
		Query query = new Query();

		// always use CityGML 2.0 as target version
		query.setTargetVersion(CityGMLVersion.v2_0_0);

		PredicateBuilder predicateBuilder = new PredicateBuilder(query, schemaMapping, namespaceContext, databaseAdapter);

		// simple filter settings
		if (queryConfig.getMode() == SimpleKmlQueryMode.SINGLE) {
			// set feature type filter
			try {
				query.setFeatureTypeFilter(new FeatureTypeFilter(schemaMapping.getFeatureType("_CityObject", CoreModule.v2_0_0.getNamespaceURI())));
			} catch (FilterException e) {
				throw new QueryBuildException("Failed to build the export filter.", e);
			}

			// gml:id filter
			if (queryConfig.isSetAttributeFilter() && queryConfig.getAttributeFilter().isSetGmlIdFilter())
				query.setSelection(new SelectionFilter(predicateBuilder.buildPredicate(queryConfig.getAttributeFilter().getGmlIdFilter())));
		}

		// complex filter settings
		else {
			// feature type filter
			if (queryConfig.isSetFeatureTypeFilter()) {
				if (queryConfig.getFeatureTypeFilter().isEmpty())
					throw new QueryBuildException("The feature type filter must not be empty.");
				
				FeatureTypeFilterBuilder featureTypeFilterBuilder = new FeatureTypeFilterBuilder(query, schemaMapping);
				query.setFeatureTypeFilter(featureTypeFilterBuilder.buildFeatureTypeFilter(queryConfig.getFeatureTypeFilter(), CityGMLVersion.v2_0_0));
			} else {
				try {
					query.setFeatureTypeFilter(new FeatureTypeFilter(schemaMapping.getFeatureType("_CityObject", CoreModule.v2_0_0.getNamespaceURI())));
				} catch (FilterException e) {
					throw new QueryBuildException("Failed to build the export filter.", e);
				}
			}

			// bbox filter
			if (queryConfig.isSetBboxFilter()) {
				if (!queryConfig.getBboxFilter().isSetEnvelope())
					throw new QueryBuildException("The bounding box filter requires an " + GeometryType.ENVELOPE + " as spatial operand.");

				KmlTilingOptions tilingOptions = queryConfig.getTilingOptions();

				Tiling tiling = new Tiling();
				tiling.setExtent(queryConfig.getBboxFilter().getEnvelope());
				tiling.setRows(tilingOptions.getRows());
				tiling.setColumns(tilingOptions.getColumns());
				tiling.setTilingOptions(tilingOptions);

				TilingFilterBuilder tilingFilterBuilder = new TilingFilterBuilder(databaseAdapter);
				query.setTiling(tilingFilterBuilder.buildTilingFilter(tiling));				
			}
		}

		return query;
	}

}
