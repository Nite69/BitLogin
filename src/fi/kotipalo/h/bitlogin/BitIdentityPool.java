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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Environment;

public class BitIdentityPool {
	private Map<String,List <BitIdentity>> identities = new HashMap<String,List <BitIdentity>>();
	private static Context appContext;

	public BitIdentityPool() {
	}

	public void setContext(Context c) {
		appContext = c;
	}
	
	public void reload() {
		// debug:
		//BitIdentity identity = new BitIdentity("serverPubKey", "uname");
		//addIdentity(identity);
		//TODO: this jams :-(
		// read all from internal storage
		// does not yet work
		if (appContext == null) {
			// WHAT CAN I DO?!
		} else {
			String filename = "identityes.dat";
			{
			    File path = appContext.getFilesDir();
			    File file = new File(path,filename);
			    BufferedReader fr;
				try {
					fr = new BufferedReader(new FileReader(file));
					String nextLine;
					List<String> lines = new ArrayList<String>();
					do {
						nextLine=fr.readLine(); // = fr.readLine().trim() -> null pointer!!!
						if (nextLine != null) nextLine = nextLine.trim();
						if ((nextLine != null) && (!nextLine.startsWith("["))) {
							lines.add(nextLine);
						} else {
							BitIdentity id = new BitIdentity(lines);
							if (id.getServerPubKey() != null) {
								addIdentity(id);
							}
							lines.clear();
							if (nextLine != null)
								lines.add(nextLine);
						}
					} while (nextLine != null);
			        //fr.write(identity.getSerialized());
			        fr.close();			    
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public BitIdentity getBitIdentityForSite(String serverPubKey) {
		// later there can be several, get them from identity handler
		// for debugging, just generate one
		if (identities.containsKey(serverPubKey)) {
			List <BitIdentity> serverIdentities = identities.get(serverPubKey);
			// later ask user to select identity, now get just the latest
			if (serverIdentities.size()>0)
				return serverIdentities.get(serverIdentities.size()-1); 
		}
		return null;
	}

	public BitIdentity getBitIdentity(String serverPubKey, String uname) {
		if (uname == null) return null;
		if (identities.containsKey(serverPubKey)) {
			List <BitIdentity> serverIdentities = identities.get(serverPubKey);
			for (BitIdentity candidate : serverIdentities) {
				if (uname.equalsIgnoreCase(candidate.getUname())) return candidate;
			}
		}
		return null;
	}	

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}	
	
	public void addIdentity(BitIdentity identity) {
		if (getBitIdentity(identity.getServerPubKey(), identity.getUname()) != null)
			return;
		List <BitIdentity> serverIdentities; 
		String serverPubKey = identity.getServerPubKey();
		if (identities.containsKey(serverPubKey))
			serverIdentities = identities.get(serverPubKey);
		else {
			serverIdentities = new ArrayList<BitIdentity>();
			identities.put(serverPubKey,serverIdentities);
		}
		serverIdentities.add(identity);
	}
	
	public void addAndSaveIdentity(BitIdentity identity) {
		if (getBitIdentity(identity.getServerPubKey(), identity.getUname()) != null)
			return;
		addIdentity(identity);
		String filename = "identityes.dat";
		/*if (isExternalStorageWritable()) 
		{
		    //File path = Environment.getExternalStoragePublicDirectory(
		    //        Environment.DIRECTORY_DOWNLOADS);
	        File root = Environment.getExternalStorageDirectory();
	        File dir = new File(root.getAbsolutePath() + "/bitid");
	        dir.mkdirs();
	        File file = new File(dir,filename);
	        FileWriter fw;
			try {
				fw = new FileWriter(file,true);
		        fw.write(identity.getSerialized());
		        fw.close();			    
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		if (appContext == null) {
			// WHAT CAN I DO?!
		} else {
			{
			    File path = appContext.getFilesDir();
			    File file = new File(path,filename);
			    path.mkdirs();
		        FileWriter fw;
				try {
					fw = new FileWriter(file,true);
			        fw.write(identity.getSerialized());
			        fw.close();			    
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
