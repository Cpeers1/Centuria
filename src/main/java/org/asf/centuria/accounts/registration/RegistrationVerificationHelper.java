package org.asf.centuria.accounts.registration;

import java.util.ArrayList;

import org.asf.centuria.accounts.CenturiaAccount;

import com.google.gson.JsonObject;

public abstract class RegistrationVerificationHelper {

	private static ArrayList<RegistrationVerificationHelper> helpers = new ArrayList<RegistrationVerificationHelper>();

	protected void register() {
		helpers.add(this);
	}

	/**
	 * Registration verification method ID
	 * 
	 * @return Method ID string
	 */
	public abstract String registrationMethodID();

	/**
	 * Retrieves a helper by a method ID
	 * 
	 * @param method Registration verification method ID
	 * @return RegistrationVerificationHelper instance or null
	 */
	public static RegistrationVerificationHelper getByMethod(String method) {
		for (RegistrationVerificationHelper helper : helpers)
			if (helper.registrationMethodID().equals(method))
				return helper;
		return null;
	}

	/**
	 * Retrieves all registration verification helpers
	 * 
	 * @return Array of RegistrationVerificationHelper instances
	 */
	public static RegistrationVerificationHelper[] getHelpers() {
		return helpers.toArray(t -> new RegistrationVerificationHelper[t]);
	}

	/**
	 * Verifies the registration request
	 * 
	 * @param loginName   Selected login name
	 * @param displayName Selected display name
	 * @param payload     Verification payload
	 * @return Verification result
	 */
	public abstract RegistrationVerificationResult verify(String loginName, String displayName, JsonObject payload);

	/**
	 * Called after an account was successfully registered
	 * 
	 * @param account     Account that was registered
	 * @param loginName   Account login name
	 * @param displayName Account display name
	 * @param payload     Verification payload
	 */
	public abstract void postRegistration(CenturiaAccount account, String loginName, String displayName,
			JsonObject payload);

}
