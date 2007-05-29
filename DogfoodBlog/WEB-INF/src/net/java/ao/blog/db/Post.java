/*
 * Created on May 26, 2007
 */
package net.java.ao.blog.db;

import java.sql.Types;
import java.util.Calendar;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.schema.SQLType;

/**
 * @author Daniel Spiewak
 */
public interface Post extends Entity {
	public String getTitle();
	public void setTitle(String title);
	
	@SQLType(Types.CLOB)
	public String getText();
	
	@SQLType(Types.CLOB)
	public void setText(String text);
	
	public Calendar getPublished();
	public void setPublished(Calendar published);
	
	public Blog getBlog();
	public void setBlog(Blog blog);
	
	@OneToMany
	public Comment[] getComments();
}
