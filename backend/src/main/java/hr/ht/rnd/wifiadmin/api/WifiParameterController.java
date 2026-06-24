package hr.ht.rnd.wifiadmin.api;

import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationRequest;
import hr.ht.rnd.wifiadmin.api.dto.WifiConfigurationResponse;
import hr.ht.rnd.wifiadmin.application.WifiParameterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wifi-parameter")
@Validated
public class WifiParameterController {

    private final WifiParameterService wifiParameterService;

    WifiParameterController(WifiParameterService wifiParameterService) {
        this.wifiParameterService = wifiParameterService;
    }

    @GetMapping("/{cpeId}")
    public ResponseEntity<WifiConfigurationResponse> getWifiParameter(
            @PathVariable @NotBlank @Size(max = 64) String cpeId) {
        return ResponseEntity.ok(wifiParameterService.getWifiParameter(cpeId));
    }

    @PutMapping
    public ResponseEntity<WifiConfigurationResponse> updateWifiParameter(
            @RequestBody @Valid WifiConfigurationRequest request) {
        return ResponseEntity.ok(wifiParameterService.updateWifiParameter(request));
    }
}
