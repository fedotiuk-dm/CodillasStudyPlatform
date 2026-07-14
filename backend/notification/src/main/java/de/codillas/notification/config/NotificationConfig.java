package de.codillas.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the module's {@link NotificationProperties} (mirrors {@code files} S3Config). */
@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {}
