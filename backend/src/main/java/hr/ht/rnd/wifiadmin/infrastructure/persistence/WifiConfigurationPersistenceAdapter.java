package hr.ht.rnd.wifiadmin.infrastructure.persistence;

import hr.ht.rnd.wifiadmin.domain.WifiConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
public class WifiConfigurationPersistenceAdapter {

    private final WifiConfigurationRepository repository;

    WifiConfigurationPersistenceAdapter(WifiConfigurationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<WifiConfiguration> findByCpeId(String cpeId) {
        return repository.findById(cpeId).map(this::toDomain);
    }

    @Transactional
    public WifiConfiguration save(WifiConfiguration configuration) {
        WifiConfigurationEntity entity = repository.findById(configuration.cpeId())
                .map(existing -> applyUpdate(existing, configuration))
                .orElseGet(() -> toNewEntity(configuration));
        return toDomain(repository.save(entity));
    }

    @Transactional
    public WifiConfiguration saveFromSync(WifiConfiguration configuration) {
        Instant now = Instant.now();
        WifiConfigurationEntity entity = repository.findById(configuration.cpeId())
                .map(existing -> {
                    applyUpdate(existing, configuration);
                    existing.setLastSyncedAt(now);
                    return existing;
                })
                .orElseGet(() -> {
                    WifiConfigurationEntity created = toNewEntity(configuration);
                    created.setLastSyncedAt(now);
                    return created;
                });
        return toDomain(repository.save(entity));
    }

    private WifiConfigurationEntity applyUpdate(WifiConfigurationEntity entity, WifiConfiguration configuration) {
        entity.setWifiBand(configuration.wifiBand());
        entity.setSsid(configuration.ssid());
        entity.setEncryptionType(configuration.encryptionType());
        entity.setPassword(configuration.password());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private WifiConfigurationEntity toNewEntity(WifiConfiguration configuration) {
        Instant now = Instant.now();
        WifiConfigurationEntity entity = new WifiConfigurationEntity();
        entity.setCpeId(configuration.cpeId());
        entity.setWifiBand(configuration.wifiBand());
        entity.setSsid(configuration.ssid());
        entity.setEncryptionType(configuration.encryptionType());
        entity.setPassword(configuration.password());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private WifiConfiguration toDomain(WifiConfigurationEntity entity) {
        return new WifiConfiguration(
                entity.getCpeId(),
                entity.getWifiBand(),
                entity.getSsid(),
                entity.getEncryptionType(),
                entity.getPassword()
        );
    }
}
