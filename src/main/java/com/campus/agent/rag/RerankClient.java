package com.campus.agent.rag;

import java.util.List;

public interface RerankClient {

    List<RagSearchResult> rerank(String query, List<RagSearchResult> candidates, int topK);
}
