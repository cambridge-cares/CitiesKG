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
package org.citydb.gui.components.checkboxtree;

import javax.swing.tree.TreePath;

/**
 * PropagatePreservingCheckTreeCheckingMode define a TreeCheckingMode with down
 * and up recursion of the check when nodes are clicked. It propagates the change
 * not only to descendants but also to ancestors. With regard to descendants
 * this mode behaves exactly like the Propagate mode. With regard to ancestors
 * it checks/unchecks them as needed so that a node is checked if and only if
 * all of its children are checked.
 * 
 * @author Boldrini
 */
public class PropagatePreservingCheckTreeCheckingMode extends TreeCheckingMode {

    PropagatePreservingCheckTreeCheckingMode(DefaultTreeCheckingModel model) {
	super(model);
    }

    @Override
    public void checkPath(TreePath path) {
	// check is propagated to children
	this.model.checkSubTree(path);
	// check all the ancestors with subtrees checked
	TreePath[] parents = new TreePath[path.getPathCount()];
	parents[0] = path;
	boolean uncheckAll = false;
	boolean greyAll = false;
	for (int i = 1; i < parents.length; i++) {
	    parents[i] = parents[i - 1].getParentPath();
	    if (uncheckAll) {
		this.model.removeFromCheckedPathsSet(parents[i]);
		if (greyAll) {
		    this.model.addToGreyedPathsSet(parents[i]);
		} else {
		    if (this.model.pathHasUncheckedChildren(parents[i])) {
			this.model.addToGreyedPathsSet(parents[i]);
			greyAll = true;
		    } else {
			this.model.removeFromGreyedPathsSet(parents[i]);
		    }
		}
	    } else {
		switch (this.model.getChildrenChecking(parents[i])) {
		case HALF_CHECKED:
		    this.model.removeFromCheckedPathsSet(parents[i]);
		    this.model.addToGreyedPathsSet(parents[i]);
		    uncheckAll = true;
		    greyAll = true;
		    break;
		case ALL_UNCHECKED:
		    this.model.removeFromCheckedPathsSet(parents[i]);
		    this.model.removeFromGreyedPathsSet(parents[i]);
		    uncheckAll = true;
		    break;
		case ALL_CHECKED:
		    this.model.addToCheckedPathsSet(parents[i]);
		    this.model.removeFromGreyedPathsSet(parents[i]);
		    break;
		default:
		case NO_CHILDREN:
		    System.err.println("This should not happen (PropagatePreservingCheckTreeCheckingMode)");
		    break;
		}
	    }
	}
    }

    @Override
    public void uncheckPath(TreePath path) {
	// uncheck is propagated to children
	this.model.uncheckSubTree(path);
	TreePath parentPath = path;
	// uncheck is propagated to parents, too
	while ((parentPath = parentPath.getParentPath()) != null) {
	    this.model.removeFromCheckedPathsSet(parentPath);
	    this.model.updatePathGreyness(parentPath);
	}
    }

    /*
         * (non-Javadoc)
         * 
         * @see it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingMode#updateCheckAfterChildrenInserted(javax.swing.tree.TreePath)
         */
    @Override
    public void updateCheckAfterChildrenInserted(TreePath parent) {
	if (this.model.isPathChecked(parent)) {
	    checkPath(parent);
	} else {
	    uncheckPath(parent);
	}
    }

    /*
         * (non-Javadoc)
         * 
         * @see it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingMode#updateCheckAfterChildrenRemoved(javax.swing.tree.TreePath)
         */
    @Override
    public void updateCheckAfterChildrenRemoved(TreePath parent) {
	if (!this.model.isPathChecked(parent)) {
	    // System.out.println(parent +" was removed (not checked)");
	    if (this.model.getChildrenPath(parent).length != 0) {
		if (!this.model.pathHasChildrenWithValue(parent, false)) {
		    // System.out.println("uncheking it");
		    checkPath(parent);
		}
	    }
	}
	this.model.updatePathGreyness(parent);
	this.model.updateAncestorsGreyness(parent);
    }

    /*
         * (non-Javadoc)
         * 
         * @see it.cnr.imaa.essi.lablib.gui.checkboxtree.TreeCheckingMode#updateCheckAfterStructureChanged(javax.swing.tree.TreePath)
         */
    @Override
    public void updateCheckAfterStructureChanged(TreePath parent) {
	if (this.model.isPathChecked(parent)) {
	    checkPath(parent);
	} else {
	    uncheckPath(parent);
	}
    }

}
