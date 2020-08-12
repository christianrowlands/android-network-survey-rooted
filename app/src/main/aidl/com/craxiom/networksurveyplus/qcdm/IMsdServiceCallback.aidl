package com.craxiom.networksurveyplus.qcdm;

interface IMsdServiceCallback {
	void stateChanged(String reason);
	void internalError();
}
