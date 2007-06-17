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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.ao.contacts.db.EmailAddress;
import net.java.ao.contacts.db.Person;
import net.java.ao.contacts.db.Friendship;

import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

/**
 * @author Daniel Spiewak
 */
public class MainFrame extends JFrame {
	@InjectedResource
	private GradientPaint backgroundGradient;
	
	public MainFrame() {
		super("Dogfood Contacts");
		
		ResourceInjector.get("ui.style").inject(this);
		
		add(new GradientHeader("Dogfood Contacts"), BorderLayout.NORTH);
		
		JPanel body = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setPaint(backgroundGradient);
				g2.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		body.setLayout(new BorderLayout());
		getContentPane().add(body);
		
		body.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		
		final ContactsTable contactsTable = new ContactsTable();
		contactsTable.addMouseListener(new MouseListener() {
			private long clickTime = 0;
			
			public void mouseClicked(MouseEvent e) {
				if (e.getWhen() - clickTime < 250) {
					mouseDoubleClicked(e);
				}
				clickTime = e.getWhen();
			}
			
			public void mouseDoubleClicked(MouseEvent e) {
				EditPersonDialog dialog = new EditPersonDialog(MainFrame.this, contactsTable.getSelectedContact());
				dialog.setVisible(true);
				
				((ContactsModel) contactsTable.getModel()).refreshModel();
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(contactsTable);
		scrollPane.getViewport().setBackground(Color.WHITE);
		body.add(scrollPane);
		
		JPanel buttons = new JPanel();
		buttons.setOpaque(false);
		body.add(buttons, BorderLayout.SOUTH);
		
		JButton addContact = new JButton("Add Contact");
		addContact.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EditPersonDialog dialog = new EditPersonDialog(MainFrame.this, null);
				dialog.setVisible(true);
				
				((ContactsModel) contactsTable.getModel()).refreshModel();
			}
		});
		addContact.setOpaque(false);
		buttons.add(addContact);
		
		JButton editContact = new JButton("Edit Contact");
		editContact.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EditPersonDialog dialog = new EditPersonDialog(MainFrame.this, contactsTable.getSelectedContact());
				dialog.setVisible(true);
				
				((ContactsModel) contactsTable.getModel()).refreshModel();
			}
		});
		editContact.setOpaque(false);
		buttons.add(editContact);
		
		JButton removeContact = new JButton("Remove Contact");
		removeContact.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Person selectedContact = contactsTable.getSelectedContact();
					for (EmailAddress address : selectedContact.getEmailAddresses()) {
						UIManager.getManager().delete(address);
					}
					for (Friendship relation : UIManager.getManager().find(Friendship.class, "personAID = ? OR personBID = ?", 
							selectedContact.getID(), selectedContact.getID())) {
						UIManager.getManager().delete(relation);
					}
					
					UIManager.getManager().delete(selectedContact);
					
					((ContactsModel) contactsTable.getModel()).refreshModel();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});
		removeContact.setOpaque(false);
		buttons.add(removeContact);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(500, 400);
		
		UIManager.centerWindow(this);
	}
}
