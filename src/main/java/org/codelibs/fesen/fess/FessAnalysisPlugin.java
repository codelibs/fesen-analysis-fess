package org.codelibs.fesen.fess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.codelibs.fesen.client.Client;
import org.codelibs.fesen.cluster.metadata.IndexNameExpressionResolver;
import org.codelibs.fesen.cluster.service.ClusterService;
import org.codelibs.fesen.common.component.LifecycleComponent;
import org.codelibs.fesen.common.io.stream.NamedWriteableRegistry;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.common.xcontent.NamedXContentRegistry;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.env.NodeEnvironment;
import org.codelibs.fesen.fess.index.analysis.ChineseTokenizerFactory;
import org.codelibs.fesen.fess.index.analysis.JapaneseBaseFormFilterFactory;
import org.codelibs.fesen.fess.index.analysis.JapaneseIterationMarkCharFilterFactory;
import org.codelibs.fesen.fess.index.analysis.JapaneseKatakanaStemmerFactory;
import org.codelibs.fesen.fess.index.analysis.JapanesePartOfSpeechFilterFactory;
import org.codelibs.fesen.fess.index.analysis.JapaneseReadingFormFilterFactory;
import org.codelibs.fesen.fess.index.analysis.JapaneseTokenizerFactory;
import org.codelibs.fesen.fess.index.analysis.KoreanTokenizerFactory;
import org.codelibs.fesen.fess.index.analysis.ReloadableJapaneseTokenizerFactory;
import org.codelibs.fesen.fess.index.analysis.TraditionalChineseConvertCharFilterFactory;
import org.codelibs.fesen.fess.index.analysis.VietnameseTokenizerFactory;
import org.codelibs.fesen.fess.service.FessAnalysisService;
import org.codelibs.fesen.index.analysis.CharFilterFactory;
import org.codelibs.fesen.index.analysis.TokenFilterFactory;
import org.codelibs.fesen.index.analysis.TokenizerFactory;
import org.codelibs.fesen.indices.SystemIndexDescriptor;
import org.codelibs.fesen.indices.analysis.AnalysisModule.AnalysisProvider;
import org.codelibs.fesen.plugins.AnalysisPlugin;
import org.codelibs.fesen.plugins.MapperPlugin;
import org.codelibs.fesen.plugins.Plugin;
import org.codelibs.fesen.plugins.SystemIndexPlugin;
import org.codelibs.fesen.repositories.RepositoriesService;
import org.codelibs.fesen.script.ScriptService;
import org.codelibs.fesen.threadpool.ThreadPool;
import org.codelibs.fesen.watcher.ResourceWatcherService;

public class FessAnalysisPlugin extends Plugin implements AnalysisPlugin, MapperPlugin, SystemIndexPlugin {

    private final PluginComponent pluginComponent = new PluginComponent();

    @Override
    public Collection<Class<? extends LifecycleComponent>> getGuiceServiceClasses() {
        final Collection<Class<? extends LifecycleComponent>> services = new ArrayList<>();
        services.add(FessAnalysisService.class);
        return services;
    }

    @Override
    public Collection<Object> createComponents(final Client client, final ClusterService clusterService, final ThreadPool threadPool,
            final ResourceWatcherService resourceWatcherService, final ScriptService scriptService,
            final NamedXContentRegistry xContentRegistry, final Environment environment, final NodeEnvironment nodeEnvironment,
            final NamedWriteableRegistry namedWriteableRegistry, final IndexNameExpressionResolver indexNameExpressionResolver,
            final Supplier<RepositoriesService> repositoriesServiceSupplier) {
        final Collection<Object> components = new ArrayList<>();
        components.add(pluginComponent);
        return components;
    }

    @Override
    public Map<String, AnalysisProvider<CharFilterFactory>> getCharFilters() {
        final Map<String, AnalysisProvider<CharFilterFactory>> extra = new HashMap<>();
        extra.put("fess_japanese_iteration_mark",
                (indexSettings, env, name, settings) -> new JapaneseIterationMarkCharFilterFactory(indexSettings, env, name, settings,
                        pluginComponent.getFessAnalysisService()));
        extra.put("fess_traditional_chinese_convert",
                (indexSettings, env, name, settings) -> new TraditionalChineseConvertCharFilterFactory(indexSettings, env, name, settings,
                        pluginComponent.getFessAnalysisService()));
        return extra;
    }

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        final Map<String, AnalysisProvider<TokenFilterFactory>> extra = new HashMap<>();
        extra.put("fess_japanese_baseform", (indexSettings, env, name, settings) -> new JapaneseBaseFormFilterFactory(indexSettings, env,
                name, settings, pluginComponent.getFessAnalysisService()));
        extra.put("fess_japanese_part_of_speech",
                (indexSettings, env, name, settings) -> new JapanesePartOfSpeechFilterFactory(indexSettings, env, name, settings,
                        pluginComponent.getFessAnalysisService()));
        extra.put("fess_japanese_readingform", (indexSettings, env, name, settings) -> new JapaneseReadingFormFilterFactory(indexSettings,
                env, name, settings, pluginComponent.getFessAnalysisService()));
        extra.put("fess_japanese_stemmer", (indexSettings, env, name, settings) -> new JapaneseKatakanaStemmerFactory(indexSettings, env,
                name, settings, pluginComponent.getFessAnalysisService()));
        return extra;
    }

    @Override
    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        final Map<String, AnalysisProvider<TokenizerFactory>> extra = new HashMap<>();
        extra.put("fess_japanese_tokenizer", (indexSettings, env, name, settings) -> new JapaneseTokenizerFactory(indexSettings, env, name,
                settings, pluginComponent.getFessAnalysisService()));
        extra.put("fess_japanese_reloadable_tokenizer",
                (indexSettings, env, name, settings) -> new ReloadableJapaneseTokenizerFactory(indexSettings, env, name, settings,
                        pluginComponent.getFessAnalysisService()));
        extra.put("fess_korean_tokenizer", (indexSettings, env, name, settings) -> new KoreanTokenizerFactory(indexSettings, env, name,
                settings, pluginComponent.getFessAnalysisService()));
        extra.put("fess_vietnamese_tokenizer", (indexSettings, env, name, settings) -> new VietnameseTokenizerFactory(indexSettings, env,
                name, settings, pluginComponent.getFessAnalysisService()));
        extra.put("fess_simplified_chinese_tokenizer", (indexSettings, env, name, settings) -> new ChineseTokenizerFactory(indexSettings,
                env, name, settings, pluginComponent.getFessAnalysisService()));
        return extra;
    }

    @Override
    public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(final Settings settings) {
        return Collections.unmodifiableList(Arrays.asList(//
                new SystemIndexDescriptor(".crawler.*", "Contains crawler data"), //
                new SystemIndexDescriptor(".suggest", "Contains suggest setting data"), //
                new SystemIndexDescriptor(".suggest_analyzer", "Contains suggest analyzer data"), //
                new SystemIndexDescriptor(".suggest_array.*", "Contains suggest setting data"), //
                new SystemIndexDescriptor(".suggest_badword.*", "Contains suggest badword data"), //
                new SystemIndexDescriptor(".suggest_elevate.*", "Contains suggest elevate data"), //
                new SystemIndexDescriptor(".fess_config.*", "Contains config data for Fess"), //
                new SystemIndexDescriptor(".fess_user.*", "Contains user data for Fess")));
    }

    public static class PluginComponent {
        private FessAnalysisService fessAnalysisService;

        public FessAnalysisService getFessAnalysisService() {
            return fessAnalysisService;
        }

        public void setFessAnalysisService(final FessAnalysisService fessAnalysisService) {
            this.fessAnalysisService = fessAnalysisService;
        }
    }
}
