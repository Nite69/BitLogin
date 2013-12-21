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

import java.util.Map;

import android.content.Context;

public class MessageHandler {
	private static MessageHandler mh;
	private static BitIdentityPool identities = new BitIdentityPool();
	private static Context appContext;
	public MessageHandler(Context c) {
		appContext = c;
		identities.setContext(c);
		identities.reload();
		mh = this;
	};

	public static MessageHandler getInstance() {
		return mh;
	}
	// Expecting:
	// [bitid:][serveraddress]?[id={func~}params]([&pcol=http(s)])([&sig(nature)=param])
	// generally params=sessionkey~pubkey~uname
	// func is 'login' or 'create' or 'delete'. Shortcuts 'l','c','d'
	//
	// Example login:
	// sessionkey~pubkey (and possibly pcol=http(s) and &signature)
	// bitid:www.site.example/bid?id=login~1234567890~13zQ3gA93iWriYqTLBfENKtuLA1iaMwZVV
	// Reply: sessionkey~uname&signature=sig
	// Reply: sessionkey&signature=sig
	// response: http://www.site.example/bid?id=login~1234567890~maijameikalainen&signature=kldsjflretreoitvneuwhnljh3874824
	// response: http://www.site.example/bid?id=login~1234567890&signature=kldsjflretreoitvneuwhnljh3874824

	// Example create: (uname can be set either on server or client. server preferred because mobile client)
	// sessionkey~pubkey~uname
	// sessionkey~pubkey
	// bitid:www.site.example/bid?id=create~1234567890~maijameikalainen~13zQ3gA93iWriYqTLBfENKtuLA1iaMwZVV
	// sessionkey(~uname)(~pubkey)&signature=signature
	// response: http://www.site.example/bid?id=create~1234567890~maijameikalainen~idcWKght34NdUXpMyPtUQqhcu488LcXnv6U&signature=kldsjflretreoitvneuwhnljh3874824
	// response: http://www.site.example/bid?id=create~1234567890&signature=kldsjflretreoitvneuwhnljh3874824

	// Example delete:
	// sessionkey~pubkey~uname
	// bitid:www.site.example/bid?id=delete~1234567890~maijameikalainen~13zQ3gA93iWriYqTLBfENKtuLA1iaMwZVV
	// sessionkey~uname&signature=signature
	// response: http://www.site.example/bid?id=delete~1234567890~maijameikalainen&signature=kldsjflretreoitvneuwhnljh3874824

	
	protected static String handlebitidUri(BitLoginUri uri) {
		if (uri == null) return "Error uri == null";
		try
		{
 		 String[] scheme = uri.getScheme().split("\\.",2);
		 if ("bitid".equalsIgnoreCase(scheme[0])) 
		 {
			String function=null;
			if (scheme.length>1) function = scheme[1];
			String addr = uri.getAddress();
			Map<String,String> params = uri.getParams();
			String protocol = params.get("pcol");
			if (protocol==null) protocol="http";
			String sessionKey = null;
			String serverPubKey = null;
			String uname = null;
			String id=params.get("id");
			if (id != null) {
				String[] ps = id.split("~");
				int lastParam = ps.length-1;
				int paramIndex = 0;
				// function is mandartory
				function = ps[paramIndex++].toLowerCase();
				if (function == null) {
					function = "error";
				}
				// sessionkey is mandartory
				sessionKey = ps[paramIndex++];
				// uname is optional
				if (lastParam > paramIndex) {
					uname = ps[paramIndex++];
				}
				// serverPubKey is allways the last
				serverPubKey = ps[lastParam];
			} else {
				sessionKey = params.get("skey");
				serverPubKey = params.get("pubkey");
				if (function == null) {
					function = params.get("f");
				}
			}
			if (function.startsWith("l")) {
				BitIdentity identity = identities.getBitIdentityForSite(serverPubKey);
				if (identity != null) {
					if (uname == null) uname = identity.getUname(); 
					//String message = "skey="+sessionKey+"&uname="+uname;
					String message = "l~"+sessionKey;//+"~"+uname;
					String signature = identity.generateSignaTure(message);
					Boolean isValidSign = identity.verifyMessage(message, signature);
					return protocol + "://"+addr+"?"+"id="+message+"&signature="+signature+"&valid="+isValidSign.toString();
				}
				return "Errror: identity for " + serverPubKey + " not found";
			} else if (function.startsWith("c")) {
				if (uname == null) uname = params.get("uname");
				if (uname == null) {
					// should ask user for wanted username
					uname = "dummy";
				}
				BitIdentity identity = identities.getBitIdentity(serverPubKey, uname);
				if (identity == null) {
					identity = new BitIdentity(serverPubKey, uname);
					identities.addAndSaveIdentity(identity);
				}
				//String myPubkey = identity.getAddress();
				//String message = "skey="+sessionKey+"&uname="+uname+"&pubkey="+myPubkey;
				String message = "c~"+sessionKey;//+"~"+uname+"~"+myPubkey;
				String signature = identity.generateSignaTure(message);
				Boolean isValidSign = identity.verifyMessage(message, signature);
				return protocol + "://"+addr+"?"+"id="+message+"&signature="+signature+"&valid="+isValidSign.toString();
			} else if (function.startsWith("d")) {
				BitIdentity identity = identities.getBitIdentityForSite(serverPubKey);
				if (identity != null) {
					if (uname == null) uname = identity.getUname(); 
					//String message = "skey="+sessionKey+"&uname="+uname;
					String message = "d~"+sessionKey;//+"~"+uname;
					String signature = identity.generateSignaTure(message);
					Boolean isValidSign = identity.verifyMessage(/*"X"+*/message, signature);
					identity.deleted();
					return protocol + "://"+addr+"?"+"id="+message+"&signature="+signature+"&valid="+isValidSign.toString();
				}
				return "Error: identity for " + serverPubKey + " not found";
			}
			return "error: unknown function";
		 }
 		 return "error: not a bitid code:"+scheme[0]+"|"+scheme[1];
		} catch (Exception e) {
			return "MH Exception: "+e.getMessage();
		}
	}
}

// OLD PROTOCOL VERSIONS:
// sessionkey~pubkey (and possibly pcol=http(s) and &signature)
// bitid.login:www.site.example/login?id=1234567890~13zQ3gA93iWriYqTLBfENKtuLA1iaMwZVV
// bitid.login:www.site.example/login?pcol=https&id=1234567890~idcWKght34NdUXpMyPtUQqhcu488LcXnv6S
// bitid:www.site.example/bid?id=login~1234567890~13zQ3gA93iWriYqTLBfENKtuLA1iaMwZVV
// sessionkey(~uname)&signature=
// response: http://www.site.example/login?id=1234567890~maijameikalainen&signature=kldsjflretreoitvneuwhnljh3874824
// response: http://www.site.example/bid?id=login~1234567890~maijameikalainen&signature=kldsjflretreoitvneuwhnljh3874824

// sessionkey~uname~pubkey
// bitid.create:www.site.example/create?id=1234567890~13zQ3gA93iWriYqTLBfENKtuLA1iaMwZVV~maijameikalainen
// bitid:www.site.example/bid?id=create~1234567890~maijameikalainen~13zQ3gA93iWriYqTLBfENKtuLA1iaMwZVV
// sessionkey~uname(~pubkey)&signature=signature
// response: http://www.site.example/create?id=1234567890~maijameikalainen~idcWKght34NdUXpMyPtUQqhcu488LcXnv6U&signature=kldsjflretreoitvneuwhnljh3874824
// response: http://www.site.example/bid?id=create~1234567890~maijameikalainen~idcWKght34NdUXpMyPtUQqhcu488LcXnv6U&signature=kldsjflretreoitvneuwhnljh3874824

// sessionkey~pubkey
// bitid.delete:www.site.example/del?id=1234567890~13zQ3gA93iWriYqTLBfENKtuLA1iaMwZVV
// bitid:www.site.example/bid?id=delete~1234567890~13zQ3gA93iWriYqTLBfENKtuLA1iaMwZVV
// response: http://www.site.example/del?skey=1234567890&uname=maijameikalainen&signaure=kldsjflretreoitvneuwhnljh3874824
// sessionkey~uname&signature=signature
// response: http://www.site.example/del?id=1234567890~maijameikalainen&signature=kldsjflretreoitvneuwhnljh3874824
// response: http://www.site.example/bid?id=delete~1234567890~maijameikalainen&signature=kldsjflretreoitvneuwhnljh3874824

