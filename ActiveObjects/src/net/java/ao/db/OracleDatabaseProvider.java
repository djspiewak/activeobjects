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
package net.java.ao.db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.ao.Common;
import net.java.ao.DBParam;
import net.java.ao.DatabaseFunction;
import net.java.ao.DatabaseProvider;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.DatabaseType;
import net.java.ao.types.TypeManager;

/**
 * @author Daniel Spiewak
 */
public class OracleDatabaseProvider extends DatabaseProvider {

	public OracleDatabaseProvider(String uri, String username, String password) {
		super(uri, username, password);
	}

	@Override
	public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
		return (Class<? extends Driver>) Class.forName("oracle.jdbc.OracleDriver");
	}

	@Override
	public void setQueryResultSetProperties(ResultSet res, Query query) throws SQLException {
		if (query.getOffset() > 0) {
			res.absolute(query.getOffset() + 1);
		}
	}

	@Override
	public ResultSet getTables(Connection conn) throws SQLException {

		String schema = conn.getMetaData().getUserName();
        
        return conn.getMetaData().getTables(null, schema, "%", new String[] {"TABLE"});
		
	}

	@Override
	protected String convertTypeToString(DatabaseType<?> type) {
		switch (type.getType()) {
			case Types.BIGINT:
				return "NUMBER";

			case Types.BOOLEAN:
				return "NUMBER";

			case Types.INTEGER:
				return "NUMBER";

			case Types.NUMERIC:
				return "NUMBER";

			case Types.DECIMAL:
				return "NUMBER";

			case Types.SMALLINT:
				return "NUMBER";

			case Types.FLOAT:
				return "NUMBER";

			case Types.DOUBLE:
				return "NUMBER";

			case Types.REAL:
				return "NUMBER";
		}

		return super.convertTypeToString(type);
	}

	@Override
	protected String renderQueryLimit(Query query) {
		return "";
	}

	@Override
	protected String renderAutoIncrement() {
		return "";
	}

	@Override
	protected String renderOnUpdate(DDLField field) {
		return "";
	}

	@Override
	protected String renderFunction(DatabaseFunction func) {
		switch (func) {
			case CURRENT_TIMESTAMP:
				return "SYSDATE";

			case CURRENT_DATE:
				return "SYSDATE";
		}

		return super.renderFunction(func);
	}

	@Override
	protected String getDateFormat() {
		return "dd-MMM-yy hh:mm:ss.SSS a";
	}

	@Override
	protected String renderTriggerForField(DDLTable table, DDLField field) {
		if (field.getOnUpdate() != null) {
			StringBuilder back = new StringBuilder();
			String value = renderValue(field.getOnUpdate());

			back.append("CREATE TRIGGER ").append(table.getName()).append('_').append(field.getName()).append("_onupdate\n");
			back.append("BEFORE UPDATE\n").append("    ON ").append(table.getName()).append("\n    FOR EACH ROW\n");
			back.append("BEGIN\n");
			back.append("    :new.").append(field.getName()).append(" := ").append(value).append(";\nEND");

			return back.toString();
		}

		return super.renderTriggerForField(table, field);
	}

	@Override
	protected String renderAlterTableChangeColumnStatement(DDLTable table, DDLField oldField, DDLField field) {
		StringBuilder current = new StringBuilder();
		current.append("ALTER TABLE ").append(table.getName()).append(" MODIFY (");
		current.append(renderField(field)).append(')');
		return current.toString();
	}

	@Override
	protected String renderAlterTableDropKey(DDLForeignKey key) {
		StringBuilder back = new StringBuilder("ALTER TABLE ");

		back.append(key.getDomesticTable()).append(" DROP CONSTRAINT ").append(key.getFKName());

		return back.toString();
	}

    @Override
    protected String renderDropTable(DDLTable table) {
        return "DROP TABLE " + table.getName() + " PURGE";
    }
    
	@Override
	protected <T> T executeInsertReturningKey(Connection conn, Class<T> pkType, String pkField, String sql, DBParam... params) throws SQLException {
		T back = null;
		Logger.getLogger("net.java.ao").log(Level.INFO, sql);
		String[] generatedColumns = { pkField };		// TODO	doesn't account for non-autogenerated values

		PreparedStatement stmt = conn.prepareStatement(sql, generatedColumns);

		for (int i = 0; i < params.length; i++) {
			Object value = params[i].getValue();

			if (value instanceof RawEntity) {
				value = Common.getPrimaryKeyValue((RawEntity<?>) value);
			}

			if (params[i].getField().equalsIgnoreCase(pkField)) {
				back = (T) value;
			}

			stmt.setObject(i + 1, value);
		}

		stmt.executeUpdate();

		if (back == null) {
			ResultSet res = stmt.getGeneratedKeys();
			if (res.next()) {
				back = TypeManager.getInstance().getType(pkType).pullFromDatabase(null, res, pkType, 1);
				
			}
			res.close();
		}

		stmt.close();

		return back;
	}

	@Override
	protected String[] renderTriggers(DDLTable table) {
        List<String> back = new ArrayList<String>();
        
        for (DDLField field : table.getFields()) {
            String trigger = renderTriggerForField(table, field);
            if (trigger != null) {
                back.add(trigger);
            }
        }
        
        // TODO	only need trigger if PK is auto-incrementing
        // Create trigger for Primary key
        StringBuilder trg = new StringBuilder();

        trg.append("CREATE TRIGGER ").append(table.getName()).append("_TRG \n");
        trg.append("BEFORE INSERT\n").append("    ON ").append(table.getName()).append("   FOR EACH ROW\n");
        trg.append("BEGIN\n");
        trg.append(" SELECT ").append(table.getName()).append("_SEQ.NEXTVAL ");
        trg.append("INTO :NEW.ID FROM DUAL; \nEND;");
    
        back.add(trg.toString());
           
        
        return back.toArray(new String[back.size()]);

	}
	
	@Override
	protected String[] renderDropTriggers(DDLTable table) {
        List<String> back = new ArrayList<String>();        
        
        StringBuilder seq = new StringBuilder();

        // TODO	only need trigger if PK is auto-incrementing
        // add sequence
        seq.append("DROP TRIGGER ").append(table.getName()).append("_TRG ");
        
        back.add(seq.toString());
        
        return back.toArray(new String[back.size()]);      
    }
	
	@Override
	protected String[] renderDropSequences(DDLTable table) {
        List<String> back = new ArrayList<String>();        
        
        StringBuilder seq = new StringBuilder();

        // TODO	only need sequence if PK is auto-incrementing
        // add sequence
        seq.append("DROP SEQUENCE ").append(table.getName()).append("_SEQ ");
        
        back.add(seq.toString());
        
        return back.toArray(new String[back.size()]);      
	}
	
	@Override
    protected String[] renderSequences(DDLTable table) {
        List<String> back = new ArrayList<String>();        
        
        StringBuilder seq = new StringBuilder();
        
        // TODO	only need sequence if PK is auto-incrementing
        // add sequence
        seq.append("CREATE SEQUENCE ").append(table.getName()).append("_SEQ ").append("INCREMENT BY 1 START WITH 1 ");
        seq.append("MAXVALUE ").append(Integer.MAX_VALUE).append(" MINVALUE 1 "); 
        
        back.add(seq.toString());
	    
        return back.toArray(new String[back.size()]);      
	}
	
}