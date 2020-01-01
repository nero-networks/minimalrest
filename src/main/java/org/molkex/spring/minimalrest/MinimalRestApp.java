package org.molkex.spring.minimalrest;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import io.swagger.codegen.v3.service.GeneratorService;
import io.swagger.models.Swagger;
import io.swagger.v3.core.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.molkex.spring.minimalrest.config.MinimalRestConfig;
import org.molkex.spring.minimalrest.tools.ScriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import springfox.documentation.swagger2.mappers.ServiceModelToSwagger2Mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static springfox.documentation.builders.PathSelectors.regex;

@EnableSwagger2
@ComponentScan("org.molkex.spring.minimalrest.config")
public class MinimalRestApp extends WebMvcConfigurationSupport implements CommandLineRunner {
    private Logger log = Logger.getLogger(MinimalRestApp.class.getName());

    @Autowired
    private DocumentationCache documentationCache;

    @Autowired
    private MinimalRestConfig config;

    @Autowired
    private ServiceModelToSwagger2Mapper mapper;

    private GeneratorService generatorService = new GeneratorService();

    private String[] commandLineArgs;

    /** Configure static webserver resources */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    /** Register a / to /index.html forward */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    /*
        api publisher
     */

    @Bean
    public Docket springFoxBuilder() {
        return configureSpringFoxApiSelectorBuilder(
                createSpringFoxBuilder().select()).build();
    }

    protected Docket createSpringFoxBuilder() {
        return new Docket(DocumentationType.SWAGGER_2);
    }

    protected ApiSelectorBuilder configureSpringFoxApiSelectorBuilder(ApiSelectorBuilder builder) {
        if (getApplicationContext() != null
                && (config.getSwagger() == null || config.getSwagger().autoconfigured)) {

            List<Predicate<String>> selector = new ArrayList<>();
            getApplicationContext().getBeansWithAnnotation(RestController.class)
                    .forEach((k, b) -> {
                        RequestMapping r = b.getClass().getAnnotation(RequestMapping.class);
                        String path = r.path().length > 0 ? r.path()[0] : r.value()[0];
                        log.info("Select path " + path + " as " + b.getClass().getName());
                        selector.add(regex(path + ".*"));
                    });
            Predicate<String> pred = Predicates.alwaysFalse();
            for(Predicate<String> p : selector) {
                pred = Predicates.or(pred, p);
            }
            builder.paths(pred);
        }
        return builder;
    }

    /*
        client generator
     */

    @Override
    public void run(String... args) throws Exception {
        if (System.getProperty("minimalrest.swagger.generate") != null) {
            if (config.getSwagger() != null) {
                if (StringUtils.isNotEmpty(config.getSwagger().getJsonFile())) {
                    writeSwaggerJsonFile(config.getSwagger().getJsonFile());
                }

                if (config.getSwagger().getClients().size() > 0) {
                    Swagger spec = getSwaggerModel();
                    for (MinimalRestConfig.SwaggerConfig.GenerationRequestConfig client : config.getSwagger().getClients()) {
                        client.spec(spec);
                        generateSwaggerClient(client);
                    }
                }
            }

            System.exit(0);
        }
    }

    protected void writeSwaggerJsonFile(String path) throws IOException {
        if (path.length() > 0) {
            Files.write(Paths.get(path), Json.pretty(getSwaggerModel()).getBytes());
        }
    }

    protected Swagger getSwaggerModel() {
        return mapper.mapDocumentation(documentationCache.documentationByGroup(Docket.DEFAULT_GROUP_NAME));
    }

    protected void generateSwaggerClient(MinimalRestConfig.SwaggerConfig.GenerationRequestConfig config) throws Exception {

        generatorService.generationRequest(config).generate();

        if (config.getInstaller() != null) {
            String outdir = config.getOptions().getOutputDir();

            if (outdir.charAt(0) == '/') outdir = outdir.substring(1);
            if (ScriptExecutor.isWindows && outdir.contains(":")) outdir = outdir.substring(3);

            String filename = "installer" + (ScriptExecutor.isWindows ? ".bat" : ".sh");
            Files.write(
                    Paths.get(outdir,filename),
                    config.getInstaller().getBytes());
            ScriptExecutor.execute(Paths.get(outdir), filename, l -> log.info(l));
        }
    }
}
