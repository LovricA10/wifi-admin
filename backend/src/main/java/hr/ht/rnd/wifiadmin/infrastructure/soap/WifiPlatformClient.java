package hr.ht.rnd.wifiadmin.infrastructure.soap;

import hr.ht.rnd.wifiadmin.domain.WifiConfiguration;

public interface WifiPlatformClient {

    WifiConfiguration getByCpeId(String cpeId);

    WifiConfiguration update(WifiConfiguration configuration);
}
