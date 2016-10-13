package com.sarvaysh;

import java.util.ArrayList;
import java.util.List;

/*
 *  This class hold all the Ids
 * 
 */
public class EventIds {
	private String eventId;
	private List<String> fees = new ArrayList<String>();
	private List<String> promoCodes = new ArrayList<String>();
	private List<String> registrants = new ArrayList<String>();
	private List<String> items = new ArrayList<String>();
	private List<String> itemAttributes = new ArrayList<String>();
	
	public EventIds(String eventId, List<String> fees, List<String> promoCodes,
			List<String> registrants, List<String> items,
			List<String> attributes) {
		super();
		this.eventId = eventId;
		this.fees = fees;
		this.promoCodes = promoCodes;
		this.registrants = registrants;
		this.items = items;
		this.itemAttributes = attributes;
	}
	
	public String getEventId() {
		return eventId;
	}
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}
	public List<String> getFees() {
		return fees;
	}
	public void setFees(List<String> fees) {
		this.fees = fees;
	}
	public List<String> getPromoCodes() {
		return promoCodes;
	}
	public void setPromoCodes(List<String> promoCodes) {
		this.promoCodes = promoCodes;
	}
	public List<String> getRegistrants() {
		return registrants;
	}
	public void setRegistrants(List<String> registrants) {
		this.registrants = registrants;
	}
	public List<String> getItems() {
		return items;
	}
	public void setItems(List<String> items) {
		this.items = items;
	}
	public List<String> getItemAttributes() {
		return itemAttributes;
	}
	public void setItemAttributes(List<String> attributes) {
		this.itemAttributes = attributes;
	}
	
}
