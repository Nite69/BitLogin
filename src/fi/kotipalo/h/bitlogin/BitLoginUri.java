/*
 * Copyright 2012-2013 the original author or authors.
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
 */

package fi.kotipalo.h.bitlogin;

import java.util.HashMap;
import java.util.Map;

public class BitLoginUri {
	private String scheme;
	private String address;
	private Map<String,String> params;
	
	public BitLoginUri(String origin) {
		if (origin == null) return;
		String[] uri = origin.split(":",2);
		setScheme(uri[0]);
		if (uri.length > 1) {
			String[] method = uri[1].split("\\?",2);
			setAddress(method [0]);
			if (method.length > 1) {
				String[] sParams = method[1].split("&");
				Map<String,String> parameters = new HashMap<String,String>(sParams.length);
				for (String param:sParams) {
					String[] keyvalue = param.split("=",2);
					String value = "";
					if (keyvalue.length > 1) 
						value = keyvalue[1];
					parameters.put(keyvalue[0], value);
				}
				params = parameters;
			}
		}
	}

	public String getScheme() {
		return scheme;
	}

	private void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public String getAddress() {
		return address;
	}

	private void setAddress(String address) {
		this.address = address;
	}

	public Map<String,String> getParams() {
		return params;
	}

	private void setParams(Map<String,String> params) {
		this.params = params;
	}
}
