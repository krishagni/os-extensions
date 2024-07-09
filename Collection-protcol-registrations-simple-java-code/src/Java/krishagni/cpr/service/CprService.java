package Java.krishagni.cpr.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import Java.krishagni.cpr.client.ApiClient;
import Java.krishagni.cpr.payload.CprDetails;

public class CprService extends ApiClient {

	private static StringBuffer response = new StringBuffer();
	private static String POST_PARAMS=CprDetails.getPaylaod();
	private static String readLine=null;

	public static void createCpr(String token, String URI)throws IOException {
		System.out.println(URI);
		urlConnection.setRequestMethod("POST");
		urlConnection.setRequestProperty("Content-Type", "application/json");
		urlConnection.setDoOutput(true);
		OutputStream outputStream = urlConnection.getOutputStream();
		outputStream.write(POST_PARAMS.getBytes());
		int responseCode = urlConnection.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			httpResponseReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

			while ((readLine = httpResponseReader.readLine()) != null) {
				response.append(readLine);
			}
			System.out.println("\nNew Registered Participant is:" + "\n" + response);
		} else {
			System.err.println("\n Unable to create new registration on the Server "+ response + "Check os.log file");
		}
	}
}