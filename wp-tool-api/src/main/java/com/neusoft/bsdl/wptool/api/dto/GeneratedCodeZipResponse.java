package com.neusoft.bsdl.wptool.api.dto;

import java.util.List;

public record GeneratedCodeZipResponse(String zipBase64, String taskId, List<String> errorLog, List<String> warnLog, List<String> checkLog) {
}
