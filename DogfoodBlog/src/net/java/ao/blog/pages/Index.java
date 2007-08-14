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

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import net.java.ao.EntityManager;
import net.java.ao.Query;
import net.java.ao.blog.BlogApplication;
import net.java.ao.blog.core.DefaultModelIteratorAdaptor;
import net.java.ao.blog.core.EntityIterator;
import net.java.ao.blog.db.Blog;
import net.java.ao.blog.db.Comment;
import net.java.ao.blog.db.Post;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ArrayIteratorAdapter;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author Daniel Spiewak
 */
public class Index extends WebPage {

	private Blog blog;

	public Index() throws SQLException {
		EntityManager manager = ((BlogApplication) getApplication()).getEntityManager();
		blog = manager.find(Blog.class)[0];
		add(new Label("pageTitle", new PropertyModel(blog, "name")));
		add(new Label("pageHeader", new PropertyModel(blog, "name")));

		add(new Link("addPostLink") {

			@Override
			public void onClick() {
				setResponsePage(new EditPost(Index.this, blog));
			}
		});

		RefreshingView posts = new RefreshingView("posts") {

			@Override
			protected Iterator getItemModels() {

				Query query = Query.select().where("blogID = ?", blog).order("published DESC");
				EntityIterator<Post> it = EntityIterator.forQuery(BlogApplication.get().getEntityManager(), Post.class, query);
				return new DefaultModelIteratorAdaptor(it);
			}

			@Override
			protected void populateItem(Item item) {
				final Post post = (Post) item.getModelObject();

				BookmarkablePageLink permalink = new BookmarkablePageLink("permalink", ViewPost.class, new PageParameters("item=" + post.getID()));
				item.add(permalink);

				Label postTitle = new Label("postTitle", new PropertyModel(post, "title"));
				permalink.add(postTitle);

				item.add(new Link("editPostLink") {
					@Override
					public void onClick() {
						setResponsePage(new EditPost(Index.this, blog, post));
					}
				});
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
							error(e.getMessage());
						}
					}
				});

				item.add(new Label("postDate", new AbstractReadOnlyModel() {
					@Override
					public Object getObject() {
						DateFormat format = DateFormat.getDateTimeInstance();
						Calendar published = post.getPublished();
						return (published != null) ? format.format(published.getTime()) : null;
					}
				}));

				item.add(new MultiLineLabel("postText", new PropertyModel(post, "text")));
			}
		};
		add(posts);
		add(new FeedbackPanel("feedback"));
	}
}
