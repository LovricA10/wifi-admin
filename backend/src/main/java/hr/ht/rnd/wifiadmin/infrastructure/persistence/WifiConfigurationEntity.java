package hr.ht.rnd.wifiadmin.infrastructure.persistence;

import hr.ht.rnd.wifiadmin.domain.EncryptionType;
import hr.ht.rnd.wifiadmin.domain.WifiBand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "wifi_configuration")
@Getter
@Setter
@NoArgsConstructor
class WifiConfigurationEntity {

    @Id
    @Column(name = "cpe_id", length = 64, nullable = false)
    private String cpeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "wifi_band", length = 32, nullable = false)
    private WifiBand wifiBand;

    @Column(name = "ssid", length = 32, nullable = false)
    private String ssid;

    @Enumerated(EnumType.STRING)
    @Column(name = "encryption_type", length = 32, nullable = false)
    private EncryptionType encryptionType;

    @Column(name = "password", length = 128)
    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
