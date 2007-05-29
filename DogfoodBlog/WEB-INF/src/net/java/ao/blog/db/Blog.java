/*
 * Created on May 26, 2007
 */
package net.java.ao.blog.db;

import net.java.ao.Entity;
import net.java.ao.OneToMany;

/**
 * @author Daniel Spiewak
 */
public interface Blog extends Entity {
	public String getName();
	public void setName(String name);
	
	@OneToMany
	public Post[] getPosts();
}
