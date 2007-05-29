/*
 * Created on May 26, 2007
 */
package net.java.ao.blog;

import net.java.ao.EntityManager;
import wicket.Application;
import wicket.IRequestCycleFactory;
import wicket.protocol.http.WebSession;

/**
 * @author Daniel Spiewak
 */
public class BlogSession extends WebSession {
	
	protected BlogSession(Application application) {
		super(application);
	}
}
