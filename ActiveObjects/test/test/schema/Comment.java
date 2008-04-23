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
package test.schema;

import java.sql.Types;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.SQLType;

/**
 * @author Daniel Spiewak
 */
@Preload("title")
public interface Comment extends Entity {
	public String getTitle();
	public void setTitle(String title);
	
	@SQLType(Types.CLOB)
	public String getText();

	@SQLType(Types.CLOB)
	public void setText(String text);
	
	public Commentable getCommentable();
	public void setCommentable(Commentable commentable);
}
