package com.neusoft.bsdl.wptool.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class WpToolCoreConfigBridge {

    @Value("${wp-tool.core-config.svn-base-dir:}")
    private String svnBaseDir;

    @Value("${wp-tool.core-config.svn-db-define-dir:}")
    private String svnDbDefineDir;

    @Value("${wp-tool.core-config.db-define-cachefile:}")
    private String dbDefineCachefile;

    @Value("${wp-tool.core-config.svn-session-items-define-file:}")
    private String svnSessionItemsDefineFile;

    @Value("${wp-tool.core-config.ai-support-url:}")
    private String aiSupportUrl;

    @Value("${wp-tool.core-config.ai-support-token:}")
    private String aiSupportToken;

    @PostConstruct
    public void applyOverrides() {
        applyIfPresent("wp-tool.svn.base-dir", svnBaseDir);
        applyIfPresent("wp-tool.svn.db-define.dir", svnDbDefineDir);
        applyIfPresent("wp-tool.db.db-define.cachefile", dbDefineCachefile);
        applyIfPresent("wp-tool.svn.session-items-define.file", svnSessionItemsDefineFile);
        applyIfPresent("wp-tool.ai.support.url", aiSupportUrl);
        applyIfPresent("wp-tool.ai.support.token", aiSupportToken);
    }

    private void applyIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            System.setProperty(key, value.trim());
        }
    }
}
