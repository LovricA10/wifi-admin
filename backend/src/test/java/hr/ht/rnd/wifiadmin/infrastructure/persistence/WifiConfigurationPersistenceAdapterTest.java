package hr.ht.rnd.wifiadmin.infrastructure.persistence;

import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiBand;
import hr.ht.rnd.wifiadmin.domain.WifiConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(WifiConfigurationPersistenceAdapter.class)
class WifiConfigurationPersistenceAdapterTest {

    @Autowired
    WifiConfigurationPersistenceAdapter adapter;

    @Autowired
    TestEntityManager testEntityManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final WifiConfiguration CONFIG = new WifiConfiguration(
            "CPE_001", WifiBand.BAND_2_4_GHZ, "TestSSID", EncryptionType.WPA2_PSK, "secret");

    @Test
    void save_newConfig_persistsAllFields() {
        WifiConfiguration saved = adapter.save(CONFIG);

        assertThat(saved.cpeId()).isEqualTo("CPE_001");
        assertThat(saved.wifiBand()).isEqualTo(WifiBand.BAND_2_4_GHZ);
        assertThat(saved.ssid()).isEqualTo("TestSSID");
        assertThat(saved.encryptionType()).isEqualTo(EncryptionType.WPA2_PSK);
        assertThat(saved.password()).isEqualTo("secret");
    }

    @Test
    void save_newConfig_setsTimestampsAndNullLastSyncedAt() {
        adapter.save(CONFIG);
        testEntityManager.flush();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM wifi_configuration WHERE cpe_id = ?", "CPE_001");
        assertThat(row.get("CREATED_AT")).isNotNull();
        assertThat(row.get("UPDATED_AT")).isNotNull();
        assertThat(row.get("LAST_SYNCED_AT")).isNull();
    }

    @Test
    void save_existingConfig_updatesFieldsAndPreservesCreatedAt() {
        adapter.save(CONFIG);
        testEntityManager.flush();

        Map<String, Object> before = jdbcTemplate.queryForMap(
                "SELECT created_at FROM wifi_configuration WHERE cpe_id = ?", "CPE_001");

        var updated = new WifiConfiguration("CPE_001", WifiBand.BAND_5_GHZ, "NewSSID", EncryptionType.OPEN, null);
        adapter.save(updated);
        testEntityManager.flush();

        Optional<WifiConfiguration> result = adapter.findByCpeId("CPE_001");
        assertThat(result).isPresent();
        assertThat(result.get().ssid()).isEqualTo("NewSSID");
        assertThat(result.get().wifiBand()).isEqualTo(WifiBand.BAND_5_GHZ);

        Map<String, Object> after = jdbcTemplate.queryForMap(
                "SELECT created_at FROM wifi_configuration WHERE cpe_id = ?", "CPE_001");
        assertThat(after.get("CREATED_AT")).isEqualTo(before.get("CREATED_AT"));
    }

    @Test
    void saveFromSync_newConfig_setsLastSyncedAt() {
        adapter.saveFromSync(CONFIG);
        testEntityManager.flush();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM wifi_configuration WHERE cpe_id = ?", "CPE_001");
        assertThat(row.get("LAST_SYNCED_AT")).isNotNull();
    }

    @Test
    void saveFromSync_existingConfig_updatesLastSyncedAt() {
        adapter.save(CONFIG);
        testEntityManager.flush();

        adapter.saveFromSync(CONFIG);
        testEntityManager.flush();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM wifi_configuration WHERE cpe_id = ?", "CPE_001");
        assertThat(row.get("LAST_SYNCED_AT")).isNotNull();
    }

    @Test
    void findByCpeId_nonExistentId_returnsEmpty() {
        Optional<WifiConfiguration> result = adapter.findByCpeId("NOT_EXISTS");

        assertThat(result).isEmpty();
    }

    @Test
    void findByCpeId_existingRecord_returnsConfig() {
        adapter.save(CONFIG);

        Optional<WifiConfiguration> result = adapter.findByCpeId("CPE_001");

        assertThat(result).isPresent();
        assertThat(result.get().cpeId()).isEqualTo("CPE_001");
        assertThat(result.get().encryptionType()).isEqualTo(EncryptionType.WPA2_PSK);
    }

    @Test
    void save_enumsStoredAsStrings() {
        adapter.save(CONFIG);
        testEntityManager.flush();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT wifi_band, encryption_type FROM wifi_configuration WHERE cpe_id = ?", "CPE_001");
        assertThat(row.get("WIFI_BAND")).isEqualTo("BAND_2_4_GHZ");
        assertThat(row.get("ENCRYPTION_TYPE")).isEqualTo("WPA2_PSK");
    }
}
