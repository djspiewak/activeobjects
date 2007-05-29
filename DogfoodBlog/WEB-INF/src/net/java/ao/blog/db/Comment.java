/*
 * Created on May 27, 2007
 */
package net.java.ao.blog.db;

import java.sql.Types;

import net.java.ao.Entity;
import net.java.ao.schema.SQLType;

/**
 * @author Daniel Spiewak
 */
public interface Comment extends Entity {
	public String getName();
	public void setName(String name);
	
	@SQLType(Types.CLOB)
	public String getComment();

	@SQLType(Types.CLOB)
	public void setComment(String comment);
	
	public Post getPost();
	public void setPost(Post post);
}
