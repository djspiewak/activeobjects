/*
 * Created on May 26, 2007
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
	private DBCPPoolProvider provider;
	
	public EntityManager getEntityManager() {
		return manager;
	}
	
	@Override
	protected void init() {
		super.init();
		
		getMarkupSettings().setStripWicketTags(true);
		
		Properties dbProperties = getDBProperties();
		
		provider = new DBCPPoolProvider(DatabaseProvider.getInstance(dbProperties.getProperty("db.uri"), 
				dbProperties.getProperty("db.username"), dbProperties.getProperty("db.password")));
		manager = new EntityManager(provider);
		
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
			Generator.migrate(provider, Comment.class);
			
			manager.create(Blog.class).setName("AO Dogfood Blog");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void destroy() {
		super.destroy();
		
		provider.close();
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
