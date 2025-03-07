## Thematic Surface Discovery Agent User Guide

### Configuration

In the `resources/config.properties` file, the property `uri.route` must be specified. This is the target resource
ID which queries and updates will be addressed to. By default, this is an AccessAgent string, where e.g.
`http://localhost:48888/churchill` is interpreted as "using an AccessAgent instance hosted at `http://localhost:48888`,
access the knowledge graph `churchill` registered in ontokgrouter". Therefore, an AccessAgent instance is required to use this.

### Request

Example HTTPRequest for ThematicSurfaceDiscoveryAgent:
```
PUT http://localhost:8080/agents/discovery/thematicsurface
Content-Type: application/json

{ "namespace": "http://localhost:9999/blazegraph/namespace/churchill/sparql/",
  "cityObjectIRI": "http://localhost:9999/blazegraph/namespace/building/..../",
  "lod2": "true",
  "threshold": "5",
  "mode": "validate" }
```

Parameters:

- `namespace`: target namespace, e.g. "http://localhost:9999/blazegraph/namespace/churchill/sparql/", which is used to prefix named graph IRIs and newly created object IRIs. Typically the same as configured `uri.route`, but not necessarily.
- `mode`: "restructure", "validate" or "footprint". See sections below.
- `lod1`, `lod2`, `lod3`, `lod4` (optional): optionally specify one or more levels of detail to target by providing `true`. All other LoDs are ignored. If no LoD is specified, all are targeted.
- `cityObjectIRI` (optional): optionally specify a single building to target. If omitted, all buildings in the
  namespace are targeted. `namespace` is still necessary even with `cityObjectIRI` specified.
- `threshold` (optional): the threshold angle in degrees; if the angle between a polygon's normal and the horizontal plane exceeds this, it is no longer considered a wall. Defaults to 15 if omitted.

### Restructure mode

In restructure mode, building(s)' `lodXMultiSurface`s are converted into thematic surfaces by reparenting their largest possible thematically homogeneous geometry subtrees to newly created thematic surfaces. `Building.lod1MultiSurfaceId` and `Building.lod2MultiSurfaceId` map to `ThematicSurface.lod2MultiSurfaceId` as thematic surfaces do not have an LoD 1 property; else, the LoD of the linking property is preserved in the conversion.

Before:

![Before](README-resources/tsda-restructure-before.png)

Red, blue and green indicate classified roof, wall and ground surfaces, where a composite surface whose components
are all the same theme is also considered that theme. Brown is a "mixed" composite surface with components of
different themes, and is therefore destroyed in the conversion. After:

![After](README-resources/tsda-restructure-after.png)

### Validate mode

In validate mode, building(s)' thematic surfaces are inspected and their themes classified using the same algorithm as restructure mode. The classifications are tagged onto bottom-level (leaf node) SurfaceGeometries by `rdfs:comment` properties. This allows comparison and debugging in Blazegraph workbench using a query e.g.

```
SELECT ?class ?comment (COUNT(*) AS ?count) WHERE {
  ?surfaceGeometry ocgml:cityObjectId ?cityObject;
            ocgml:GeometryType ?geometryType.
  ?cityObject  ocgml:objectClassId ?class.
  OPTIONAL{?surfaceGeometry rdfs:comment ?comment}
  FILTER(!ISBLANK(?geometryType))
} GROUP BY ?class ?comment
```

which counts all the combinations of original data themes and classified themes.

### Footprint mode

Footprint mode inspects building(s)' `lodXMultiSurface`s, identifying themes as in restructure mode, and then
*copies* identifies ground surfaces into a new SurfaceGeometry tree which is linked to the building by
`lod0FootprintId`. Unlike restructure mode, this does not destroy the old `lodXMultiSurface`. This is useful if the
large triple overhead of a full thematicisation is not desired, but footprints are needed to be extracted for other use.

## UPRN Agent User Guide

An AccessAgent instance is required, similar to the steps described in the Thematic Surface Discovery Agent [configuration](#configuration)

### HTTP Request

Example HTTP Request for UPRN Agent:
```
PUT http://localhost:8080/agents/uprn
Content-Type: application/json

{ "namespace": "http://localhost:9999/blazegraph/namespace/churchill/sparql/",
  "cityObjectIRI": "http://localhost:9999/blazegraph/namespace/churchill/sparql/building/..../" }
```
Parameters:

- `namespace`: target namespace, e.g. "http://localhost:9999/blazegraph/namespace/churchill/sparql/", which is used to prefix named graph IRIs and newly created object IRIs. Typically the same as configured `uri.route`, but not necessarily.
- `cityObjectIRI` (optional): optionally specify a single building to target. If omitted, all buildings in the
  `namespace` are targeted. `namespace` is still necessary even with `cityObjectIRI` specified. Even though the parameter is called cityObjectIRI, it actually refers to a building IRI.


