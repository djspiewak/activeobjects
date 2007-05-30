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

import net.java.ao.EntityManager;
import net.java.ao.blog.BlogApplication;
import net.java.ao.blog.db.Comment;
import net.java.ao.blog.db.Post;
import wicket.PageParameters;
import wicket.markup.html.WebPage;
import wicket.markup.html.basic.Label;
import wicket.markup.html.basic.MultiLineLabel;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.TextArea;
import wicket.markup.html.form.TextField;
import wicket.markup.html.link.BookmarkablePageLink;
import wicket.markup.html.link.Link;
import wicket.markup.html.link.PageLink;
import wicket.markup.html.list.ListItem;
import wicket.markup.html.list.ListView;
import wicket.model.PropertyModel;

/**
 * @author Daniel Spiewak
 */
public class ViewPost extends WebPage {
	private String pageTitle;
	private String postedFormatted;
	
	public ViewPost(PageParameters params) {
		final Post post = ((BlogApplication) getApplication()).getEntityManager().get(Post.class, Integer.parseInt((String) params.get("item")));
		
		pageTitle = post.getBlog().getName() + ": " + post.getTitle();
		postedFormatted = DateFormat.getDateTimeInstance().format(post.getPublished().getTime());
		
		Link indexLink = new BookmarkablePageLink("indexLink", Index.class);
		add(indexLink);
		
		indexLink.add(new Label("blogTitle", new PropertyModel(post.getBlog(), "name")));
		
		add(new Label("pageTitle", new PropertyModel(this, "pageTitle")));
		add(new Label("pageHeader", new PropertyModel(post, "title")));

		add(new PageLink("editPostLink", new EditPost(this, post.getBlog(), post)));
		
		add(new Label("posted", new PropertyModel(this, "postedFormatted")));
		
		add(new MultiLineLabel("text", new PropertyModel(post, "text")));
		
		final ListView comments = new ListView("comments", Arrays.asList(post.getComments())) {
			@Override
			protected void populateItem(ListItem item) {
				Comment comment = (Comment) item.getModelObject();
				
				item.add(new Label("commentBy", new PropertyModel(comment, "name")));
				item.add(new MultiLineLabel("comment", new PropertyModel(comment, "comment")));
			}
		};
		add(comments);
		
		final CommentBean commentBean = new CommentBean();
		Form commentForm = new Form("commentForm") {
			@Override
			protected void onSubmit() {
				Comment comment = null;
				try {
					comment = ((BlogApplication) getApplication()).getEntityManager().create(Comment.class);
				} catch (SQLException e) {
					e.printStackTrace();
					return;
				}
				
				comment.setName(commentBean.getName());
				comment.setComment(commentBean.getComment());
				comment.setPost(post);
				
				comments.setList(Arrays.asList(post.getComments()));
				comments.modelChanged();
			}
		};
		add(commentForm);
		
		commentForm.add(new TextField("name", new PropertyModel(commentBean, "name")));
		commentForm.add(new TextArea("commentText", new PropertyModel(commentBean, "comment")));
	}

	public String getPostedFormatted() {
		return postedFormatted;
	}

	public void setPostedFormatted(String postedFormatted) {
		this.postedFormatted = postedFormatted;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}
	
	private static class CommentBean implements Serializable {
		private String name, comment;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}
	}
}
