/*
 * This file is part of the 3D City Database Importer/Exporter.
 * Copyright (c) 2007 - 2013
 * Institute for Geodesy and Geoinformation Science
 * Technische Universitaet Berlin, Germany
 * http://www.gis.tu-berlin.de/
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, see 
 * <http://www.gnu.org/licenses/>.
 * 
 * The development of the 3D City Database Importer/Exporter has 
 * been financially supported by the following cooperation partners:
 * 
 * Business Location Center, Berlin <http://www.businesslocationcenter.de/>
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * Berlin Senate of Business, Technology and Women <http://www.berlin.de/sen/wtf/>
 */
package org.citydb.modules.common.concurrent;

import java.util.concurrent.locks.ReentrantLock;

import org.citydb.api.concurrent.Worker;
import org.citydb.log.Logger;
import org.citygml4j.util.xml.SAXEventBuffer;
import org.citygml4j.util.xml.SAXWriter;
import org.xml.sax.SAXException;

public class IOWriterWorker extends Worker<SAXEventBuffer> {
	private final Logger LOG = Logger.getInstance();
	private final ReentrantLock runLock = new ReentrantLock();	
	private volatile boolean shouldRun = true;

	private final SAXWriter saxWriter;

	public IOWriterWorker(SAXWriter saxWriter) {
		this.saxWriter = saxWriter;
	}

	@Override
	public void interrupt() {
		shouldRun = false;
		workerThread.interrupt();
	}

	@Override
	public void interruptIfIdle() {
		final ReentrantLock runLock = this.runLock;
		shouldRun = false;

		if (runLock.tryLock()) {
			try {
				workerThread.interrupt();
			} finally {
				runLock.unlock();
			}
		}
	}

	@Override
	public void run() {
    	if (firstWork != null) {
    		doWork(firstWork);
    		firstWork = null;
    	}

    	while (shouldRun) {
			try {
				SAXEventBuffer work = workQueue.take();
				doWork(work);
			} catch (InterruptedException ie) {
				// re-check state
			}
    	}
	}

	private void doWork(SAXEventBuffer work) {
		final ReentrantLock runLock = this.runLock;
        runLock.lock();

        try {
        	work.send(saxWriter, true);
        	saxWriter.flush();
        } catch (SAXException e) {
        	LOG.error("XML error: " + e.getMessage());
        } finally {
        	runLock.unlock();
        }
	}
}
