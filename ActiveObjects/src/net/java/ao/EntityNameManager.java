/*
 * Copyright 2007, Daniel Spiewak
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the ActiveObjects project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.java.ao;

import java.util.HashMap;
import java.util.Map;

import net.java.ao.schema.CamelCaseNameConverter;
import net.java.ao.schema.PluggableNameConverter;

/**
 * @author Daniel Spiewak
 */
public final class EntityNameManager {
	private static EntityNameManager instance;
	
	private PluggableNameConverter defaultConverter;
	private Map<Class<? extends Entity>, PluggableNameConverter> converters;
	
	private EntityNameManager(PluggableNameConverter defaultConverter) {
		this.defaultConverter = defaultConverter;
		
		converters = new HashMap<Class<? extends Entity>, PluggableNameConverter>();
	}

	public void setDefaultConverter(PluggableNameConverter converter) {
		this.defaultConverter = converter;
	}
	
	public PluggableNameConverter getConverter(Class<? extends Entity> entity) {
		if (entity == null || !converters.containsKey(entity)) {
			return defaultConverter;
		}
		
		return converters.get(entity);
	}
	
	public void setConverter(Class<? extends Entity> entity, PluggableNameConverter converter) {
		converters.put(entity, converter);
	}
	
	public String getName(Class<? extends Entity> entity) {
		return getConverter(entity).getName(entity);
	}

	public static EntityNameManager getInstance() {
		if (instance == null) {
			instance = new EntityNameManager(new CamelCaseNameConverter());
		}
		
		return instance;
	}
}
