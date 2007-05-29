/*
 * Created on May 27, 2007
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
