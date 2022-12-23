package org.asf.centuria.modules.events.accounts;

import java.util.HashMap;
import java.util.Map;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;

/**
 * 
 * Misc. moderation event - Called when moderation is performed
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.moderate")
public class MiscModerationEvent extends EventObject {

	private String issuerID;
	private CenturiaAccount target;
	private String eventName;
	private String friendlyName;
	private Map<String, String> details;

	public MiscModerationEvent(String eventName, String friendlyName, Map<String, String> details, String issuerID,
			CenturiaAccount target) {
		this.eventName = eventName;
		this.friendlyName = friendlyName;
		this.issuerID = issuerID;
		this.target = target;
		this.details = details;
	}

	@Override
	public String eventPath() {
		return "accounts.moderate";
	}

	/**
	 * Retrieves the moderation event ID (used to identify what kind of action was
	 * taken)
	 * 
	 * @return Event id string
	 */
	public String getModerationEventID() {
		return eventName;
	}

	/**
	 * Retrieves a log-friendly (human-readable) moderation event name
	 * 
	 * @return Moderation event name
	 */
	public String getModerationEventTitle() {
		return friendlyName;
	}

	/**
	 * Retrieves the detail map (human-readable)
	 * 
	 * @return Detail map
	 */
	public Map<String, String> getDetails() {
		return new HashMap<String, String>(details);
	}

	/**
	 * Retrieves the account that is being moderated (CAN BE NULL)
	 * 
	 * @return CenturiaAccount instance or null
	 */
	public CenturiaAccount getTarget() {
		return target;
	}

	/**
	 * Retrieves the action issuer
	 *
	 * @return Action issuer ID
	 */
	public String getIssuer() {
		return issuerID;
	}

}
