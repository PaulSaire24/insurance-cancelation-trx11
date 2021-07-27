package com.bbva.rbvd.lib.r011.impl;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.elara.library.AbstractLibrary;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.rbvd.lib.r003.RBVDR003;
import com.bbva.rbvd.lib.r011.RBVDR011;
import com.bbva.rbvd.lib.r012.RBVDR012;

public abstract class RBVDR011Abstract extends AbstractLibrary implements RBVDR011 {

	protected ApplicationConfigurationService applicationConfigurationService;

	protected PISDR100 pisdR100;

	protected RBVDR012 rbvdR012;

	protected RBVDR003 rbvdR003;


	/**
	* @param applicationConfigurationService the this.applicationConfigurationService to set
	*/
	public void setApplicationConfigurationService(ApplicationConfigurationService applicationConfigurationService) {
		this.applicationConfigurationService = applicationConfigurationService;
	}

	/**
	* @param pisdR100 the this.pisdR100 to set
	*/
	public void setPisdR100(PISDR100 pisdR100) {
		this.pisdR100 = pisdR100;
	}

	/**
	* @param rbvdR012 the this.rbvdR012 to set
	*/
	public void setRbvdR012(RBVDR012 rbvdR012) {
		this.rbvdR012 = rbvdR012;
	}

	/**
	* @param rbvdR003 the this.rbvdR003 to set
	*/
	public void setRbvdR003(RBVDR003 rbvdR003) {
		this.rbvdR003 = rbvdR003;
	}

}