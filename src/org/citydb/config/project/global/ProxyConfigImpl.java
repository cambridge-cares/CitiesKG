/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * (C) 2013 - 2015,
 * Chair of Geoinformatics,
 * Technische Universitaet Muenchen, Germany
 * http://www.gis.bgu.tum.de/
 * 
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 * 
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Muenchen <http://www.moss.de/>
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 */
package org.citydb.config.project.global;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.citydb.api.io.ProxyConfig;
import org.citydb.api.io.ProxyType;
import org.citydb.config.language.Language;

@XmlType(name="ProxyConfigType", propOrder={
		"type",
		"host",
		"port",
		"username",
		"password",
		"savePassword"
})
public class ProxyConfigImpl implements ProxyConfig {
	@XmlAttribute(required=true)
	private Boolean isEnabled = false;
	@XmlAttribute(required=true)
	private Boolean requiresAuthentication;
	@XmlAttribute(required=true)
	private ProxyType type = ProxyType.HTTP;
	private String host = "";
	private Integer port = 0;
	private String username = "";
	private String password = "";
	private Boolean savePassword = false;
	@XmlTransient
	private String internalPassword = "";
	@XmlTransient
	private int failedConnectAttempts = 0;
	@XmlTransient
	private ProxyConfigImpl other = null;

	public ProxyConfigImpl() {		
	}
	
	public ProxyConfigImpl(ProxyType type, ProxyConfigImpl other) {
		this();
		this.type = type;
		isEnabled = other.isEnabled;
		requiresAuthentication = other.requiresAuthentication;
		host = other.host;
		port = other.port;
		username = other.username;
		password = other.password;
		savePassword = other.savePassword;
		internalPassword = other.internalPassword;
		failedConnectAttempts = other.failedConnectAttempts;
		this.other = other;
	}

	public ProxyConfigImpl(ProxyType type) {
		this.type = type;
		
		switch (type) {
		case HTTP:
			port = 80;
			break;
		case HTTPS:
			port = 443;
			break;
		case SOCKS:
			port = 1080;
			break;
		}
	}

	@Override
	public boolean isEnabled() {
		if (isEnabled != null)
			return isEnabled.booleanValue();

		return false;
	}

	public void setEnabled(boolean enable) {
		this.isEnabled = enable;
	}

	@Override
	public boolean requiresAuthentication() {
		if (requiresAuthentication != null)
			return requiresAuthentication.booleanValue();

		return false;
	}

	public void setRequiresAuthentication(boolean requiresAuthentication) {
		this.requiresAuthentication = requiresAuthentication;
	}

	@Override
	public ProxyType getType() {
		return type;
	}

	public void setType(ProxyType type) {
		this.type = type;
	}

	@Override
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		if (host != null)
			this.host = host;
	}

	@Override
	public int getPort() {
		if (port != null)
			return port.intValue();

		return 0;
	}

	public void setPort(int port) {
		if (port > 0)
			this.port = port;
	}

	@Override
	public String getUsername() {
		return username;
	}

	public void setUsername(String user) {
		if (user != null)
			this.username = user;
	}

	@Override
	public String getPassword() {
		return internalPassword.length() > 0 ? internalPassword : password;
	}

	public String getExternalPassword() {
		return password;
	}

	public void setExternalPassword(String password) {
		if (password != null)
			this.password = password;
	}

	public boolean isSavePassword() {
		if (savePassword != null)
			return savePassword.booleanValue();

		return false;
	}

	public void setSavePassword(boolean savePassword) {
		this.savePassword = savePassword;
	}

	public String getInternalPassword() {
		return internalPassword;
	}

	public void setInternalPassword(String internalPassword) {
		if (internalPassword != null)
			this.internalPassword = internalPassword;
	}

	public int failed() {
		return ++failedConnectAttempts;
	}
	
	public void resetFailedConnectAttempts() {
		failedConnectAttempts = 0;
	}

	public boolean hasValidProxySettings() {
		return host.length() > 0 && port > 0;
	}

	public boolean hasValidUserCredentials() {
		return username.length() > 0 && internalPassword.length() > 0;	
	}
	
	public boolean isCopy() {
		return other != null;
	}
	
	public ProxyConfigImpl getCopiedFrom() {
		return other;
	}

	@Override
	public Proxy toProxy() {
		if (hasValidProxySettings()) {
			switch (type) {
			case HTTP:
			case HTTPS:
				return new Proxy(Type.HTTP, new InetSocketAddress(host, port));
			case SOCKS:
				return new Proxy(Type.SOCKS, new InetSocketAddress(host, port));
			}
		}

		return null;
	}

	@Override
	public String toString() {
		switch (type) {
		case HTTP:
			return Language.I18N.getString("pref.proxy.label.http");
		case HTTPS:
			return Language.I18N.getString("pref.proxy.label.https");
		case SOCKS:
			return Language.I18N.getString("pref.proxy.label.socks");
		default:
			return "n/a";
		}
	}
}
