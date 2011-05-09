/*
 * This file is part of the 3D City Database Importer/Exporter.
 * Copyright (c) 2007 - 2011
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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
package de.tub.citydb.util.gui;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JTextField;

import de.tub.citydb.config.internal.Internal;
import de.tub.citydb.gui.components.StandardEditingPopupMenu;

public class GuiUtil {

	public static GridBagConstraints setConstraints(int gridx, int gridy, double weightx, double weighty, int fill,
			                                 int insetTop, int insetLeft, int insetBottom, int insetRight) {
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.gridx = gridx;
		constraint.gridy = gridy;
		constraint.weightx = weightx;
		constraint.weighty = weighty;
		constraint.fill = fill;
		constraint.insets = new Insets(insetTop, insetLeft, insetBottom, insetRight);
		return constraint;
	}
	
	public static void setMnemonic(JMenuItem item, String label, int index) {
		try {
			char mnemonic = label.charAt(index);
			item.setMnemonic(mnemonic);
			item.setDisplayedMnemonicIndex(index);
		}  catch (IndexOutOfBoundsException e) {
			//
		}
	}
	
	public static void setMnemonic(JMenuItem item, String labelKey, String indexKey) {		
		try {
			setMnemonic(item, Internal.I18N.getString(labelKey), Integer.valueOf(Internal.I18N.getString(indexKey)));
		} catch (NumberFormatException e) {
			//
		}
	}
	
	public static void addStandardEditingPopupMenu(final JComponent... components) {
		for (final JComponent component : components) {
			final StandardEditingPopupMenu popupMenu = StandardEditingPopupMenu.getInstance(component);
			
			component.addMouseListener(new MouseAdapter() {
				private void processMouseEvent(MouseEvent e) {
					if (e.isPopupTrigger()) {
						if (!e.getComponent().isEnabled())
							return;
						
						if (e.getComponent() instanceof JTextField &&
								!((JTextField)e.getComponent()).isEditable())
							return;

						popupMenu.show(e.getComponent(), e.getX(), e.getY());
						popupMenu.setInvoker(e.getComponent());
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					processMouseEvent(e);
				}

				@Override
				public void mousePressed(MouseEvent e) {
					processMouseEvent(e);
				}

			});
		}
	}

}
