package Java.krishagni.cpr.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import Java.krishagni.cpr.service.CprService;

public class ApiClient {

	private static final String usernamePassword = "admin:Login@123";
	private static final String API_URL = "http://localhost:8080/openspecimen/rest/ng/collection-protocol-registrations/";
	public static URL serverUrl=null;
	public static HttpURLConnection urlConnection = null;
	public static BufferedReader httpResponseReader = null;
	private static String basicUrlAuthentication = null;

	public static void main(String args[]) throws IOException {

		String token = login();
		CprService.createCpr(token,API_URL);
		logout();
	}

	private static String login()throws IOException {
		serverUrl = new URL(API_URL);
		urlConnection = (HttpURLConnection) serverUrl.openConnection();
		basicUrlAuthentication = "Basic "+ Base64.getEncoder().encodeToString(usernamePassword.getBytes());
		urlConnection.addRequestProperty("Authorization", basicUrlAuthentication);
		return basicUrlAuthentication;
	} 

	private static void logout() throws IOException {
		urlConnection.disconnect();
		basicUrlAuthentication=null;
	}

}
