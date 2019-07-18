package org.molkex.spring.minimalrest.config;

import io.swagger.codegen.v3.service.GenerationRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Values are automatically mapped from ${minimalrest}. */
@Component
@ConfigurationProperties("minimalrest")
public class MinimalRestConfig {

    /** Namespace container for values of ${minimalrest.swagger} */
    private SwaggerConfig swagger;

    public SwaggerConfig getSwagger() {
        return swagger;
    }

    public void setSwagger(SwaggerConfig swagger) {
        this.swagger = swagger;
    }

    public static class SwaggerConfig {

        /** Enable auto-configuration of the SpringFox Docket */
        public boolean autoconfigured = true;

        public boolean isAutoconfigured() {
            return autoconfigured;
        }

        public void setAutoconfigured(boolean autoconfigured) {
            this.autoconfigured = autoconfigured;
        }

        /** Path to a file to serialize the swagger.json into. */
        private String jsonFile;

        public String getJsonFile() {
            return jsonFile;
        }

        public void setJsonFile(String jsonFile) {
            this.jsonFile = jsonFile;
        }

        /**  */
        private List<GenerationRequestConfig> clients = new ArrayList<>();

        public List<GenerationRequestConfig> getClients() {
            return clients;
        }

        public static class GenerationRequestConfig extends GenerationRequest {

            /** */
            private String installer;

            public String getInstaller() {
                return installer;
            }

            public void setInstaller(String installer) {
                this.installer = installer;
            }
        }
    }
}
