/*
 * Copyright 2007, Daniel Spiewak
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the ActiveObjects project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.java.ao.contacts.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;

import net.java.ao.contacts.db.Person;

import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

/**
 * @author Daniel Spiewak
 */
public class ContactsTable extends JTable {
	private ContactsModel contactsModel;
	
	@InjectedResource
	private int rowHeight;
	
	@InjectedResource
	private Color gradientA, gradientB, columnBGA, columnBGB;

	public ContactsTable() {
		ResourceInjector.get("ui.style").inject(this);
		
		getTableHeader().setVisible(true);

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		setDefaultRenderer(String.class, new CustomStringRenderer());
		setIntercellSpacing(new Dimension(0, 0));
		setShowGrid(false);
		setRowHeight(rowHeight);
		
		contactsModel = new ContactsModel();
		setModel(contactsModel);
	}
	
	public Person getSelectedContact() {
		if (getSelectedRow() < 0) {
			return null;
		}
		
		return contactsModel.getPersonAt(getSelectedRow());
	}
	
	private class CustomStringRenderer implements TableCellRenderer {
		
		public Component getTableCellRendererComponent(JTable table, Object value, final boolean isSelected, 
				boolean hasFocus, final int row, final int column) {
			JLabel label = new JLabel((String) value) {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g;
					
					if (isSelected) {
						GradientPaint selectionGradient = new GradientPaint(0, 0, gradientA, 0, getHeight() - 1, gradientB);
						
						g2.setPaint(selectionGradient);
						g2.fillRect(0, 0, getWidth(), getHeight());
					} else if (column % 2 == 0) {
						g2.setColor(columnBGA);
						g2.fillRect(0, 0, getWidth(), getHeight());
					} else if (column % 2 == 1) {
						g2.setColor(columnBGB);
						g2.fillRect(0, 0, getWidth(), getHeight());
					}
					
					super.paintComponent(g);
				}
			};
			label.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
			
			if (isSelected) {
				label.setForeground(Color.WHITE);
			}
			
			return label;
		}
	}
}
