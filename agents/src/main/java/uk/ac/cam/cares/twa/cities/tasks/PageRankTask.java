package uk.ac.cam.cares.twa.cities.tasks;

import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

/**
 * Runnable task implementing Page Rank algorithm.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
public class PageRankTask implements Runnable {
  private boolean stop = false;

  public boolean isRunning() {
    return !stop;
  }

  public void stop() {
    stop = true;
  }

  @Override
  public void run() {
    while (isRunning()) {
      try {

      } catch (Exception e) {
        throw new JPSRuntimeException(e);
      } finally {
        stop();
      }
    }
  }

}
