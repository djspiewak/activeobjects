/*
 * Created on May 2, 2007
 */
package net.java.ao;

import static net.java.ao.Utilities.convertDowncaseName;
import static net.java.ao.Utilities.convertSimpleClassName;
import static net.java.ao.Utilities.interfaceIneritsFrom;
import static net.java.ao.Utilities.getTableName;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Daniel Spiewak
 */
class EntityProxy<T extends Entity> implements InvocationHandler {
	private EntityManager manager;	
	private Class<T> type;
	
	private Map<String, Object> cache;
	private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
	
	private int id;

	public EntityProxy(EntityManager manager, Class<T> type) {
		this.manager = manager;
		this.type = type;
		
		cache = new HashMap<String, Object>();
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("setID")) {
			setID((Integer) args[0]);

			return Void.TYPE;
		} else if (method.getName().equals("getID")) {
			return getID();
		} else if (method.getName().equals("getTableName")) {
			return getTableName(type);
		} else if (method.getName().equals("hashCode")) {
			return hashCodeImpl();
		} else if (method.getName().equals("equals")) {
			return equalsImpl((Entity) proxy, args[0]);
		} else if (method.getName().equals("toString")) {
			return toStringImpl();
		}

		String tableName = getTableName(type);
		
		Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
		Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
		OneToMany oneToManyAnnotation = method.getAnnotation(OneToMany.class);
		ManyToMany manyToManyAnnotation = method.getAnnotation(ManyToMany.class);

		if (mutatorAnnotation != null) {
			invokeSetter(getID(), tableName, mutatorAnnotation.value(), args[0]);
			return Void.TYPE;
		} else if (accessorAnnotation != null) {
			return invokeGetter(getID(), tableName, accessorAnnotation.value(), method.getReturnType());
		} else if (oneToManyAnnotation != null && method.getReturnType().isArray() 
				&& interfaceIneritsFrom(method.getReturnType().getComponentType(), Entity.class)) {
			Class<? extends Entity> type = (Class<? extends Entity>) method.getReturnType().getComponentType();
			String otherTableName = getTableName(type);
			
			String mapField = oneToManyAnnotation.value();
			if (mapField.equals("")) {
				mapField = tableName + "ID";
			}
			
			return retrieveRelations(otherTableName, mapField, getID(), (Class<? extends Entity>) type);
		} else if (manyToManyAnnotation != null) {
			if (method.getReturnType().isArray() && interfaceIneritsFrom(method.getReturnType().getComponentType(), Entity.class)) {
				String manyTable = manyToManyAnnotation.table();
				if (manyTable.equals("")) {
					manyTable = getManyRelationTable((Class<? extends Entity>) method.getReturnType().getComponentType());
				}
				
				String relateSelf = manyToManyAnnotation.relateSelf();
				if (relateSelf.equals("")) {
					relateSelf = getManyRelationField(type);
				}
				
				String relateOther = manyToManyAnnotation.relateOther();
				if (relateOther.equals("")) {
					relateOther = getManyRelationField((Class<? extends Entity>) method.getReturnType().getComponentType());
				}
				
				return retrieveManyRelations(manyTable, relateSelf, relateOther, 
						(Class<? extends Entity>) method.getReturnType().getComponentType());
			} else if (method.getParameterTypes()[0].isArray() && interfaceIneritsFrom(method.getParameterTypes()[0].getComponentType(), Entity.class)
					&& method.getReturnType().equals(Void.TYPE)) {
				String manyTable = manyToManyAnnotation.table();
				if (manyTable.equals("")) {
					manyTable = getManyRelationTable((Class<? extends Entity>) method.getParameterTypes()[0].getComponentType());
				}
				
				String relateSelf = manyToManyAnnotation.relateSelf();
				if (relateSelf.equals("")) {
					relateSelf = getManyRelationField(type);
				}
				
				String relateOther = manyToManyAnnotation.relateOther();
				if (relateOther.equals("")) {
					relateOther = getManyRelationField((Class<? extends Entity>) method.getParameterTypes()[0].getComponentType());
				}
				
				setManyRelations(manyTable, relateSelf, relateOther, (Entity[]) args[0]);
				
				return Void.TYPE;
			} else {
				throw new RuntimeException("Unrecognized many to many relationship: " + method.getName());
			}
		} else if (method.getName().startsWith("get")) {
			String name = convertDowncaseName(method.getName().substring(3));
			if (interfaceIneritsFrom(method.getReturnType(), Entity.class)) {
				name += "ID";
			}
			
			return invokeGetter(getID(), tableName, name, method.getReturnType());
		} else if (method.getName().startsWith("is")) {
			String name = convertDowncaseName(method.getName().substring(2));
			if (interfaceIneritsFrom(method.getReturnType(), Entity.class)) {
				name += "ID";
			}
			
			return invokeGetter(getID(), tableName, name, method.getReturnType());
		} else if (method.getName().startsWith("set")) {
			String name = convertDowncaseName(method.getName().substring(3));
			if (interfaceIneritsFrom(method.getParameterTypes()[0], Entity.class)) {
				name += "ID";
			}
			invokeSetter(getID(), tableName, name, args[0]);
			
			return Void.TYPE;
		}

		return null;
	}

	public int getID() {
		return id;
	}

	public void setID(int id) {
		this.id = id;
	}
	
	public int hashCodeImpl() {
		return (int) (new Random(getID()).nextFloat() * getID()) + getID() % (2 << 15);
	}

	public boolean equalsImpl(Entity proxy, Object obj) {
		if (proxy == obj) {
			return true;
		}
		
		if (obj instanceof Entity) {
			Entity entity = (Entity) obj;
			
			return entity.getID() == proxy.getID() && entity.getTableName().equals(proxy.getTableName());
		}
		
		return false;
	}

	public String toStringImpl() {
		return getTableName(type) + " {id = " + getID() + "}";
	}
	
	private Connection getConnectionImpl() throws SQLException {
		return DBEncapsulator.getInstance(manager.getProvider()).getConnection();
	}
	
	private void closeConnectionImpl(Connection conn) throws SQLException {
		DBEncapsulator.getInstance(manager.getProvider()).closeConnection(conn);
	}

	private <V> V invokeGetter(int id, String table, String name, Class<V> type) throws Throwable {
		V back = null;
		
		cacheLock.writeLock().lock();
		try {
			if (cache.containsKey(name)) {
				return (V) cache.get(name);
			}
			
			Connection conn = getConnectionImpl();
			
			try {
				PreparedStatement stmt = conn.prepareStatement("SELECT " + name + " FROM " + table + " WHERE id = ?");
				stmt.setInt(1, id);
	
				ResultSet res = stmt.executeQuery();
				if (res.next()) {
					back = convertValue(res, name, type);
				}
				res.close();
				stmt.close();
			} finally {
				closeConnectionImpl(conn);
			}
	
			if (back != null) {
				cache.put(name, back);
			}
		} finally {
			cacheLock.writeLock().unlock();
		}
		
		return back;
	}

	private void invokeSetter(int id, String table, String name, Object value) throws Throwable {
		cacheLock.writeLock().lock();
		try {
			cache.put(name, value);
		} finally {
			cacheLock.writeLock().unlock();
		}
		
		Connection conn = getConnectionImpl();
		try {
			String sql = "UPDATE " + table + " SET " + name + " = ? WHERE id = ?";

			if (value == null) {
				sql = "UPDATE " + table + " SET " + name + " = NULL WHERE id = ?";
			}

			PreparedStatement stmt = conn.prepareStatement(sql);

			int index = 1;
			if (value != null) {
				convertValue(stmt, index++, value);
			}
			stmt.setInt(index++, id);

			stmt.executeUpdate();

			stmt.close();
		} finally {
			closeConnectionImpl(conn);
		}
	}
	
	private <V extends Entity> V[] retrieveRelations(String table, String relate, int id, Class<V> type) throws SQLException {
		List<V> back = new ArrayList<V>(); 
		Connection conn = getConnectionImpl();
		
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT id FROM " + table + " WHERE " + relate + " = ?");
			stmt.setInt(1, id);
			
			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				back.add(manager.getEntity(res.getInt("id"), type));
			}
			res.close();
			stmt.close();
		} finally {
			closeConnectionImpl(conn);
		}
		
		return back.toArray((V[]) Array.newInstance(type, back.size()));
	}
	
	private <V extends Entity> V[] retrieveManyRelations(String tableRelate, String relateSelf, String relateOther, Class<V> type) throws SQLException {
		List<V> back = new ArrayList<V>();
		Connection conn = getConnectionImpl();
		
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT " + relateOther + " FROM " + tableRelate + " WHERE " + relateSelf + " = ?");
			stmt.setInt(1, getID());
			
			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				back.add(manager.getEntity(res.getInt(relateOther), type));
			}
			res.close();
			stmt.close();
		} finally {
			closeConnectionImpl(conn);
		}
		
		return back.toArray((V[]) Array.newInstance(type, back.size()));
	}
	
	private void setManyRelations(String tableRelate, String relateSelf, String relateOther, Entity[] values) throws SQLException {
		Connection conn = getConnectionImpl();
		
		try {
			PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + tableRelate + " WHERE " + relateSelf + " = ?");
			stmt.setInt(1, getID());
			
			stmt.executeUpdate();
			stmt.close();
			
			stmt = conn.prepareStatement("INSERT INTO " + tableRelate + " (" + relateSelf + "," + relateOther + ") VALUES (?,?)");
			
			for (Entity entity : values) {
				stmt.setInt(1, getID());
				stmt.setInt(2, entity.getID());
				stmt.addBatch();
			}
			stmt.executeBatch();
			
			stmt.close();
		} finally {
			closeConnectionImpl(conn);
		}
	}
	
	private String getManyRelationTable(Class<? extends Entity> typeOther) {
		String back = convertDowncaseName(convertSimpleClassName(type.getCanonicalName())) + "To" 
				+ convertSimpleClassName(typeOther.getCanonicalName());

		return back;
	}
	
	private String getManyRelationField(Class<? extends Entity> type) {
		return convertDowncaseName(convertSimpleClassName(type.getCanonicalName())) + "ID";
	}

	private <V> V convertValue(ResultSet res, String field, Class<V> type) throws SQLException {
		if (res.getString(field) == null) {
			return null;
		}

		if (type.equals(Integer.class) || type.equals(int.class)) {
			return (V) new Integer(res.getInt(field));
		} else if (type.equals(Long.class) || type.equals(long.class)) {
			return (V) new Long(res.getLong(field));
		} else if (type.equals(Short.class) || type.equals(short.class)) {
			return (V) new Short(res.getShort(field));
		} else if (type.equals(Float.class) || type.equals(float.class)) {
			return (V) new Float(res.getFloat(field));
		} else if (type.equals(Double.class) || type.equals(double.class)) {
			return (V) new Double(res.getDouble(field));
		} else if (type.equals(Byte.class) || type.equals(byte.class)) {
			return (V) new Byte(res.getByte(field));
		} else if (type.equals(String.class)) {
			return (V) res.getString(field);
		} else if (type.equals(Calendar.class)) {
			Calendar back = Calendar.getInstance();
			back.setTimeInMillis(res.getTimestamp(field).getTime());

			return (V) back;
		} else if (type.equals(Date.class)) {
			return (V) new Date(res.getTimestamp(field).getTime());
		} else if (type.equals(URL.class)) {
			return (V) res.getURL(field);
		} else if (type.equals(InputStream.class)) {
			return (V) res.getBlob(field).getBinaryStream();
		} else if (interfaceIneritsFrom(type, Entity.class)) {
			return (V) manager.getEntity(res.getInt(field), (Class<? extends Entity>) type);
		} else {
			throw new RuntimeException("Unrecognized type: " + type.toString());
		}
	}

	private void convertValue(PreparedStatement stmt, int index, Object value) throws SQLException {
		if (value instanceof Integer) {
			stmt.setInt(index, (Integer) value);
		} else if (value instanceof Long) {
			stmt.setLong(index, (Long) value);
		} else if (value instanceof Short) {
			stmt.setShort(index, (Short) value);
		} else if (value instanceof Float) {
			stmt.setFloat(index, (Float) value);
		} else if (value instanceof Double) {
			stmt.setDouble(index, (Double) value);
		} else if (value instanceof Byte) {
			stmt.setByte(index, (Byte) value);
		} else if (value instanceof String) {
			stmt.setString(index, (String) value);
		} else if (value instanceof URL) {
			stmt.setURL(index, (URL) value);
		} else if (value instanceof Calendar) {
			stmt.setTimestamp(index, new Timestamp(((Calendar) value).getTimeInMillis()));
		} else if (value instanceof Date) {
			stmt.setTimestamp(index, new Timestamp(((Date) value).getTime()));
		} else if (value instanceof Entity) {
			stmt.setInt(index, ((Entity) value).getID());
		} else {
			throw new RuntimeException("Unrecognized type: " + value.getClass().toString());
		}
	}
}
