package com.neusoft.bsdl.wptool.core.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionManagementContent {
	/** システム項目 */
	Map<String, List<SessionManagementSystemField>> sessionManagement;
}
