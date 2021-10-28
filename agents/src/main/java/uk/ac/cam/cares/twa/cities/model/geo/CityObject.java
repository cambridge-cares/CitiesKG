package uk.ac.cam.cares.twa.cities.model.geo;

import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.KGRouter;

/**
 * GenericAttribute class is good.
 */
public class CityObject {

  private KnowledgeBaseClientInterface kgClient;
  private static String route = "http://kb/singapore-local";


  /**
   * sets KG Client for specific endpoint.
   */
  private void setKGClient(){

    this.kgClient = KGRouter.getKnowledgeBaseClient(CityObject.route,
        true,
        false);
  }
}
