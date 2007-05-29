/*
 * Created on May 26, 2007
 */
package net.java.ao.blog;

import java.sql.SQLException;
import java.util.List;

import net.java.ao.DBParam;
import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;
import net.java.ao.blog.db.Blog;
import net.java.ao.blog.db.Comment;
import net.java.ao.blog.pages.EditPost;
import net.java.ao.blog.pages.Index;
import net.java.ao.blog.pages.ViewPost;
import net.java.ao.db.DBCPPoolProvider;
import net.java.ao.schema.Generator;
import wicket.ISessionFactory;
import wicket.Request;
import wicket.Session;
import wicket.markup.html.WebPage;
import wicket.protocol.http.WebApplication;
import wicket.session.ISessionStore;

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
		
		provider = new DBCPPoolProvider(DatabaseProvider.getInstance(
				"jdbc:mysql://localhost/wicket_blog", "root", "mysqlroot"));
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
	
	private void generateSchema(EntityManager manager) {
		try {
			Generator.generate(provider, Comment.class);
			
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
