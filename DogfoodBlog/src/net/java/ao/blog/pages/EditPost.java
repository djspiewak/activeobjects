/*
 * Copyright 2007 Daniel Spiewak
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *	    http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.ao.blog.pages;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Calendar;

import net.java.ao.DBParam;
import net.java.ao.blog.BlogApplication;
import net.java.ao.blog.db.Blog;
import net.java.ao.blog.db.Post;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;

/**
 * @author Daniel Spiewak
 */
public class EditPost extends WebPage {
	private String pageHeader = "Edit Post";

	private String submitButton = "Edit";

	public EditPost(Page returnPage, final Blog blog) {
		this(returnPage, blog, new PostBean());

		pageHeader = "Create Post";
		submitButton = "Create";
	}

	public EditPost(final Page returnPage, final Blog blog, final Object bean) {
		add(new Label("pageTitle", new PropertyModel(blog, "name")));
		add(new Label("pageHeader", new PropertyModel(this, "pageHeader")));

		add(new Form("postForm") {
			{
				add(new TextField("postTitle", new PropertyModel(bean, "title")));
				add(new TextArea("postText", new PropertyModel(bean, "text")));

				add(new Button("submitButton", new PropertyModel(this, "submitButton")));
			}

			@Override
			protected void onSubmit() {
				if (!(bean instanceof Post)) {
					Post post = null;
					PostBean postBean = (PostBean) bean;

					try {
						post = ((BlogApplication) getApplication()).getEntityManager().create(Post.class, new DBParam("blogID", blog));
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}

					post.setTitle(postBean.getTitle());
					post.setText(postBean.getText());
					post.setPublished(Calendar.getInstance());
				}
				setResponsePage(returnPage);
			}
		});
	}

	public String getPageHeader() {
		return pageHeader;
	}

	public void setPageHeader(String pageHeader) {
		this.pageHeader = pageHeader;
	}

	public String getSubmitButton() {
		return submitButton;
	}

	public void setSubmitButton(String submitButton) {
		this.submitButton = submitButton;
	}

	private static class PostBean implements Serializable {
		private String title;

		private String text;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
