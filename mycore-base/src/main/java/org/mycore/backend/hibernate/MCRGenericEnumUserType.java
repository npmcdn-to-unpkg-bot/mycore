/*
 * $Id$
 * $Revision: 5697 $ $Date: 06.02.2012 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.hibernate;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.TypeResolver;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRGenericEnumUserType implements UserType, ParameterizedType {
    private static final String DEFAULT_IDENTIFIER_METHOD_NAME = "name";

    private static final String DEFAULT_VALUE_OF_METHOD_NAME = "valueOf";

    @SuppressWarnings("rawtypes")
    private Class<? extends Enum> enumClass;

    private Method identifierMethod;

    private Method valueOfMethod;

    @SuppressWarnings("rawtypes")
    private AbstractSingleColumnStandardBasicType type;

    private int[] sqlTypes;

    @SuppressWarnings("rawtypes")
    public void setParameterValues(Properties parameters) {
        String enumClassName = parameters.getProperty("enumClass");
        if (enumClassName == null) {
            throw new HibernateException("Parameter \"enumClass\" is undefined.");
        }
        try {
            Class<?> clazz = Class.forName(enumClassName);
            enumClass = clazz.asSubclass(Enum.class);
        } catch (ClassNotFoundException cfne) {
            throw new HibernateException("Enum class not found", cfne);
        }

        String identifierMethodName = parameters.getProperty("identifierMethod", DEFAULT_IDENTIFIER_METHOD_NAME);

        Class<?> identifierType;
        try {
            identifierMethod = enumClass.getMethod(identifierMethodName, new Class[0]);
            identifierType = identifierMethod.getReturnType();
        } catch (Exception e) {
            throw new HibernateException("Failed to obtain identifier method", e);
        }

        TypeResolver tr = new TypeResolver();
        type = (AbstractSingleColumnStandardBasicType) tr.basic(identifierType.getName());
        if (type == null) {
            throw new HibernateException("Unsupported identifier type " + identifierType.getName());
        }
        sqlTypes = new int[] { type.sqlType() };

        String valueOfMethodName = parameters.getProperty("valueOfMethod", DEFAULT_VALUE_OF_METHOD_NAME);

        try {
            valueOfMethod = enumClass.getMethod(valueOfMethodName, new Class[] {identifierType});
        } catch (Exception e) {
            throw new HibernateException("Failed to obtain valueOf method", e);
        }
    }

    @SuppressWarnings("rawtypes")
    public Class<? extends Enum> returnedClass() {
        return enumClass;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
        Object identifier = type.get(rs, names[0]);
        if (rs.wasNull()) {
            return null;
        }

        try {
            return valueOfMethod.invoke(enumClass, new Object[] { identifier });
        } catch (Exception e) {
            throw new HibernateException("Exception while invoking valueOf method '" + valueOfMethod.getName() + "' of " + "enumeration class '" + enumClass
                + "'", e);
        }
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
        try {
            if (value == null) {
                st.setNull(index, type.sqlType());
            } else {
                Object identifier = identifierMethod.invoke(value, new Object[0]);
                type.set(st, identifier, index);
            }
        } catch (Exception e) {
            throw new HibernateException("Exception while invoking identifierMethod '" + identifierMethod.getName() + "' of " + "enumeration class '"
                + enumClass + "'", e);
        }
    }

    public int[] sqlTypes() {
        return sqlTypes;
    }

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

}
