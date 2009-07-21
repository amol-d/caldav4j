/*
 * Copyright 2005 Open Source Applications Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osaf.caldav4j;

import java.io.InputStream;

import junit.framework.TestCase;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.webdav.lib.methods.DeleteMethod;
import org.apache.webdav.lib.util.WebdavStatus;
import org.junit.Before;
import org.osaf.caldav4j.methods.CalDAV4JMethodFactory;
import org.osaf.caldav4j.methods.HttpClient;
import org.osaf.caldav4j.methods.MkCalendarMethod;
import org.osaf.caldav4j.methods.PutMethod;
import org.osaf.caldav4j.util.XMLUtils;
import org.osaf.caldav4j.xml.OutputsDOMBase;
import org.w3c.dom.Document;

public abstract class BaseTestCase  extends TestCase implements TestConstants {
    protected static final Log log = LogFactory.getLog(BaseTestCase.class);
    protected HttpClient http;
    protected HttpClient httpClient;

    protected HostConfiguration hostConfig;
    protected CaldavCredential caldavCredential = new CaldavCredential();
    public  String COLLECTION_PATH;


    @Before
    protected void setUp() throws Exception {
    	super.setUp();
        COLLECTION_PATH = caldavCredential.CALDAV_SERVER_WEBDAV_ROOT
        + caldavCredential.COLLECTION;
        hostConfig = createHostConfiguration();
        http  = createHttpClient();
        httpClient = createHttpClient();
        methodFactory = new CalDAV4JMethodFactory();
    	
    }
    

    protected CalDAV4JMethodFactory methodFactory = new CalDAV4JMethodFactory();
    
    public String getCalDAVServerHost() {
        return caldavCredential.CALDAV_SERVER_HOST;
    }
    
    public int getCalDAVServerPort(){
        return caldavCredential.CALDAV_SERVER_PORT;
    }
    
    public String getCalDavSeverProtocol(){
        return caldavCredential.CALDAV_SERVER_PROTOCOL;
    }
    
    public String getCalDavSeverWebDAVRoot(){
        return caldavCredential.CALDAV_SERVER_WEBDAV_ROOT;
    }
    
    public String getCalDavSeverUsername(){
        return caldavCredential.CALDAV_SERVER_USERNAME;
    }
    
    public String getCalDavSeverPassword(){
        return caldavCredential.CALDAV_SERVER_PASSWORD;
    }
        

    public BaseTestCase(String method) {
    	super(method);
	}
    public BaseTestCase() {
	}

	public HttpClient createHttpClient(){
        HttpClient http = new HttpClient();

        Credentials credentials = new UsernamePasswordCredentials(caldavCredential.CALDAV_SERVER_USERNAME, 
        		caldavCredential.CALDAV_SERVER_PASSWORD);
        http.getState().setCredentials(
        		new AuthScope(this.getCalDAVServerHost(), this.getCalDAVServerPort()),
        		credentials);
        http.getParams().setAuthenticationPreemptive(true);
        return http;
    }
	public static HttpClient createHttpClient(CaldavCredential caldavCredential){
        HttpClient http = new HttpClient();

        Credentials credentials = new UsernamePasswordCredentials(caldavCredential.CALDAV_SERVER_USERNAME, 
        		caldavCredential.CALDAV_SERVER_PASSWORD);
        http.getState().setCredentials(
        		new AuthScope(caldavCredential.CALDAV_SERVER_HOST, caldavCredential.CALDAV_SERVER_PORT),
        		credentials);
        http.getParams().setAuthenticationPreemptive(true);
        return http;
    }
    public static HttpClient createHttpClientWithNoCredentials(){

        HttpClient http = new HttpClient();
        http.getParams().setAuthenticationPreemptive(true);
        return http;
    }
    public HostConfiguration createHostConfiguration(){
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(getCalDAVServerHost(), getCalDAVServerPort(), getCalDavSeverProtocol());
        return hostConfig;
    }
    public static HostConfiguration createHostConfiguration(CaldavCredential caldavCredential){
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(caldavCredential.CALDAV_SERVER_HOST,caldavCredential.CALDAV_SERVER_PORT, caldavCredential.CALDAV_SERVER_PROTOCOL);
        return hostConfig;
    }
    
    // TODO testme
    public static Calendar getCalendarResource(String resourceName) {
        Calendar cal;

        InputStream stream = BaseTestCase.class.getClassLoader()
                .getResourceAsStream(resourceName);
        CalendarBuilder cb = new CalendarBuilder();
        
        try {
            cal = cb.build(stream);
        } catch (Exception e) {        	
            throw new RuntimeException("Problems opening file:" + resourceName + "\n" + e);
        }
        
        return cal;
    }    
    
    /***
     * FIXME this put updates automatically the timestamp of the event 
     * @param resourceFileName
     * @param path
     */
    protected void put(String resourceFileName, String path) {    	
        PutMethod put = methodFactory.createPutMethod();
        InputStream stream = this.getClass().getClassLoader()
        .getResourceAsStream(resourceFileName);
        String event = parseISToString(stream);
        event = event.replaceAll("DTSTAMP:.*", "DTSTAMP:" + new DateTime(true).toString());
        log.debug(new DateTime(true).toString());
        //log.trace(event);        
        
        put.setRequestEntity(event);
        put.setPath(path);
    	log.debug("\nPUT " + put.getPath());
        try {
            http.executeMethod(hostConfig, put);
            
            int statusCode =  put.getStatusCode();
            
            switch (statusCode) {
			case WebdavStatus.SC_CREATED:
				
				break;
			case WebdavStatus.SC_NO_CONTENT:
				break;
			case WebdavStatus.SC_PRECONDITION_FAILED:
				log.error("item exists?");
				break;
			case WebdavStatus.SC_CONFLICT:
				log.error("conflict: item still on server" + put.getResponseBodyAsString());
				break;
			default:
                log.error(put.getResponseBodyAsString());
				throw new Exception("trouble executing PUT of " +resourceFileName + "\nresponse:" + put.getResponseBodyAsString());

			}
        } catch (Exception e){
        	log.info("Error while put():" + e.getMessage());
            throw new RuntimeException(e);
        }

    }
    
    protected void del(String path){
        DeleteMethod delete = new DeleteMethod();
        delete.setPath(path.replaceAll("/+", "/"));
        try {
        	http.executeMethod(hostConfig, delete);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    
    protected void mkcalendar(String path){
        MkCalendarMethod mk = new MkCalendarMethod();
        mk.setPath(path);
        mk.addDescription(CALENDAR_DESCRIPTION, "en");
        try {
        	http.executeMethod(hostConfig, mk);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    
    public String parseISToString(java.io.InputStream is){
        java.io.DataInputStream din = new java.io.DataInputStream(is);
        StringBuffer sb = new StringBuffer();
        try{
          String line = null;
          while((line=din.readLine()) != null){
            sb.append(line+"\n");
          }
        }catch(Exception ex){
          ex.getMessage();
        }finally{
          try{
            is.close();
          }catch(Exception ex){}
        }
        return sb.toString();
      }



}
