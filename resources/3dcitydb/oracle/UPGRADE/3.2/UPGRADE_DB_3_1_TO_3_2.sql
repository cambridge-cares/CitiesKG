-- 3D City Database - The Open Source CityGML Database
-- http://www.3dcitydb.org/
-- 
-- Copyright 2013 - 2016
-- Chair of Geoinformatics
-- Technical University of Munich, Germany
-- https://www.gis.bgu.tum.de/
-- 
-- The 3D City Database is jointly developed with the following
-- cooperation partners:
-- 
-- virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
-- M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
-- 
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--     http://www.apache.org/licenses/LICENSE-2.0
--     
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

SET SERVEROUTPUT ON

SELECT 'Starting 3D City Database upgrade...' as message from DUAL;

--// drop old versions of CITYDB_PKG
@DROP_CITYDB_PKG.sql

--// contents of @@../../CREATE_CITYDB_PKG.sql
SELECT 'Installing CITYDB packages...' as message from DUAL;
@@../../PL_SQL/CITYDB_PKG/UTIL/UTIL.sql;
@@../../PL_SQL/CITYDB_PKG/INDEX/IDX.sql;
@@../../PL_SQL/CITYDB_PKG/SRS/SRS.sql;
@@../../PL_SQL/CITYDB_PKG/STATISTICS/STAT.sql;
@@../../PL_SQL/CITYDB_PKG/ENVELOPE/ENVELOPE.sql;
@@../../PL_SQL/CITYDB_PKG/DELETE/DELETE.sql;
@@../../PL_SQL/CITYDB_PKG/DELETE/DELETE_BY_LINEAGE.sql;
SELECT 'CITYDB package installed.' as message from DUAL;

SELECT '3D City Database upgrade complete!' as message from DUAL;
