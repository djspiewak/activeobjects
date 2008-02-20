/*
 * Copyright 2008 Daniel Spiewak
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>WARNING: <i>Not</i> part of the public API.  This class is public only
 * to allow its use within other packages in the ActiveObjects library.</p>
 * 
 * <p>About now is when I start missing Scala's case classes...</p>
 * 
 * @author Daniel Spiewak
 */
public class AnnotationDelegate {
	private final Method method1, method2;
	
	AnnotationDelegate(Method method1, Method method2) {
		this.method1 = method1;
		this.method2 = method2;
	}
	
	public <T extends Annotation> T getAnnotation(Class<T> type) {
		T back = method1.getAnnotation(type);
		if (back == null && method2 != null) {
			back = method2.getAnnotation(type);
		}
		
		return back;
	}
	
	public Annotation[] getAnnotations() {
		List<Annotation> back = new ArrayList<Annotation>();
		
		back.addAll(Arrays.asList(method1.getAnnotations()));
		if (method2 != null) {
			back.addAll(Arrays.asList(method2.getAnnotations()));
		}
		
		return back.toArray(new Annotation[back.size()]);
	}

	public Method getMethod1() {
		return method1;
	}

	public Method getMethod2() {
		return method2;
	}
}
