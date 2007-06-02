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

import java.sql.SQLException;

import javax.swing.table.AbstractTableModel;

import net.java.ao.contacts.db.EmailAddress;
import net.java.ao.contacts.db.Person;

/**
 * @author Daniel Spiewak
 */
public class ContactsModel extends AbstractTableModel {
	private Person[] people;
	
	public ContactsModel() {
		refreshModel();
	}
	
	@Override
	public String getColumnName(int column) {
		switch (column) {
			case 0:
				return "First Name";
				
			case 1:
				return "Last Name";
				
			case 2:
				return "Email";
		}
		return null;
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	public int getColumnCount() {
		return 3;
	}

	public int getRowCount() {
		return people.length;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case 0:
				return people[rowIndex].getFirstName();
				
			case 1:
				return people[rowIndex].getLastName();
				
			case 2:
				EmailAddress[] addresses = people[rowIndex].getEmailAddresses();
				String value = "";
				
				if (addresses.length > 0) {
					value += addresses[0].getEmail() + (addresses.length > 1 ? ", ..." : "");
				}
				
				return value;
		}
		return null;
	}
	
	public Person getPersonAt(int rowIndex) {
		return people[rowIndex];
	}
	
	public void refreshModel() {
		try {
			people = UIManager.getManager().find(Person.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		fireTableDataChanged();
	}
}
