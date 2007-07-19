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
