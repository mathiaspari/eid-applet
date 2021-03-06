/*
 * eID Applet Project.
 * Copyright (C) 2008-2009 FedICT.
 * Copyright (C) 2014 e-Contract.be BVBA.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eid.applet;

import java.awt.Component;

/**
 * Interface for view component.
 * 
 * @author Frank Cornelis
 * 
 */
public interface View {

	void addDetailMessage(String detailMessage);

	void setStatusMessage(Status status, Messages.MESSAGE_ID messageId);

	boolean privacyQuestion(boolean includeAddress, boolean includePhoto,
			String identityDataUsage);

	Component getParentComponent();

	/*
	 * Progress indication functions.
	 */
	void setProgressIndeterminate();

	void resetProgress(int max);

	void increaseProgress();

	void confirmAuthenticationSignature(String message);

	int confirmSigning(String description, String digestAlgo);
}
