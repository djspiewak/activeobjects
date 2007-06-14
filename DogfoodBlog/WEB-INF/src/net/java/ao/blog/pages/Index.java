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
package net.java.ao.blog.pages;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;

import net.java.ao.EntityManager;
import net.java.ao.Query;
import net.java.ao.blog.BlogApplication;
import net.java.ao.blog.db.Blog;
import net.java.ao.blog.db.Comment;
import net.java.ao.blog.db.Post;
import wicket.PageParameters;
import wicket.markup.html.WebPage;
import wicket.markup.html.basic.Label;
import wicket.markup.html.basic.MultiLineLabel;
import wicket.markup.html.link.BookmarkablePageLink;
import wicket.markup.html.link.Link;
import wicket.markup.html.link.PageLink;
import wicket.markup.html.list.ListItem;
import wicket.markup.html.list.ListView;
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
		return Arrays.asList(manager.find(Post.class, Query.select().where("blogID = ?", blog).order("published DESC")));
	}
}
