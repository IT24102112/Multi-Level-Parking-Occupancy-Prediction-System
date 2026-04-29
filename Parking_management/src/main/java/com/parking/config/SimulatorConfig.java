package com.parking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "simulator")
public class SimulatorConfig {

    private boolean enabled = true;
    private int speedMultiplier = 1;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(int speedMultiplier) { this.speedMultiplier = speedMultiplier; }
}