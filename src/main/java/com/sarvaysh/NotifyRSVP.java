package com.sarvaysh;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NotifyRSVP {
	private static String TOKEN = "";
	private static String KEY = "";
	private LinkedList<EventIds> queue = new LinkedList<EventIds>();
	private JsonFactory jsonfactory = new JsonFactory();
	private ObjectMapper mapper = new ObjectMapper();
	
	
	public NotifyRSVP(String token, String key) {
		TOKEN = token;
		KEY = key;
	    jsonfactory.setCodec(mapper);
	}

	private String restGetCall(String url) {
		System.out.print(".");
		try {
			Thread.sleep(250);
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestProperty("Authorization", "Bearer " + TOKEN);
			con.setRequestProperty("Accept", "application/json");
			int responseCode = con.getResponseCode();
			String response = "";
			if(responseCode == 200) {
				InputStream in = new BufferedInputStream(con.getInputStream());
				Scanner scanner = new Scanner(in);
				response = scanner.useDelimiter("\\Z").next();
				scanner.close();
			}else {
				System.out.println(obj.toString() + " returned response code is " + responseCode);
			}
			return response;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	private String restPostCall(String url, String param ) {
		String charset = "UTF-8"; 
		URL obj;
		try {
			obj = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
			connection.setDoOutput(true); // Triggers POST.
			connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestProperty("Content-Type", "application/json;charset=" + charset);
			connection.setRequestProperty("Authorization", "Bearer " + TOKEN);

			//try (OutputStream output = connection.getOutputStream()) {
			  //output.write(param.getBytes(charset));
			//}
			OutputStream output = connection.getOutputStream();
			output.write(param.getBytes(charset));
			
			String response = "";
			int responseCode = connection.getResponseCode();
			if(responseCode == HttpURLConnection.HTTP_CREATED) {
				InputStream in = new BufferedInputStream(connection.getInputStream());
				Scanner scanner = new Scanner(in);
				response = scanner.useDelimiter("\\Z").next();
				scanner.close();
			}else {
				System.out.println(obj.toString() + " returned response code is " + responseCode);
			}
			return response;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	private List<String> getListFromJsonArray(String response) {
		List<String> values = new ArrayList<String>();
		try {
			JsonParser jp = jsonfactory.createJsonParser(response);
			JsonToken current;

			current = jp.nextToken();
		    if (current != JsonToken.START_ARRAY) {
		      System.out.println("Error: root should be array: quiting.");
		      return values;
		    }
		    while (jp.nextToken() != JsonToken.END_ARRAY) {
		    	JsonNode node = jp.readValueAsTree();
		    	values.add(node.get("id").textValue());    
		    }
		   
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return values;
	}
	
	private List<JsonNode> parseJsonArray(String response) {
		List<JsonNode> values = new ArrayList<JsonNode>();

		try {
			JsonParser jp = jsonfactory.createJsonParser(response);
			JsonToken current;

			current = jp.nextToken();
		    if (current != JsonToken.START_ARRAY) {
		      System.out.println("Error: root should be array: quiting.");
		      return null;
		    }
		    while (jp.nextToken() != JsonToken.END_ARRAY) {
		    	JsonNode node = jp.readValueAsTree();
		    	values.add(node);
		    }
		   
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return values;
	}
	
	private JsonNode parseJson(String response) {
		try {
			JsonParser jp = jsonfactory.createJsonParser(response);
			JsonToken current;

			current = jp.nextToken();
		    if (current != JsonToken.START_OBJECT) {
		      System.out.println("Error: root should be object: quiting.");
		      return null;
		    }
		    while (jp.nextToken() != JsonToken.END_OBJECT) {
		    	JsonNode node = jp.readValueAsTree();
		        return node;
		    }		    
		    return null;
		} catch (JsonParseException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean getEvents() {
		//https://api.constantcontact.com/v2/eventspot/events
		final String url = "https://api.constantcontact.com/v2/eventspot/events?api_key=" + KEY;
		String response = restGetCall(url);
		JsonNode node = parseJson(response);
		if(node != null) {
	        Iterator<JsonNode> it = node.get("results").iterator();
	        while(it.hasNext()) {
	        	JsonNode n = it.next();
	        	if(!"ACTIVE".contentEquals(n.get("status").textValue())) {
	            	  continue;
	            }
	            queue.add(new EventIds(n.get("id").textValue(), null, null, null, null, null));
	        }
		}
		return true;
	}
	
	public boolean getEventFees() {
		//https://api.constantcontact.com/v2/eventspot/events/{eventId}/fees
		EventIds eventIds;
		while((eventIds = queue.peek()) != null && eventIds.getFees() == null) {
			eventIds = queue.pop();
			final String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventIds.getEventId() + "/fees?api_key=" + KEY;
			String response = restGetCall(url);
			eventIds.setFees(getListFromJsonArray(response));
			queue.add(eventIds);
		}
		return true;
	}
	
	public boolean getPromoCodes() {
		//https://api.constantcontact.com/v2/eventspot/events/{event_id}/promocodes
		EventIds eventIds;
		while((eventIds = queue.peek()) != null && eventIds.getPromoCodes() == null) {
			eventIds = queue.pop();
			final String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventIds.getEventId() + "/promocodes?api_key=" + KEY;
			String response = restGetCall(url);
			eventIds.setPromoCodes(getListFromJsonArray(response));
			queue.add(eventIds);
		}
		return true;
	}


	public boolean getRegistrants() {
		EventIds eventIds;
		while((eventIds = queue.peek()) != null && eventIds.getRegistrants() == null) {
			eventIds = queue.pop();
			List<String> registrants = new ArrayList<String>();
			//https://api.constantcontact.com/v2/eventspot/events/{eventId}/registrants
			final String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventIds.getEventId() + "/registrants?limit=500&api_key=" + KEY;
			String response = restGetCall(url);
			JsonNode node = parseJson(response);
			if(node != null) {
		        Iterator<JsonNode> it = node.get("results").iterator();
		        while(it.hasNext()) {
		        	JsonNode n = it.next();
		        	if(!"REGISTERED".contentEquals(n.get("registration_status").textValue())) {
		            	  continue;
		            }
		        	registrants.add(n.get("id").textValue());
		        }
		        eventIds.setRegistrants(registrants);
	            queue.add(eventIds);
			}
		}
		return true;
	}
	
	public boolean getEventItems() {
		//https://api.constantcontact.com/v2/eventspot/events/{eventId}/items
		EventIds eventIds;
		while((eventIds = queue.peek()) != null && eventIds.getItems() == null) {
			eventIds = queue.pop();
			final String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventIds.getEventId() + "/items?api_key=" + KEY;
			String response = restGetCall(url);
			eventIds.setItems(getListFromJsonArray(response));
		    queue.add(eventIds);
		}
		return true;
	}
	
	public boolean getItemAttributes() {
		//https://api.constantcontact.com/v2/eventspot/events/{eventId}/items/{itemId}/attributes
		EventIds eventIds;
		List<String> itemAttributes = new ArrayList<String>();
		while((eventIds = queue.peek()) != null && eventIds.getItemAttributes() == null) {
			eventIds = queue.pop();
			List<String> items = eventIds.getItems();
			for(String itemId : items) {
				final String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventIds.getEventId() + "/items/" + itemId + "/attributes?api_key=" + KEY;
				String response = restGetCall(url);
				itemAttributes.addAll(getListFromJsonArray(response));
			}
			eventIds.setItemAttributes(itemAttributes);
		    queue.add(eventIds);
		}
		return true;
	}
	
	public String getEventFee(String eventId, String feeId, String message) {
		//https://api.constantcontact.com/v2/eventspot/events/{eventId}/fees/{feeId}
		final String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventId + "/fees/" + feeId + "?api_key=" + KEY;
		String response = restGetCall(url);	
		JsonNode node = parseJson(response);
		if(node != null) {
	        message = message + node.get("label").textValue() + "(" + node.get("fee").textValue()  + ")" ;
		}
		return message;
	}
	public String getPromoCode(String eventId, String promoCodeId, String message) {
		//https://api.constantcontact.com/v2/eventspot/events/{eventId}/promocodes/{promocodeId}
		final String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventId + "/promocodes/" + promoCodeId + "?api_key=" + KEY;
		String response = restGetCall(url);	
		JsonNode node = parseJson(response);
		if(node != null) {
	        message = message + node.get("code_name").textValue() + " Used:" + node.get("quantity_used").textValue();
		}
		return message;
	}
	public String getEventItem(String eventId, String itemId, String message) {
		//https://api.constantcontact.com/v2/eventspot/events/{eventId}/items/{itemId}
		final String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventId + "/items/" + itemId + "?api_key=" + KEY;
		String response = restGetCall(url);	
		JsonNode node = parseJson(response);
		if(node != null) {
	        message = message + node.get("code_name").textValue() + " Used:" + node.get("quantity_used").textValue();
		}
		return message;
	}
	
	public String updatetMessageWithEventDetails(String eventId, String message) {
		//https://api.constantcontact.com/v2/eventspot/events/{event_id}
		final String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventId +"?api_key=" + KEY;
		String response = restGetCall(url);	
		JsonNode node = parseJson(response);
		if(node != null) {
			String title = node.get("title").textValue();
			message = message.replace("{{title}}",title + "\n");
		}
        return message;
	}
	
	public String updateMessageWithPromocodesDetails(String eventId, List<String> promoCodes, String message) {
		String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventId +"/promocodes/";
		String promocodesDetails = "";
		for(String promoCode: promoCodes) {
			url = url + promoCode + "?api_key=" + KEY;
			String response = restGetCall(url);	
			JsonNode node = parseJson(response);
			if(node != null) {
				promocodesDetails = node.get("code_name").textValue() + " Total:" + node.get("quantity_total").textValue();
				promocodesDetails = " Used: " + node.get("quantity_used").textValue() + " Available:" + node.get("quantity_available").textValue();	
				promocodesDetails = "\n";
			}
		}
		message = message.replace("{{promocodes}}",promocodesDetails);
        return message;
	}
	
	public String updateMessageWithItemssDetails(String eventId, List<String> items, String message) {
		String itemsDetails = "";
		for(String item: items) {
			String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventId +"/items/";
			url = url + item + "?api_key=" + KEY;
			String response = restGetCall(url);	
			JsonNode node = parseJson(response);
			if(node != null) {
				itemsDetails = itemsDetails + node.get("name").textValue();
				Iterator<JsonNode> it = node.get("attributes").iterator();
				while(it.hasNext()) {
					JsonNode n = it.next();
					itemsDetails = itemsDetails + "\n    " + n.get("name").textValue() + " Total:" + n.get("quantity_total").asText() + " Available:" + n.get("quantity_available").asText(); 
					
				}
				itemsDetails = itemsDetails + "\n";
			} 
		}
		message = message.replace("{{items}}",itemsDetails);
        return message;
	}
	
	public String updateMessageWithRegistrants(String eventId, List<String> registrants, String message) {
		String registrantsMessage = "";
		registrantsMessage = registrantsMessage + "\nTotal number of completed registrations: " + registrants.size();
		
		HashMap<String, Integer> items = new HashMap<String, Integer>();
		
		for(String registrantId : registrants) {
			String url = "https://api.constantcontact.com/v2/eventspot/events/" + eventId +"/registrants/";
			url = url + registrantId + "?api_key=" + KEY;
			String response = restGetCall(url);	
			JsonNode node = parseJson(response);
			Iterator<JsonNode> it = node.get("payment_summary").get("order").get("items").iterator();
			while(it.hasNext()) {
				JsonNode n = it.next();
				String itemName = n.get("name").textValue();
				if(items.containsKey(itemName)) {
					items.put(itemName, items.get(itemName) + n.get("quantity").asInt());
				} else {
					items.put(itemName, n.get("quantity").asInt());
				}
			}
		}
		
		Set<String> itemsSet = items.keySet();
		ArrayList sortedItems = new ArrayList<String>(itemsSet);
		Collections.sort(sortedItems);
		Collections.reverse(sortedItems);

		
		Iterator<String> itemsIterator = sortedItems.iterator();
		while(itemsIterator.hasNext()) {
			String item = itemsIterator.next();
			Integer value = items.get(item);
			registrantsMessage = registrantsMessage + "\n    " + item + ":" + value;
		}
		
		message = message.replace("{{registrants}}",registrantsMessage);
		
		return message;
	}
	
	public String getCommitteeContactListId() {
		final String url = "https://api.constantcontact.com/v2/lists?api_key=" + KEY;
		String listId = "";
		String response = restGetCall(url);	
		List<JsonNode> nodes = parseJsonArray(response);
		if(nodes != null) {
			Iterator<JsonNode> it = nodes.iterator();
			while(it.hasNext()) {
				JsonNode n = it.next();
				if(n.get("name").textValue().equalsIgnoreCase("committee")) {
					listId = n.get("id").asText();
					return listId;
				}
			}
		}
		return listId;
	}
		
	public void sendEmail(String message) {
		String listId = getCommitteeContactListId();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd HH:mm:ss");
		String date = sdf.format(new Date());
		String title = "Automated RSVP notification " + date;
		final String accountInfoURL = "https://api.constantcontact.com/v2/account/info?api_key=" + KEY;
		final String createCampaignURL = "https://api.constantcontact.com/v2/emailmarketing/campaigns?api_key=" + KEY;
		String response = restGetCall(accountInfoURL);	
		JsonNode node = parseJson(response);
		String org_name = node.get("organization_name").textValue();
		String email = node.get("email").textValue();
		String countryCode = node.get("country_code").textValue();
		String stateCode = node.get("state_code").textValue();
		Iterator<JsonNode> it = node.get("organization_addresses").iterator();
		String footer = "\n\nSource Code location: https://github.com/ravikulkarni/EventNotification\n";
		
		message = message + footer;
		
		String htmlMessage = message.replaceAll("\\n", "<br>");
		String textMessage = message.replaceAll("\\n", "\\\\n");
		String city = "";
		String line1 = "";
		String postalCode = "";
		
        while(it.hasNext()) {
        	JsonNode n = it.next();
        	city = n.get("city").textValue();
        	line1 = n.get("line1").textValue();
        	postalCode = n.get("postal_code").textValue();
        }
		//Create an email Campaign
		String payload = "{" +
						"\"name\": \"" + title + "\" ,"+
						"\"subject\": \"" + title + "\" ," +
						"\"from_name\": \"" + org_name + "\"," +
						"\"from_email\": \"" + email + "\"," +
						"\"reply_to_email\": \"" + email + "\"," +
						"\"is_permission_reminder_enabled\": true,"+
					    "\"permission_reminder_text\": \"As a reminder, you're receiving this email because you are on the committee\","+
					    "\"is_view_as_webpage_enabled\": false,"+
					    "\"view_as_web_page_text\": \"View this message as a web page\","+
					    "\"view_as_web_page_link_text\": \"Click here\","+
					    "\"greeting_salutations\": \"Hello\","+
					    "\"greeting_name\": \"NONE\","+
					    "\"greeting_string\": \" \","+
					    "\"email_content\": \"<html><body><p> " + htmlMessage + "</p></body></html>\","+
					    "\"text_content\": \"" + textMessage + "\","+
					    "\"email_content_format\": \"HTML\","+
					    "\"style_sheet\": \"\","+
					    "\"message_footer\": {"+
					    "    \"organization_name\": \"" + org_name + "\","+
					    "    \"address_line_1\": \"" + line1 + "\","+
					    "    \"address_line_2\": \"\","+
					    "    \"address_line_3\": \"\","+
					    "    \"city\": \"" + city + "\","+
					    "    \"state\": \"" + stateCode + "\","+
					    "    \"international_state\": \"\","+
					    "    \"postal_code\": \"" + postalCode + "\","+
					    "    \"country\": \"" + countryCode  + "\","+
					    "    \"include_forward_email\": true,"+
					    "    \"forward_email_link_text\": \"Click here to forward this message\","+
					    "    \"include_subscribe_link\": true,"+
					    "    \"subscribe_link_text\": \"Subscribe to Our Newsletter!\""+
					    "},"+
					    "\"sent_to_contact_lists\": ["+
					    "    {"+
					    "        \"id\": \"" + listId + "\"" + 
					    "    }"+
					    "]"+
					    "}";

		
		 response = restPostCall(createCampaignURL, payload);
		 node = parseJson(response);
		 if(node != null) {
			 String campaignId = node.get("id").asText();
			 String scheduleURL = "https://api.constantcontact.com/v2/emailmarketing/campaigns/" + campaignId + "/schedules?api_key=" + KEY;
			 
			 response = restPostCall(scheduleURL, "{}");
			 node = parseJson(response);
			 if(node != null) {
				 System.out.println("Campaign Scheduled for " + node.get("scheduled_date").textValue());
			 }
			 
		 }
		 
	}
	public void process() {
		//Clear the queue
		queue.clear();
		
		//Get all the ids
		getEvents();
		getEventFees();
		getPromoCodes();
		getRegistrants();
		getEventItems();
		//getItemAttributes();
		
		//Generate message
		
		String message = "";
		try {
			//message = new String(Files.readAllBytes(Paths.get(getClass().getResource("EmailTemplate.txt").toURI())));
			InputStream in = getClass().getResourceAsStream("/EmailTemplate.txt"); 
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = "";
			while((line = reader.readLine()) != null) {
				message = message + line;
			}
			
			EventIds eventIds;
			Iterator<EventIds> it = queue.iterator();
			while(it.hasNext()) {
				EventIds eventId = it.next();
				//Add Event information
				message = updatetMessageWithEventDetails(eventId.getEventId(), message);
				
				//Add Info on Promocodes used.
				//message = updateMessageWithPromocodesDetails(eventId.getEventId(), eventId.getPromoCodes(),message);
				
				//Add Info on Items used
				//message = updateMessageWithItemssDetails(eventId.getEventId(), eventId.getItems(),message);
				
				//Add Info on Registrants 
				message = updateMessageWithRegistrants(eventId.getEventId(), eventId.getRegistrants(), message);		
			}
			
			String listId = getCommitteeContactListId();
			
			System.out.println("\nSending Message to List Id:" + listId + "\n" + message);
			
			sendEmail(message);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /*catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
				
	}
	public static void main(String args[]) {
		NotifyRSVP notifyRsvp = new NotifyRSVP(args[0], args[1]);
		notifyRsvp.process();
	}


}

