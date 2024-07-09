package Java.krishagni.cpr.payload;

public class CprDetails {
	
	final static String POST_PAYLOAD=		
			"{\n"+
			          "        \"participant\": {\n                                  "+
			          "        \"source\": \"OpenSpecimen\",\r\n                     "+
			          "        \"firstName\": \"Sanju\",\r\n                         "+
			          "        \"lastName\": \"Samson\",\r\n                         "+
			          "        \"middleName\": \"Ramesh\",\r\n                       "+
			          "        \"emailAddress\": \"sanjay@gmail.com\",\r\n           "+
			          "        \"gender\": \"Male\",\r\n                             "+
			          "        \"races\": [\n                                        "+
			          "            \"White\",\r\n                                    "+
			          "            \"Asian\"\r\n                                     "+ 
			          "          ],\r\n                                              "+
			          "        \"pmis\": [\n                                         "+
			          "        {\n													 "+
			          "           \"siteName\": \"Kaustubh_Site_1\",\r\n			 "+
			          "           \"mrn\": \"2r3456789\"                             "+
			          "          \n},\r\n											 "+
			          "          {\n                                                 "+
			          "            \"siteName\": \"Biobank_site\",\r\n               "+
			          "            \"mrn\": \"2r345678991\"                          "+
			          "        \n}                                                   "+
			          "        \n],\r\n                                              "+
			          "     \"ethnicities\": [\n                                     "+
			          "          \"Unknown\",\r\n                                    "+
			          "          \"Not Reported\"                                    "+
			          "        \n],\r\n                                              "+
			          "     \"uid\": \"776-87-3333\",\r\n                            "+
			          "     \"activityStatus\": \"Active\",\r\n                      "+
			          "     \"empi\": \"2r3457652\",\r\n                             "+
			          "     \"phiAccess\": true,\r\n                                 "+
			          "     \"extensionDetail\": {\n                                 "+
			          "        \"formId\": 97,\r\n                                   "+
			          "        \"formCaption\": \"ParticipantCustomFields\",\r\n     "+
			          "        \"attrs\": [\n                                        "+
			          "              {\n											 "+
			          "                 \"name\": \"comments\",\r\n                  "+
			          "                 \"udn\": \"comments\",\r\n                   "+
			          "                 \"caption\": \"comments\",\r\n               "+
			          "                 \"value\": \"Pure Veg\"                      "+
			          "              \n},\r                                          "+
			          "             {\n                                              "+
			          "                \"name\": \"diet\",\r\n                       "+
			          "                \"udn\": \"diet\",\r\n                        "+
			          "                \"caption\": \"diet\",\r\n                    "+
			          "                \"value\": \"Non Veg\"                        "+
			          "             \n}                                              "+
			          "          \n],\r\n                                            "+
			          "         \"useUdn\": false                                    "+
			          "     \n}                                                      "+
			          " \n},\r\n                                                     "+
			          " \"cpId\": 1,\r\n                                             "+
			          " \"cpTitle\": \"Blood samples collection protocol\",\r\n      "+
			          " \"cpShortTitle\": \"blood sample cp\",\r\n                   "+
			          " \"ppid\": \"bloodsample00123\",\r\n                          "+
			          " \"barcode\": null,\r\n                                       "+
			          " \"activityStatus\": \"Active\",\r\n                          "+
			          " \"registrationDate\": \"2021-03-16\",\r\n                    "+
			          " \"externalSubjectId\": \"010\",\r\n                          "+
			          " \"site\": \"Biobank_site\",\r\n                              "+
			          " \"consentDetails\": null,\r\n                                "+
			          " \"specimenLabelFmt\": null,\r\n                              "+
			          " \"aliquotLabelFmt\": \"%PSPEC_LABEL%_%PSPEC_UID%\",\r\n      "+
			          " \"derivativeLabelFmt\": null,\r\n                            "+
			          " \"forceDelete\": false                                       "+
			         "\n}";
	
	public static String getPaylaod() {	
		return POST_PAYLOAD;
	}
}
