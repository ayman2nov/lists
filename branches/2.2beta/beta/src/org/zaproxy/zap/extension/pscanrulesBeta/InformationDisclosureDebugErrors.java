/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.pscanrulesBeta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.htmlparser.jericho.Source;

import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.network.HttpBody;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.extension.pscan.PassiveScanThread;
import org.zaproxy.zap.extension.pscan.PluginPassiveScanner;

public class InformationDisclosureDebugErrors extends PluginPassiveScanner {

	private PassiveScanThread parent = null;
	private static final String debugErrorFile = "xml/debug-error-messages.txt";
	private static final Logger logger = Logger.getLogger(InformationDisclosureDebugErrors.class);
	private List<String> errors = null;
	
	@Override
	public void scanHttpRequestSend(HttpMessage msg, int id) {
		
	}

	@Override
	public void scanHttpResponseReceive(HttpMessage msg, int id, Source source) {
		if (msg.getResponseBody().length() > 0 && msg.getResponseHeader().isText()) {
			String parameter;
			if ((parameter = doesResponseContainsDebugErrorMessage(msg.getResponseBody())) != null) {
				this.raiseAlert(msg, id, parameter);
			}
		}
	}

	private void raiseAlert(HttpMessage msg, int id, String infoDisclosureDBError) {
		Alert alert = new Alert(getId(), Alert.RISK_LOW, Alert.WARNING, 
		    	getName());
		    	alert.setDetail(
		    			"The response request appeared to contain common error messages returned by platforms such as ASP.NET, and Web-servers such as IIS and Apache. You can configure the list of common debug messages", 
		    	    msg.getRequestHeader().getURI().toString(),
		    	    "",
		    	    "", 
		    	    "",
		    	    "Disable debugging messages before pushing to production", 
		            "", 
					infoDisclosureDBError,	// Evidence
					0,	// TODO CWE Id
		            13,	// WASC Id - Info leakage
		            msg);
	
    	parent.raiseAlert(id, alert);
	}
	
	private String doesResponseContainsDebugErrorMessage (HttpBody body) {
		if (this.errors == null) {
			this.errors = loadFile(debugErrorFile);
		}
		String sBody = body.toString().toLowerCase();
		for (String error : this.errors) {
			int start = sBody.indexOf(error);
			if (start >= 0) {
				// Return the original (case exact) string so we can match it in the response
				return body.toString().substring(start, start + error.length());
			}
		}
		return null;
	}
	
	private List<String> loadFile(String file) {
		List<String> strings = new ArrayList<String>();
		BufferedReader reader = null;
		File f = new File(Constant.getZapHome() + File.separator + file);
		if (! f.exists()) {
			logger.error("No such file: " + f.getAbsolutePath());
			return strings;
		}
		try {
			String line;
			reader = new BufferedReader(new FileReader(f));
			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("#")) {
					strings.add(line.trim().toLowerCase());
				}
			}
		} catch (IOException e) {
			logger.debug("Error on opening/reading debug error file. Error: " + e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();			
				}
				catch (IOException e) {
					logger.debug("Error on closing the file reader. Error: " + e.getMessage(), e);
				}
			}
		}
		return strings;
	}

	@Override
	public void setParent(PassiveScanThread parent) {
		this.parent = parent;
	}

	@Override
	public String getName() {
		return "Information disclosure - debug error messages";
	}
	
	private int getId() {
		return 10023;
	}
}
