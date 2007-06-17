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

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.UnsupportedLookAndFeelException;

import net.java.ao.EntityManager;
import net.java.ao.schema.PluralizedNameConverter;

import org.jdesktop.fuse.ResourceInjector;

/**
 * @author Daniel Spiewak
 */
public final class UIManager {
	private static EntityManager manager;
	
	public static void init() {
		Properties dbProperties = new Properties();
		
		InputStream is = UIManager.class.getResourceAsStream("/db.properties");
		try {
			dbProperties.load(is);
		} catch (IOException e) {
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
		
		manager = new EntityManager(dbProperties.getProperty("db.uri"), dbProperties.getProperty("db.username"), 
				dbProperties.getProperty("db.password"));
		
//		Logger.getLogger("net.java.ao").setLevel(Level.FINE);
		
		manager.setNameConverter(new PluralizedNameConverter());
		
		ResourceInjector.addModule("org.jdesktop.fuse.swing.SwingModule");
		ResourceInjector.get("ui.style").load("/style.properties");
		
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (UnsupportedLookAndFeelException e) {
		}
	}
	
	public static EntityManager getManager() {
		return manager;
	}
	
	public static void show() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				MainFrame frame = new MainFrame();
				frame.setVisible(true);
			}
		});
	}
	
	public static void centerWindow(Window window) {
		int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
		int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
		
		window.setLocation((screenWidth - window.getWidth()) / 2, (screenHeight - window.getHeight()) / 2);
	}
	
	public static void dispose() {
		manager.getProvider().dispose();
	}
}
