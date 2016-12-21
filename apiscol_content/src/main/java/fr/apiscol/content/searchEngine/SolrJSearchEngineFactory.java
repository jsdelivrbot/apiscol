package fr.apiscol.content.searchEngine;

import fr.apiscol.auth.oauth.OauthServersProxy;

public class SolrJSearchEngineFactory implements ISearchEngineFactory {

	@Override
	public ISearchEngineQueryHandler getQueryHandler(String solrAddress,
			String solrSearchPath, String solrUpdatePath,
			String solrExtractPath, String solrSuggestPath,
			OauthServersProxy oauthServersProxy) {
		return new SolrJSearchEngineQueryHandler(solrAddress, solrSearchPath,
				solrUpdatePath, solrExtractPath, solrSuggestPath, oauthServersProxy);
	}

	@Override
	public ISearchEngineResultHandler getResultHandler() {
		return new SolrJSearchEngineResultHandler();
	}

}
