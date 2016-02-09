package com.krishagni.openspecimen.spr.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.openspecimen.spr.events.LabHeader;
import com.krishagni.openspecimen.spr.events.LabResult;
import com.krishagni.openspecimen.spr.util.ClinicalApiConnectionException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class ClinicalApiConnection {
	
	public LabHeader[] GetLabs(String mrn, String facility, Date loDate, Date hiDate, 
			String[] categories, String[] testCodes) {
			String url = "clinical/patients/mrn/" 
				+ mrn + "/labs";
		
		MultivaluedMapImpl params = new MultivaluedMapImpl();

		if(categories!=null&&categories.length>0) 
			params.add("category", buildCategoriesParam(categories));
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		params.add("dateFrom",dateFormat.format(loDate));
		params.add("dateTo",dateFormat.format(hiDate));
		params.add("facility", facility);
		params.add("max", "200");
		
		String resStr = "";
		
		resStr = doGet(url, params);
		
		LabHeader[] labs = getGson().fromJson(resStr, LabHeader[].class);
		
		//If the list of test codes is null or empty, return all test codes.  Otherwise, 
		// filter the results.
		if(testCodes!=null&&testCodes.length>0) labs = filterTestCodes(testCodes, labs);

		return labs;
	}
	
	public LabResult GetLabResult(String mrn, String pathId) {
		String url = "clinical/patients/mrn/"
				+ mrn + "/labs/" 
				+ new String(Base64.encodeBase64(pathId.getBytes()));
		
		String resStr = doGet(url, null);
		//Everything comes back in arrays
		return getGson().fromJson(resStr, LabResult[].class)[0];
	}
	
	public String GetTextReport(String mrn, String pathId) {		
		return GetLabResult(mrn, pathId).getComponents()[0].getResult();
	}
	
	private String doGet(String url, MultivaluedMapImpl params) {
		String baseUrl = ConfigUtil.getInstance().getStrSetting(JHU_MODULE, "base_url", "https://api.jh.edu:443/internal/v1/");
		String httpsProtocol = ConfigUtil.getInstance().getStrSetting(JHU_MODULE, "https_protocol", "TLSv1");
		String auth = ConfigUtil.getInstance().getStrSetting(JHU_MODULE, "authentication_key", "");
		String clientId = ConfigUtil.getInstance().getStrSetting(JHU_MODULE, "client_id", "");
		String clientSecret = ConfigUtil.getInstance().getStrSetting(JHU_MODULE, "client_secret", "");
		System.setProperty("https.protocols", httpsProtocol); 
		WebResource webResource = getClient().resource(baseUrl + url);
		
		ClientResponse response = null;

		if(params!=null) webResource = webResource.queryParams(params);
		response = webResource.header("Content-Type","application/json")
				.header("Authorization",auth)
				.header("client_id",clientId)
				.header("client_secret",clientSecret)
				.get(ClientResponse.class);	
		
		if(response.getStatus()!=200) 
			throw new ClinicalApiConnectionException(response.getStatus() + ": " + response.getClientResponseStatus().getReasonPhrase());

		return response.getEntity(String.class);
	}
	
	private String buildCategoriesParam(String[] categories) {
		StringBuilder catBuilder = new StringBuilder();
		for(int i = 0; i<categories.length; i++) {
			catBuilder.append(categories[i] + ",");
		}
		int catLength = catBuilder.length();
		return catBuilder.toString().substring(0, catLength-1);
	}
	
	private LabHeader[] filterTestCodes(String[] testCodes, LabHeader[] allLabs) {
		List<String> testCodesList = Arrays.asList(testCodes);
		ArrayList<LabHeader> reqLabs = new ArrayList<LabHeader>();
		
		for(LabHeader lab : allLabs) {
			if(testCodesList.contains(lab.getTestCode())) reqLabs.add(lab);
		}	
		return reqLabs.toArray(new LabHeader[reqLabs.size()]);
	}
	
	private Client getClient() {
		return Client.create();
	}
	
	private Gson getGson() {
		return new GsonBuilder().create();
	}
	
	private static final String JHU_MODULE = "plugin_jhu";
}
