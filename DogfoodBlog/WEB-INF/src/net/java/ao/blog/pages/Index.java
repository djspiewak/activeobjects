/*
 * Created on May 26, 2007
 */
package net.java.ao.blog.pages;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.java.ao.EntityManager;
import net.java.ao.blog.BlogApplication;
import net.java.ao.blog.db.Blog;
import net.java.ao.blog.db.Comment;
import net.java.ao.blog.db.Post;
import wicket.Component;
import wicket.PageParameters;
import wicket.markup.html.WebPage;
import wicket.markup.html.basic.Label;
import wicket.markup.html.basic.MultiLineLabel;
import wicket.markup.html.link.BookmarkablePageLink;
import wicket.markup.html.link.Link;
import wicket.markup.html.link.PageLink;
import wicket.markup.html.list.ListItem;
import wicket.markup.html.list.ListView;
import wicket.model.IModel;
import wicket.model.PropertyModel;

/**
 * @author Daniel Spiewak
 */
public class Index extends WebPage {
	private ListView posts;
	
	private Blog blog;

	public Index() throws SQLException {
		EntityManager manager = ((BlogApplication) getApplication()).getEntityManager();
		blog = manager.find(Blog.class)[0];
		add(new Label("pageTitle", new PropertyModel(blog, "name")));
		add(new Label("pageHeader", new PropertyModel(blog, "name")));
		
		add(new PageLink("addPostLink", new EditPost(this, blog)));
		
		posts = new ListView("posts", constructPostsList(manager)) {
			@Override
			protected void populateItem(ListItem item) {
				final Post post = (Post) item.getModelObject();
				
				BookmarkablePageLink permalink = new BookmarkablePageLink("permalink", ViewPost.class, 
						new PageParameters("item=" + post.getID()));
				item.add(permalink);
				
				Label postTitle = new Label("postTitle", new PropertyModel(post, "title"));
				permalink.add(postTitle);
				
				item.add(new PageLink("editPostLink", new EditPost(Index.this, blog, post)));
				item.add(new Link("deletePostLink") {
					@Override
					public void onClick() {
						setResponsePage(Index.class);
						
						EntityManager manager = ((BlogApplication) getApplication()).getEntityManager();
						try {
							for (Comment comment : post.getComments()) {
								manager.delete(comment);
							}
							manager.delete(post);
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				});
				
				item.add(new Label("postDate", new PropertyModel(new Serializable() {
					@SuppressWarnings("unused")
					public String getFormattedDate() {
						DateFormat format = DateFormat.getDateTimeInstance();
						return format.format(post.getPublished().getTime());
					}
				}, "formattedDate")));
				
				item.add(new MultiLineLabel("postText", new PropertyModel(post, "text")));
			}
		};
		add(posts);
	}
	
	public void refreshList(EntityManager manager) throws SQLException {
		posts.setList(constructPostsList(manager));
		posts.modelChanged();
	}
	
	private List<Post> constructPostsList(EntityManager manager) throws SQLException {
		return Arrays.asList(manager.find(Post.class, "blogID = ? ORDER BY published DESC", blog));
	}
}
