package li.doerf.hacked.db.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import li.doerf.hacked.db.DatasetChangeListener;
import li.doerf.hacked.db.annotations.Column;
import li.doerf.hacked.db.annotations.Table;


abstract class TableBase  {
    private String tableName;
    private Map<String,Field> columnNamesAndFields;
    private final String LOGTAG = this.getClass().getSimpleName();

    public abstract Long getId();
    public abstract void setId(Long anId);
    protected abstract TableBase getReference(SQLiteDatabase db, String aReferenceName, Long anId);

    protected String getTableName() {
        if (tableName == null) {
            Table annotationTable = getClass().getAnnotation(Table.class);
            if (annotationTable == null) {
                throw new IllegalArgumentException("Missing table annotation");
            }
            tableName = annotationTable.name();
        }
        return tableName;
    }

    public void createTable(SQLiteDatabase db) {
        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        sql.append(getTableName());
        sql.append(" (");
        boolean firstColumn = true;

        for (Map.Entry<String,Field> entry : getColumnNamesWithFields().entrySet()) {
            Field field = entry.getValue();

            Column fieldEntityAnnotation = getColumn(field);
            if (fieldEntityAnnotation != null) {
                if (! firstColumn) {
                    sql.append(", ");
                }

                sql.append(fieldEntityAnnotation.name());
                sql.append(" ");

                if (fieldEntityAnnotation.isReference())
                {
                    sql.append("INTEGER");
                }
                else {
                    sql.append(fieldEntityAnnotation.type());
                }

                if (fieldEntityAnnotation.isPrimaryKey())
                    sql.append(" PRIMARY KEY");

                if (fieldEntityAnnotation.isAutoincrement())
                    sql.append(" AUTOINCREMENT");
            }

            firstColumn = false;
        }
        sql.append(");");
        String query = sql.toString();
        Log.d(LOGTAG, "SQL query: " + query);
        db.execSQL(query);
    }

    public void dropTable(SQLiteDatabase db) {
        String query = String.format("DROP TABLE %s", getTableName());
        Log.d(LOGTAG, "SQL query: " + query);
        db.execSQL(query);
    }

    /**
     * @return an array with the names of all sqlite db columns
     */
    protected String[] getColumnNames() {
        Map<String, Field> columnNamesWithFields = getColumnNamesWithFields();
        return columnNamesWithFields.keySet().toArray(new String[columnNamesWithFields.size()]);
    }

    /**
     * @return a map with the column names as keys and fields as values
     */
    protected Map<String,Field> getColumnNamesWithFields() {
        if (columnNamesAndFields == null) {
            Map<String, Field> result = new TreeMap<>();
            for (Field field : getClass().getDeclaredFields()) {
                String columnName = getColumnName(field);
                if (columnName == null) {
                    continue;
                }

                result.put(columnName, field);
            }
         columnNamesAndFields = result;
        }
        return columnNamesAndFields;
    }

    protected String getPrimaryKeyColumnName() {
        Map<String, Field> columns = getColumnNamesWithFields();
        for (Map.Entry<String,Field> e : columns.entrySet()) {
            Column c = getColumn(e.getValue());
            if (c.isPrimaryKey()) {
                return e.getKey();
            }
        }

        throw new IllegalStateException("Table does not have primary key");
    }

    private Column getColumn(Field aField) {
        return aField.getAnnotation(Column.class);
    }

    private boolean isPrimaryKey(Field aField) {
        Column fieldEntityAnnotation = getColumn(aField);
        return fieldEntityAnnotation.isPrimaryKey();
    }

    private boolean isAutoIncrement(Field aField) {
        Column fieldEntityAnnotation = getColumn(aField);
        return fieldEntityAnnotation.isAutoincrement();
    }

    private boolean isColumn(Field aField)
    {
        Column fieldEntityAnnotation = getColumn(aField);
        return fieldEntityAnnotation != null;
    }

    private String getColumnName(Field aField)
    {
        Column fieldEntityAnnotation = getColumn(aField);
        if (fieldEntityAnnotation == null) {
            return null;
        }
        return fieldEntityAnnotation.name();
    }

    // Object -> DB
    private ContentValues getFilledContentValues(boolean withPk) throws IllegalAccessException {
        ContentValues contentValues = new ContentValues();
        for (Field field : getClass().getDeclaredFields()) {
            if (!isColumn(field)) continue; // not a DB field
            if (isAutoIncrement(field)) continue; // managed by DB
            if (!withPk && isPrimaryKey(field)) continue;
            putInContentValues(contentValues, field, this);
        }
        return contentValues;
    }

    private void putInContentValues(ContentValues contentValues, Field field,
                                    Object object) throws IllegalAccessException {
        if (!field.isAccessible())
            field.setAccessible(true); // for private variables
        Object fieldValue = field.get(object);

        if (fieldValue == null) {
            Log.v(LOGTAG, "fieldValue null. ignoring: " + field);
            return;
        }

        String key = getColumnName(field);

        if (fieldValue instanceof String) {
            contentValues.put(key, (String) fieldValue);
        } else if (fieldValue instanceof Short) {
            contentValues.put(key, (Short) fieldValue);
        } else if (fieldValue instanceof Long) {
            contentValues.put(key, (Long) fieldValue);
        } else if (fieldValue instanceof Integer) {
            contentValues.put(key, (Integer) fieldValue);
        } else if (fieldValue instanceof Float) {
            contentValues.put(key, (Float) fieldValue);
        } else if (fieldValue instanceof Double) {
            contentValues.put(key, (Double) fieldValue);
        } else if (fieldValue instanceof Boolean) {
            contentValues.put(key, ((Boolean) fieldValue == true) ? 1 : 0);
        } else {
            Column column = getColumn(field);
            if (column.isReference()) {
                contentValues.put(key, ((TableBase) fieldValue).getId());
            } else {
                // Byte, Byte[] and Boolean are currently not supported
                throw new IllegalArgumentException("unsupported type: " + field);
            }
        }
    }

    // DB -> Object
    protected void fillFromCursor(SQLiteDatabase db, Cursor aCursor, Map<String, Field> aColumnNamesAndFields) {
        try {
            for (Map.Entry<String, Field> e : aColumnNamesAndFields.entrySet()) {
                String columnName = e.getKey();
                Field field = e.getValue();
                field.setAccessible(true);
                Class<?> type = field.getType();

                if (String.class.isAssignableFrom(type)) {
                    String value = aCursor.getString(aCursor.getColumnIndex(columnName));
                    field.set(this, value);
                } else if (Short.class.isAssignableFrom(type)) {
                    Short value = aCursor.getShort(aCursor.getColumnIndex(columnName));
                    field.set(this, value);
                } else if (Integer.class.isAssignableFrom(type)) {
                    Integer value = aCursor.getInt(aCursor.getColumnIndex(columnName));
                    field.set(this, value);
                } else if (Long.class.isAssignableFrom(type)) {
                    Long value = aCursor.getLong(aCursor.getColumnIndex(columnName));
                    field.set(this, value);
                } else if (Float.class.isAssignableFrom(type)) {
                    Float value = aCursor.getFloat(aCursor.getColumnIndex(columnName));
                    field.set(this, value);
                } else if (Double.class.isAssignableFrom(type)) {
                    Double value = aCursor.getDouble(aCursor.getColumnIndex(columnName));
                    field.set(this, value);
                } else if (Boolean.class.isAssignableFrom(type)) {
                    Integer value = aCursor.getInt(aCursor.getColumnIndex(columnName));
                    field.set(this, value == 1);
                } else {
                    Column column = getColumn(field);
                    if (column.isReference() && db != null) {
                        Long id = aCursor.getLong(aCursor.getColumnIndex(columnName));
                        TableBase value = getReference(db, columnName, id);
                        field.set(this, value);
                    } else {
                        throw new IllegalArgumentException("unsupported type: " + field);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            Log.e(LOGTAG, "caught IllegalAccessException during building object", e);
        }
    }

    public void insert(SQLiteDatabase db) {
        if (getId() != null) {
            throw new IllegalStateException("id: "+ getId() + " is already set. This object cannot be inserted");
        }

        try {
            ContentValues values = getFilledContentValues(true);
            long id = db.insert(
                    getTableName(),
                    null,
                    values);
            setId(id);
            Log.i(LOGTAG, "Inserted entity - id: " + id);
            notifyObservers( getTableName());
        } catch (IllegalAccessException e) {
            Log.e(LOGTAG, "caught IllegalAccessException during insert", e);
        }
    }

    public void update(SQLiteDatabase db) {
        if (getId() == null) {
            throw new IllegalStateException("id not set. This object cannot be updated");
        }

        try {
            ContentValues values = getFilledContentValues(false);
            String idAsString = Long.toString(getId());
            db.update(
                    getTableName(),
                    values,
                    getPrimaryKeyColumnName() + " = ?",
                    new String[] { idAsString });
            Log.i(LOGTAG, "Updated entity - id: " + idAsString);
            notifyObservers( getTableName());
        } catch (IllegalAccessException e) {
            Log.e(LOGTAG, "caught IllegalAccessException during update", e);
        }
    }

    public void delete(SQLiteDatabase db) {
        if (getId() == null) {
            throw new IllegalStateException("id not set. This object cannot be deleted");
        }

        String idAsString = Long.toString(getId());
        db.delete(
                getTableName(),
                getPrimaryKeyColumnName() + " = ?",
                new String[] { idAsString });
        Log.i(LOGTAG, "Deleted entity - id: " + idAsString);
        notifyObservers( getTableName());
    }

    private static Map<String,List<DatasetChangeListener>> myDatasetChangedListeners;
    static {
        myDatasetChangedListeners = new HashMap<>();
    }

    public synchronized static <T extends TableBase> void registerDatasetChangedListener(DatasetChangeListener anObserver, Class<T> aTableClass) {
        String tablename;

        try {
            tablename = aTableClass.newInstance().getTableName();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        List<DatasetChangeListener> observerList = myDatasetChangedListeners.get(tablename);
        if ( observerList == null ) {
            observerList = new ArrayList<>();
            myDatasetChangedListeners.put(tablename, observerList);
        }
        observerList.add( anObserver);
    }

    public synchronized static <T extends TableBase> void unregisterDatasetChangedListener(DatasetChangeListener anObserver, Class<T> aTableClass) {
        String tablename;

        try {
            tablename = aTableClass.newInstance().getTableName();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        List<DatasetChangeListener> observerList = myDatasetChangedListeners.get(tablename);
        if ( observerList == null ) {
            return;
        }
        observerList.remove( anObserver);
    }

    private void notifyObservers(String tableName) {
        List<DatasetChangeListener> observerList = myDatasetChangedListeners.get(tableName);
        if ( observerList == null ) {
            return;
        }

        for ( DatasetChangeListener o : observerList ) {
            o.datasetChanged();
        }
    }
}
