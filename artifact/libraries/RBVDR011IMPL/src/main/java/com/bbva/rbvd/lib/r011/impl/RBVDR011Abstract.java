package com.bbva.rbvd.lib.r011.impl;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.elara.library.AbstractLibrary;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.pisd.lib.r401.PISDR401;
import com.bbva.rbvd.lib.r011.RBVDR011;
import com.bbva.rbvd.lib.r042.RBVDR042;
import com.bbva.rbvd.lib.r051.RBVDR051;
import com.bbva.rbvd.lib.r311.RBVDR311;

/**
 * This class automatically defines the libraries and utilities that it will use.
 */
public abstract class RBVDR011Abstract extends AbstractLibrary implements RBVDR011 {

	protected ApplicationConfigurationService applicationConfigurationService;

	protected PISDR100 pisdR100;

	protected PISDR103 pisdR103;

	protected RBVDR042 rbvdR042;

	protected RBVDR051 rbvdR051;

	protected RBVDR311 rbvdR311;

	protected PISDR401 pisdR401;
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
	* @param pisdR103 the this.pisdR103 to set
	*/
	public void setPisdR103(PISDR103 pisdR103) {
		this.pisdR103 = pisdR103;
	}

	/**
	* @param rbvdR042 the this.rbvdR042 to set
	*/
	public void setRbvdR042(RBVDR042 rbvdR042) {
		this.rbvdR042 = rbvdR042;
	}

	/**
	* @param rbvdR051 the this.rbvdR051 to set
	*/
	public void setRbvdR051(RBVDR051 rbvdR051) {
		this.rbvdR051 = rbvdR051;
	}

	/**
	* @param rbvdR311 the this.rbvdR311 to set
	*/
	public void setRbvdR311(RBVDR311 rbvdR311) {
		this.rbvdR311 = rbvdR311;
	}

	public void setPisdR401(PISDR401 pisdR401) {this.pisdR401 = pisdR401;}
}