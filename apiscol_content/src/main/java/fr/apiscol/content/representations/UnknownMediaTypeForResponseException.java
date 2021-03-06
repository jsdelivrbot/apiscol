package fr.apiscol.content.representations;

import javax.ws.rs.WebApplicationException;

import org.apache.log4j.Logger;

import fr.apiscol.ApiscolException;
import fr.apiscol.utils.LogUtility;

public class UnknownMediaTypeForResponseException extends ApiscolException {
	private static Logger webApplicationExceptionLogger;
	{
		if (webApplicationExceptionLogger == null) {
			webApplicationExceptionLogger = LogUtility
					.createLogger(WebApplicationException.class
							.getCanonicalName());
		}
	}

	public UnknownMediaTypeForResponseException(String mediaType) {
		super(
				String.format(
						"There are no responses provided for the requested mediatype %s.",
						mediaType));
		webApplicationExceptionLogger
				.error("There are no responses provided for the requested mediatype "
						+ mediaType);

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
