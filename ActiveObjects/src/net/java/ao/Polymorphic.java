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
package net.java.ao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Tags a given entity type as polymorphically abstract.  This means that
 * the given type will <i>not</i> peer to any table, but rather represent
 * a polymorphic supertype to other entities (entities which extend the
 * given interface).  Unlike conventional inheritence, which also causes
 * the supertype to not peer to a table, polymorphic type inheritence
 * allows instances of the subtype to be stored in the database into fields
 * which are "typed" in ActiveObjects as the supertype.  All of this is
 * really much simpler than it sounds:</p>
 * 
 * <pre>public interface Person extends Entity {
 *     public Computer getComputer();
 *     public void setComputer(Computer computer);
 * }
 * 
 * &#064;Polymorphic
 * public interface Computer extends Entity {
 *     public float getSpeed();
 *     public void setSpeed(float speed);
 * }
 * 
 * public interface Mac extends Computer {}
 * public interface PC extends Computer {}</pre>
 * 
 * <p>In this case, <code>Computer</code> does not correspond with any table in
 * the database.  Likewise, <code>Person</code> has no foreign keys for the
 * corresponding field to <code>getComputer()</code>.  When an entity type is
 * polymorphic, its subtypes can be used polymorphically as they are in
 * <code>Person</code>.  This is essentially the conceptual analog of an
 * abstract class when mapped into the database.</p>
 * 
 * @author Daniel Spiewak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Polymorphic {}
