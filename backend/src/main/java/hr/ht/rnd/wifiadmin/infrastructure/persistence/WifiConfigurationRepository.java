package hr.ht.rnd.wifiadmin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface WifiConfigurationRepository extends JpaRepository<WifiConfigurationEntity, String> {
}
