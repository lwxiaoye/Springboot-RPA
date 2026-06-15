package rpa.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatasourceDefaultsTest {

    @Test
    void localDatasourceDefaultsUseLocalMysql3307() throws IOException {
        Map<String, Object> yaml = loadApplicationYaml();
        Map<String, Object> spring = map(yaml.get("spring"));
        Map<String, Object> datasource = map(spring.get("datasource"));

        assertThat(datasource.get("url").toString())
                .contains("${DB_HOST:localhost}")
                .contains("${DB_PORT:3307}")
                .contains("${DB_NAME:rpa_system}");
        assertThat(datasource.get("username")).isEqualTo("${DB_USERNAME:root}");
        assertThat(datasource.get("password")).isEqualTo("${DB_PASSWORD:123456}");
    }

    private Map<String, Object> loadApplicationYaml() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertThat(input).isNotNull();
            return new Yaml().load(input);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
