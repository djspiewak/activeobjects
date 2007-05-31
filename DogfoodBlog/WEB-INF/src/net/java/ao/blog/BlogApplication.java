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
package net.java.ao.blog;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;
import net.java.ao.blog.db.Blog;
import net.java.ao.blog.db.Comment;
import net.java.ao.blog.pages.Index;
import net.java.ao.blog.pages.ViewPost;
import net.java.ao.db.DBCPPoolProvider;
import net.java.ao.schema.Generator;
import wicket.ISessionFactory;
import wicket.Session;
import wicket.markup.html.WebPage;
import wicket.protocol.http.WebApplication;

/**
 * @author Daniel Spiewak
 */
public class BlogApplication extends WebApplication {
	private EntityManager manager;
	
	public EntityManager getEntityManager() {
		return manager;
	}
	
	@Override
	protected void init() {
		super.init();
		
		getMarkupSettings().setStripWicketTags(true);
		
		Properties dbProperties = getDBProperties();
		
		String uri = dbProperties.getProperty("db.uri");
		String username = dbProperties.getProperty("db.username");
		String password = dbProperties.getProperty("db.password");
		manager = new EntityManager(DatabaseProvider.getInstance(uri, username, password));
		
		try {
			if (manager.find(Blog.class).length == 0) {
				generateSchema(manager);
			}
		} catch (SQLException e) {
			generateSchema(manager);
		}

		mountBookmarkablePage("/index", Index.class);
		mountBookmarkablePage("/post", ViewPost.class);
	}
	
	private Properties getDBProperties() {
		Properties back = new Properties();
		
		InputStream is = BlogApplication.class.getResourceAsStream("/db.properties");
		
		if (is == null) {
			throw new RuntimeException("Unable to locate db.properties");
		}
		
		try {
			back.load(is);
			is.close();
		} catch (IOException e) {
			throw new RuntimeException("Unable to load db.properties");
		}
		
		return back;
	}
	
	private void generateSchema(EntityManager manager) {
		try {
			Generator.migrate(manager.getProvider(), Comment.class);
			
			manager.create(Blog.class).setName("AO Dogfood Blog");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void destroy() {
		super.destroy();
		
		manager.getProvider().dispose();
	}

	@Override
	public Class<? extends WebPage> getHomePage() {
		return Index.class;
	}

	@Override
	protected ISessionFactory getSessionFactory() {
		return new ISessionFactory() {
			public Session newSession() {
				return new BlogSession(BlogApplication.this);
			}
		};
	}
}
