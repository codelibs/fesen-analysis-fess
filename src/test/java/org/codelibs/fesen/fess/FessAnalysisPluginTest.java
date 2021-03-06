package org.codelibs.fesen.fess;

import static org.codelibs.fesen.runner.FesenRunner.newConfigs;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.codelibs.curl.CurlResponse;
import org.codelibs.fesen.action.DocWriteResponse.Result;
import org.codelibs.fesen.action.index.IndexResponse;
import org.codelibs.fesen.action.search.SearchResponse;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.common.xcontent.XContentBuilder;
import org.codelibs.fesen.common.xcontent.XContentFactory;
import org.codelibs.fesen.common.xcontent.XContentType;
import org.codelibs.fesen.index.query.QueryBuilders;
import org.codelibs.fesen.node.Node;
import org.codelibs.fesen.runner.FesenRunner;
import org.codelibs.fesen.runner.net.FesenCurl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FessAnalysisPluginTest {

    private FesenRunner runner;

    private final int numOfNode = 1;

    private final int numOfDocs = 1000;

    private String clusterName;

    @Before
    public void setUp() throws Exception {
        clusterName = "es-kuromojineologd-" + System.currentTimeMillis();
        runner = new FesenRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("http.cors.allow-origin", "*");
            settingsBuilder.put("discovery.type", "single-node");
            // settingsBuilder.putList("discovery.seed_hosts", "127.0.0.1:9301");
            // settingsBuilder.putList("cluster.initial_master_nodes", "127.0.0.1:9301");
        }).build(newConfigs().clusterName(clusterName).numOfNode(numOfNode).pluginTypes("org.codelibs.fesen.fess.FessAnalysisPlugin"));
    }

    @After
    public void cleanUp() throws Exception {
        runner.close();
        runner.clean();
    }

    @Test
    public void test_japanese() throws Exception {

        runner.ensureYellow();
        final Node node = runner.node();

        final String index = "dataset";
        final String type = "item";

        final String indexSettings = "{\"index\":{\"analysis\":{" + "\"tokenizer\":{"//
                + "\"ja_user_dict\":{\"type\":\"fess_japanese_tokenizer\",\"mode\":\"extended\",\"user_dictionary\":\"userdict_ja.txt\"}"
                + "},"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"ja_user_dict\",\"filter\":[\"fess_japanese_stemmer\"]}" + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings, XContentType.JSON).build());

        // create a mapping
        final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()//
                .startObject()//
                .startObject(type)//
                .startObject("properties")//

                // id
                .startObject("id")//
                .field("type", "keyword")//
                .endObject()//

                // msg1
                .startObject("msg")//
                .field("type", "text")//
                .field("analyzer", "ja_analyzer")//
                .endObject()//

                .endObject()//
                .endObject()//
                .endObject();
        runner.createMapping(index, type, mappingBuilder);

        final IndexResponse indexResponse1 = runner.insert(index, type, "1", "{\"msg\":\"????????????????????????\", \"id\":\"1\"}");
        assertEquals(Result.CREATED, indexResponse1.getResult());
        runner.refresh();

        assertDocCount(0, index, "msg", "????????????????????????");

        try (CurlResponse response = FesenCurl.post(node, "/" + index + "/_analyze").header("Content-Type", "application/json")
                .body("{\"text\":\"????????????????????????\",\"analyzer\":\"ja_analyzer\"}").execute()) {
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getContent(FesenCurl.jsonParser()).get("tokens");
            assertEquals(0, tokens.size());
        }

    }

    private void assertDocCount(final int expected, final String index, final String field, final String value) {
        final SearchResponse searchResponse = runner.search(index, QueryBuilders.matchPhraseQuery(field, value), null, 0, numOfDocs);
        assertEquals(expected, searchResponse.getHits().getTotalHits().value);
    }
}
