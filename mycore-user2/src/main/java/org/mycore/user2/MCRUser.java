/**
 * $Revision$ 
 * $Date$
 *
 * This file is part of the MILESS repository software.
 * Copyright (C) 2011 MILESS/MyCoRe developer team
 * See http://duepublico.uni-duisburg-essen.de/ and http://www.mycore.de/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package org.mycore.user2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.mycore.common.MCRException;
import org.mycore.common.MCRUserInformation;

/**
 * Represents a login user. Each user has a unique numerical ID.
 * Each user belongs to a realm. The user name must be unique within a realm.
 * Any changes made to an instance of this class does not persist automatically.
 * Use {@link MCRUserManager#updateUser(MCRUser)} to achieve this.
 * @author Frank L\u00fctzenkirchen
 * @author Thomas Scheffler (yagee)
 */
@XmlRootElement(name = "user")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "ownerId", "realName", "eMail", "lastLogin", "validUntil", "groups", "attributesMap", "password" })
public class MCRUser implements MCRUserInformation, Cloneable, Serializable {
    private static final long serialVersionUID = 3378645055646901800L;

    /** The unique user ID */
    int internalID;

    @XmlAttribute(name = "name")
    /** The login user name */
    private String userName;

    @XmlElement
    private Password password;

    /** The realm the user comes from */
    @XmlAttribute(name = "realm")
    private String realmID;

    /** The ID of the user that owns this user, or 0 */
    private MCRUser owner;

    /** The name of the person that this login user represents */
    @XmlElement
    private String realName;

    /** The E-Mail address of the person that this login user represents */
    @XmlElement
    private String eMail;

    /** The last time the user logged in */
    @XmlElement
    private Date lastLogin, validUntil;

    /**
     * 
     */
    private Map<String, String> attributes;

    private Collection<String> systemGroups;

    private Collection<String> externalGroups;

    protected MCRUser() {
        this(null);
    }

    /**
     * Creates a new user.
     * 
     * @param userName the login user name
     * @param realm the realm this user belongs to
     */
    public MCRUser(String userName, MCRRealm mcrRealm) {
        this(userName, mcrRealm.getID());
    }

    /**
     * Creates a new user.
     * 
     * @param userName the login user name
     * @param realmID the ID of the realm this user belongs to
     */
    public MCRUser(String userName, String realmID) {
        this.userName = userName;
        this.realmID = realmID;
        this.systemGroups = new HashSet<String>();
        this.externalGroups = new HashSet<String>();
        this.attributes = new HashMap<String, String>();
        this.password = new Password();
    }

    /**
     * Creates a new user in the default realm.
     * 
     * @param userName the login user name
     */
    public MCRUser(String userName) {
        this(userName, MCRRealmFactory.getLocalRealm().getID());
    }

    /**
     * Returns the login user name. The user name is
     * unique within its realm.
     *  
     * @return the login user name.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the login user name. The login user name
     * can be changed as long as it is unique within
     * its realm and the user ID is not changed.
     * 
     * @param userName the new login user name
     */
    void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Returns the realm the user belongs to.
     * 
     * @return the realm the user belongs to.
     */
    public MCRRealm getRealm() {
        return MCRRealmFactory.getRealm(realmID);
    }

    /**
     * Returns the ID of the realm the user belongs to.
     * 
     * @return the ID of the realm the user belongs to.
     */
    public String getRealmID() {
        return realmID;
    }

    /**
     * Sets the realm this user belongs to. 
     * The realm can be changed as long as the login user name
     * is unique within the new realm.
     * 
     * @param realmID the ID of the realm the user belongs to.
     */
    void setRealmID(String realmID) {
        setRealm(MCRRealmFactory.getRealm(realmID));
    }

    /**
     * Sets the realm this user belongs to. 
     * The realm can be changed as long as the login user name
     * is unique within the new realm.
     * 
     * @param realm the realm the user belongs to.
     */
    void setRealm(MCRRealm realm) {
        this.realmID = realm.getID();
    }

    /**
     * @return the hash
     */
    public String getPassword() {
        return password == null ? null : password.hash;
    }

    /**
     * @param hash the hash to set
     */
    public void setPassword(String password) {
        this.password.hash = password;
    }

    /**
     * @return the salt
     */
    public String getSalt() {
        return password == null ? null : password.salt;
    }

    /**
     * @param salt the salt to set
     */
    public void setSalt(String salt) {
        this.password.salt = salt;
    }

    /**
     * @return the hashType
     */
    public MCRPasswordHashType getHashType() {
        return password == null ? null : password.hashType;
    }

    /**
     * @param hashType the hashType to set
     */
    public void setHashType(MCRPasswordHashType hashType) {
        this.password.hashType = hashType;
    }

    /**
     * Returns the user that owns this user, or null 
     * if the user is independent and has no owner.
     *  
     * @return the user that owns this user.
     */
    public MCRUser getOwner() {
        return owner;
    }

    /**
     * Returns true if this user has no owner and therefore
     * is independent. Independent users may change their passwords 
     * etc., owned users may not, they are created to limit read access
     * in general.
     * 
     * @return true if this user has no owner
     */
    public boolean hasNoOwner() {
        return owner == null;
    }

    /**
     * Returns the name of the person this login user represents.
     * 
     * @return the name of the person this login user represents.
     */
    public String getRealName() {
        return realName;
    }

    /**
     * Returns the E-Mail address of the person this login user represents.
     * 
     * @return the E-Mail address of the person this login user represents.
     */
    public String getEMailAddress() {
        return eMail;
    }

    /**
     * Returns a hint the user has stored in case of forgotten hash.
     * 
     * @return a hint the user has stored in case of forgotten hash.
     */
    public String getHint() {
        return password == null ? null : password.hint;
    }

    /**
     * Returns the last time the user has logged in.
     * 
     * @return the last time the user has logged in.
     */
    public Date getLastLogin() {
        if (lastLogin == null) {
            return null;
        }
        return new Date(lastLogin.getTime());
    }

    /**
     * Sets the user that owns this user. 
     * Setting this to null makes the user independent.
     * 
     * @param ownerID the ID of the owner of the user.
     */
    public void setOwner(MCRUser owner) {
        this.owner = owner;
    }

    /**
     * Sets the name of the person this login user represents.
     * 
     * @param realName the name of the person this login user represents.
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }

    /**
     * Sets a hint to store in case of hash loss.
     * 
     * @param hint a hint for the user in case hash is forgotten.
     */
    public void setHint(String hint) {
        this.password.hint = hint;
    }

    /**
     * Sets the E-Mail address of the person this user represents.
     * 
     * @param eMail the E-Mail address
     */
    public void setEMail(String eMail) {
        this.eMail = eMail;
    }

    /**
     * Sets the time of last login to now.
     */
    public void setLastLogin() {
        this.lastLogin = new Date();
    }

    /**
     * Sets the time of last login.
     * 
     * @param lastLogin the last time the user logged in.
     */
    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin == null ? null : new Date(lastLogin.getTime());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MCRUser)) {
            return false;
        }
        MCRUser other = (MCRUser) obj;
        if (realmID == null) {
            if (other.realmID != null) {
                return false;
            }
        } else if (!realmID.equals(other.realmID)) {
            return false;
        }
        if (userName == null) {
            if (other.userName != null) {
                return false;
            }
        } else if (!userName.equals(other.userName)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((realmID == null) ? 0 : realmID.hashCode());
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    @Override
    public String getUserID() {
        String cuid = this.getUserName();
        if (!getRealm().equals(MCRRealmFactory.getLocalRealm()))
            cuid += "@" + getRealmID();

        return cuid;
    }

    /**
     * Returns additional user attributes.
     * This methods handles {@link MCRUserInformation#ATT_REAL_NAME} and
     * all attributes defined in {@link #getAttributes()}.
     */
    @Override
    public String getUserAttribute(String attribute) {
        if (MCRUserInformation.ATT_REAL_NAME.equals(attribute)) {
            return getRealName();
        }
        return getAttributes().get(attribute);
    }

    @Override
    public boolean isUserInRole(final String role) {
        boolean directMember = getSystemGroupIDs().contains(role) || getExternalGroupIDs().contains(role);
        if (directMember){
            return true;
        }
        return MCRGroupManager.isInGroup(this, role);
    }

    /**
     * @return the attributes
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * Returns a collection any system group ID this user is member of.
     * @see MCRGroup#isSystemGroup()
     */
    public Collection<String> getSystemGroupIDs() {
        return systemGroups;
    }

    /**
     * Returns a collection any external group ID this user is member of.
     * @see MCRGroup#isSystemGroup()
     */
    public Collection<String> getExternalGroupIDs() {
        return externalGroups;
    }

    /**
     * Adds this user to the given group.
     * @param groupName the group the user should be added to (must already exist)
     */
    public void addToGroup(String groupName) {
        MCRGroup mcrGroup = MCRGroupManager.getGroup(groupName);
        if (mcrGroup == null) {
            throw new MCRException("Could not find group " + groupName);
        }
        addToGroup(mcrGroup);
    }

    private void addToGroup(MCRGroup mcrGroup) {
        if (mcrGroup.isSystemGroup()) {
            getSystemGroupIDs().add(mcrGroup.getName());
        } else {
            getExternalGroupIDs().add(mcrGroup.getName());
        }
    }

    /**
     * Removes this user to the given group.
     * @param groupName the group the user should be removed from (must already exist)
     */
    public void removeFromGroup(String groupName) {
        MCRGroup mcrGroup = MCRGroupManager.getGroup(groupName);
        if (mcrGroup == null) {
            throw new MCRException("Could not find group " + groupName);
        }
        if (mcrGroup.isSystemGroup()) {
            getSystemGroupIDs().remove(mcrGroup.getName());
        } else {
            getExternalGroupIDs().remove(mcrGroup.getName());
        }
    }

    /**
     * Enable login for this user.
     */
    public void enableLogin() {
        setValidUntil(null);
    }

    /**
     * Disable login for this user.
     */
    public void disableLogin() {
        setValidUntil(new Date());
    }

    /**
     * Returns true if logins are allowed for this user.
     * @return
     */
    public boolean loginAllowed() {
        return validUntil == null || validUntil.after(new Date());
    }

    /**
     * Returns a {@link Date} when this user can not login anymore.
     */
    public Date getValidUntil() {
        if (validUntil == null) {
            return null;
        }
        return new Date(validUntil.getTime());
    }

    /**
     * Sets a {@link Date} when this user can not login anymore.
     * @param validUntil the validUntil to set
     */
    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil == null ? null : new Date(validUntil.getTime());
    }

    //This is code to get JAXB work

    private static class Password {
        @XmlAttribute
        /** The hash hash of the user, for users from local realm */
        private String hash;

        //base64 encoded
        @XmlAttribute
        private String salt;

        @XmlAttribute
        private MCRPasswordHashType hashType;

        /** A hint stored by the user in case hash is forgotten */
        @XmlAttribute
        private String hint;

    }

    private static class MapEntry {
        @XmlAttribute
        public String name;

        @XmlAttribute
        public String value;
    }

    private static class UserIdentifier {
        @XmlAttribute
        public String name;

        @XmlAttribute
        public String realm;
    }

    @SuppressWarnings("unused")
    @XmlElementWrapper(name = "groups")
    @XmlElement(name = "group")
    private MCRGroup[] getGroups() {
        if (getSystemGroupIDs().isEmpty() && getExternalGroupIDs().isEmpty()) {
            return null;
        }
        Collection<MCRGroup> groups = new ArrayList<MCRGroup>();
        for (String groupName : getSystemGroupIDs()) {
            groups.add(MCRGroupManager.getGroup(groupName));
        }
        for (String groupName : getExternalGroupIDs()) {
            groups.add(MCRGroupManager.getGroup(groupName));
        }
        return groups.toArray(new MCRGroup[groups.size()]);
    }

    @SuppressWarnings("unused")
    private void setGroups(MCRGroup[] groups) {
        for (MCRGroup group : groups) {
            addToGroup(group);
        }
    }

    @SuppressWarnings("unused")
    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "attribute")
    private MapEntry[] getAttributesMap() {
        if (attributes == null) {
            return null;
        }
        ArrayList<MapEntry> list = new ArrayList<MapEntry>(attributes.size());
        for (Entry<String, String> entry : attributes.entrySet()) {
            MapEntry mapEntry = new MapEntry();
            mapEntry.name = entry.getKey();
            mapEntry.value = entry.getValue();
            list.add(mapEntry);
        }
        return list.toArray(new MapEntry[list.size()]);
    }

    @SuppressWarnings("unused")
    private void setAttributesMap(MapEntry[] entries) {
        for (MapEntry entry : entries) {
            attributes.put(entry.name, entry.value);
        }
    }

    @SuppressWarnings("unused")
    @XmlElement(name = "owner")
    private UserIdentifier getOwnerId() {
        if (owner == null) {
            return null;
        }
        UserIdentifier userIdentifier = new UserIdentifier();
        userIdentifier.name = owner.getUserName();
        userIdentifier.realm = owner.getRealmID();
        return userIdentifier;
    }

    @SuppressWarnings("unused")
    private void setOwnerId(UserIdentifier userIdentifier) {
        if (userIdentifier.name.equals(this.userName) && userIdentifier.realm.equals(this.realmID)) {
            setOwner(this);
            return;
        }
        MCRUser owner = MCRUserManager.getUser(userIdentifier.name, userIdentifier.realm);
        setOwner(owner);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRUser clone() {
        MCRUser copy = getSafeCopy();
        if (copy.password == null) {
            copy.password = new Password();
        }
        copy.password.hashType = this.password.hashType;
        copy.password.hash = this.password.hash;
        copy.password.salt = this.password.salt;
        return copy;
    }

    /**
     * Returns this MCRUser with basic information.
     * Same as {@link #getSafeCopy()} but without these informations:
     * <ul>
     * <li>real name
     * <li>eMail
     * <li>attributes
     * <li>group information
     * <li>last login
     * <li>valid until
     * <li>password hint
     * </ul>
     * @return a clone copy of this instance
     */
    public MCRUser getBasicCopy() {
        MCRUser copy = new MCRUser(userName, realmID);
        copy.owner = this.equals(this.owner) ? copy : this.owner;
        copy.setAttributes(null);
        copy.password = null;
        return copy;
    }

    /**
     * Returns this MCRUser with safe information.
     * Same as {@link #clone()} but without these informations:
     * <ul>
     * <li>password hash type
     * <li>password hash value
     * <li>password salt
     * </ul>
     * @return a clone copy of this instance
     */
    public MCRUser getSafeCopy() {
        MCRUser copy = getBasicCopy();
        if (getHint() != null) {
            copy.password = new Password();
            copy.password.hint = getHint();
        }
        copy.setAttributes(new HashMap<String, String>());
        copy.eMail = this.eMail;
        copy.lastLogin = this.lastLogin;
        copy.validUntil = this.validUntil;
        copy.realName = this.realName;
        copy.systemGroups.addAll(this.systemGroups);
        copy.externalGroups.addAll(this.externalGroups);
        copy.attributes.putAll(this.attributes);
        return copy;
    }
}
