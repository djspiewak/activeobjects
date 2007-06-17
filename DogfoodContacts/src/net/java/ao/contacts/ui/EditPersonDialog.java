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
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import net.java.ao.contacts.db.EmailAddress;
import net.java.ao.contacts.db.Friendship;
import net.java.ao.contacts.db.Person;

import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

/**
 * @author Daniel Spiewak
 */
public class EditPersonDialog extends JDialog {
	@InjectedResource
	private int cellHeight;
	
	@InjectedResource
	private GradientPaint backgroundGradient;
	
	@InjectedResource
	private Color listBackground, gradientA, gradientB;
	
	public EditPersonDialog(JFrame parent, final Person person) {
		super(parent, (person == null ? "Add Person" : "Edit Person"), true);
		
		ResourceInjector.get("ui.style").inject(this);
		
		ListCellRenderer listCellRenderer = new ListCellRenderer() {
			public Component getListCellRendererComponent(JList list, Object value, int index, final boolean isSelected, boolean cellHasFocus) {
				JLabel label = new JLabel(value.toString()) {
					@Override
					protected void paintComponent(Graphics g) {
						Graphics2D g2 = (Graphics2D) g;
						
						if (isSelected) {
							GradientPaint selectionGradient = new GradientPaint(0, 0, gradientA, 0, getHeight() - 1, gradientB);
							
							g2.setPaint(selectionGradient);
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
		};
		
		add(new GradientHeader((person == null ? "Add Person" : "Edit Person")), BorderLayout.NORTH);
		
		JPanel body = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setPaint(backgroundGradient);
				g2.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		body.setLayout(new SWTGridLayout(2, false));
		body.setBorder(BorderFactory.createEmptyBorder(10, 10, 1, 10));
		getContentPane().add(body);
		
		body.add(new JLabel("First Name:"));
		
		final JTextField firstName = new JTextField();
		if (person != null) {
			firstName.setText(person.getFirstName());
		}
		
		SWTGridData data0 = new SWTGridData();
		data0.horizontalAlignment = SWTGridData.FILL;
		data0.grabExcessHorizontalSpace = true;
		body.add(firstName, data0);
		
		body.add(new JLabel("Last Name:"));
		
		final JTextField lastName = new JTextField();
		if (person != null) {
			lastName.setText(person.getLastName());
		}
		
		SWTGridData data1 = new SWTGridData();
		data1.horizontalAlignment = SWTGridData.FILL;
		data1.grabExcessHorizontalSpace = true;
		body.add(lastName, data1);
		
		body.add(new JLabel("Email:"));
		body.add(new JLabel());
		
		JPanel emailBody = new JPanel(new BorderLayout());
		emailBody.setOpaque(false);
		
		SWTGridData data2 = new SWTGridData();
		data2.horizontalAlignment = SWTGridData.FILL;
		data2.grabExcessHorizontalSpace = true;
		data2.verticalAlignment = SWTGridData.FILL;
		data2.grabExcessVerticalSpace = true;
		data2.horizontalSpan = 2;
		body.add(emailBody, data2);
		
		Vector<String> serializedEmails = new Vector<String>();
		if (person != null) {
			for (EmailAddress emailAddress : person.getEmailAddresses()) {
				serializedEmails.add(emailAddress.getEmail());
			}
		}
		
		final JList emails = new JList(serializedEmails);
		emails.setFixedCellHeight(cellHeight);
		emails.setBackground(listBackground);
		emails.setCellRenderer(listCellRenderer);
		emails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		emailBody.add(new JScrollPane(emails));
		
		JPanel rightEmailButtons = new JPanel(new SWTGridLayout(1, false));
		rightEmailButtons.setOpaque(false);
		emailBody.add(rightEmailButtons, BorderLayout.EAST);
		
		final JButton plusEmail = new JButton("+");
		plusEmail.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String email = JOptionPane.showInputDialog(EditPersonDialog.this, "Enter the email address:");
				
				Vector<String> emailData = new Vector<String>();
				for (int i = 0; i < emails.getModel().getSize(); i++) {
					emailData.add((String) emails.getModel().getElementAt(i));
				}
				emailData.add(email);
				
				emails.setListData(emailData);
			}
		});
		plusEmail.setOpaque(false);
		
		SWTGridData data4 = new SWTGridData();
		data4.horizontalAlignment = SWTGridData.FILL;
		rightEmailButtons.add(plusEmail, data4);
		
		final JButton minusEmail = new JButton("-");
		minusEmail.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Vector<Object> emailData = new Vector<Object>();
				for (int i = 0; i < emails.getModel().getSize(); i++) {
					if (!emails.getSelectedValue().equals(emails.getModel().getElementAt(i))) {
						emailData.add(emails.getModel().getElementAt(i));
					}
				}
				
				emails.setListData(emailData);
			}
		});
		minusEmail.setOpaque(false);
		
		SWTGridData data5 = new SWTGridData();
		data5.horizontalAlignment = SWTGridData.FILL;
		rightEmailButtons.add(minusEmail, data5);
		
		body.add(new JLabel("People:"));
		body.add(new JLabel());
		
		JPanel peopleBody = new JPanel(new BorderLayout());
		peopleBody.setOpaque(false);
		
		SWTGridData data6 = new SWTGridData();
		data6.horizontalAlignment = SWTGridData.FILL;
		data6.grabExcessHorizontalSpace = true;
		data6.verticalAlignment = SWTGridData.FILL;
		data6.grabExcessHorizontalSpace = true;
		data6.horizontalSpan = 2;
		body.add(peopleBody, data6);
		
		Vector<PersonWrapper> personWrappers = new Vector<PersonWrapper>();
		if (person != null) {
			for (Person related : person.getPeople()) {
				personWrappers.add(new PersonWrapper(related));
			}
		}
		
		final JList people = new JList(personWrappers);
		people.setFixedCellHeight(cellHeight);
		people.setBackground(listBackground);
		people.setCellRenderer(listCellRenderer);
		people.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		peopleBody.add(new JScrollPane(people));
		
		JPanel rightPeopleButtons = new JPanel(new SWTGridLayout(1, false));
		rightPeopleButtons.setOpaque(false);
		peopleBody.add(rightPeopleButtons, BorderLayout.EAST);
		
		final JButton plusPerson = new JButton("+");
		plusPerson.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					final PersonSelector selector = new PersonSelector(EditPersonDialog.this);
					selector.setLocation(plusPerson.getLocationOnScreen());
					selector.addComponentListener(new ComponentListener() {
						public void componentHidden(ComponentEvent e) {
							Person selectedPerson = selector.getPerson();
							if (selectedPerson == null) {
								return;
							}
							
							Vector<PersonWrapper> personData = new Vector<PersonWrapper>();
							for (int i = 0; i < people.getModel().getSize(); i++) {
								personData.add((PersonWrapper) people.getModel().getElementAt(i));
							}
							personData.add(new PersonWrapper(selectedPerson));
							
							people.setListData(personData);
						}
	
						public void componentMoved(ComponentEvent e) {
						}
	
						public void componentResized(ComponentEvent e) {
						}
	
						public void componentShown(ComponentEvent e) {
						}
					});
					selector.setVisible(true);
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});
		plusPerson.setOpaque(false);
		
		SWTGridData data7 = new SWTGridData();
		data7.horizontalAlignment = SWTGridData.FILL;
		rightPeopleButtons.add(plusPerson, data7);
		
		final JButton minusPerson = new JButton("-");
		minusPerson.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Vector<Object> personData = new Vector<Object>();
				for (int i = 0; i < people.getModel().getSize(); i++) {
					if (!emails.getSelectedValue().equals(people.getModel().getElementAt(i))) {
						personData.add(people.getModel().getElementAt(i));
					}
				}
				
				people.setListData(personData);
			}
		});
		minusPerson.setOpaque(false);
		
		SWTGridData data8 = new SWTGridData();
		data8.horizontalAlignment = SWTGridData.FILL;
		rightPeopleButtons.add(minusPerson, data8);
		
		JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setOpaque(false);
		
		SWTGridData data3 = new SWTGridData();
		data3.horizontalAlignment = SWTGridData.FILL;
		data3.grabExcessHorizontalSpace = true;
		data3.verticalAlignment = SWTGridData.FILL;
		data3.horizontalSpan = 2;
		body.add(footer, data3);
		
		final JButton cancel = new JButton("Cancel");
		cancel.setOpaque(false);
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		footer.add(cancel);
		
		final JButton ok = new JButton((person == null ? "Add" : "Edit"));
		ok.setOpaque(false);
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				firstName.setEnabled(false);
				lastName.setEnabled(false);
				
				plusEmail.setEnabled(false);
				minusEmail.setEnabled(false);
				
				plusPerson.setEnabled(false);
				minusPerson.setEnabled(false);
				
				cancel.setEnabled(false);
				ok.setEnabled(false);
				
				new Thread() {
					{
						setPriority(3);
					}
					
					public void run() {
						Person editPerson = person;
						if (editPerson == null) {
							try {
								editPerson = UIManager.getManager().create(Person.class);
							} catch (SQLException e1) {
								e1.printStackTrace();
							}
						}
						
						editPerson.setFirstName(firstName.getText());
						editPerson.setLastName(lastName.getText());
						
						try {
							editPerson.save();
						} catch (SQLException e) {
							e.printStackTrace();
							return;
						}
						
						EmailAddress[] existingEmails = editPerson.getEmailAddresses();
						
						for (int i = 0; i < emails.getModel().getSize(); i++) {
							String email = (String) emails.getModel().getElementAt(i);
							if (!containsEmail(existingEmails, email)) {
								try {
									EmailAddress newAddress = UIManager.getManager().create(EmailAddress.class);
									
									newAddress.setEmail(email);
									newAddress.setPerson(editPerson);
									newAddress.save();
								} catch (SQLException e1) {
									e1.printStackTrace();
								}
							}
						}
						
						Person[] existingPeople = editPerson.getPeople();
						
						for (int i = 0; i < people.getModel().getSize(); i++) {
							PersonWrapper wrapper = (PersonWrapper) people.getModel().getElementAt(i);
							if (!containsPerson(existingPeople, wrapper)) {
								try {
									Friendship relation = UIManager.getManager().create(Friendship.class);
									
									relation.setFromPerson(wrapper.getPerson());
									relation.setToPerson(editPerson);
									relation.save();
								} catch (SQLException e1) {
									e1.printStackTrace();
								}
							}
						}
						
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								dispose();
							}
						});
					}
				}.start();
			}
		});
		getRootPane().setDefaultButton(ok);
		footer.add(ok);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(300, 500);
		
		UIManager.centerWindow(this);
	}
	
	private boolean containsEmail(EmailAddress[] addresses, String address) {
		for (EmailAddress email : addresses) {
			if (email.getEmail().equalsIgnoreCase(address)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean containsPerson(Person[] people, PersonWrapper wrapper) {
		for (Person person : people) {
			if (person.equals(wrapper.getPerson())) {
				return true;
			}
		}
		return false;
	}
	
	private static class PersonSelector extends JDialog {
		private Person person;
		 
		public PersonSelector(JDialog parent) throws SQLException {
			super(parent, true);
			
			((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			setUndecorated(true);
			
			Person[] peopleObjects = UIManager.getManager().find(Person.class);
			Vector<PersonWrapper> peopleStrings = new Vector<PersonWrapper>();

			for (Person person : peopleObjects) {
				peopleStrings.add(new PersonWrapper(person));
			}

			final JList people = new JList(peopleStrings);
			people.addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
					PersonWrapper wrapper = (PersonWrapper) people.getSelectedValue();
					
					if (wrapper != null) {
						person = wrapper.getPerson();
					}
					
					setVisible(false);
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
			
			JScrollPane scrollPane = new JScrollPane(people);
			scrollPane.getViewport().setBackground(Color.WHITE);
			getContentPane().add(scrollPane);
			
			setSize(200, 200);
		}
		
		public Person getPerson() {
			return person;
		}
	}
	
	private static class PersonWrapper {
		private Person person;
		
		public PersonWrapper(Person person) {
			this.person = person;
		}
		
		public Person getPerson() {
			return person;
		}
		
		@Override
		public String toString() {
			return person.getFirstName() + " " + person.getLastName();
		}
	}
}
