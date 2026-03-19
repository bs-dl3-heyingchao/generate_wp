package com.neusoft.bsdl.wptool.core.service.impl;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.neusoft.bsdl.wptool.core.exception.WPException;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.SessionManagementContent;
import com.neusoft.bsdl.wptool.core.model.SessionManagementSystemField;
import com.neusoft.bsdl.wptool.core.service.ConfigService;
import com.neusoft.bsdl.wptool.core.service.IWPSessionItemService;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;

/**
 * メッセージ情報ロードツールクラス
 */
public class WPSessionItemService implements IWPSessionItemService {
    private Map<String, String> sessionMap = null;

    @Override
    public String findSessionKeyByName(String sessionName) {
        assert sessionMap != null && !sessionMap.isEmpty() : "sessionMap is not initialized.";
        return sessionMap.get(sessionName);
    }

    @Override
    public String findSessionNameByKey(String sessionKey) {
        assert sessionMap != null && !sessionMap.isEmpty() : "sessionMap is not initialized.";
        var valueOptional = sessionMap.entrySet().stream().filter(entry -> entry.getValue().equals(sessionKey)).findFirst();
        if (valueOptional.isPresent()) {
            return valueOptional.get().getKey();
        }
        return null;
    }

    public synchronized void initialize() {
        File defineFile = ConfigService.getSvnSessionItemDefineFile();
        if (!defineFile.exists() || !defineFile.isFile()) {
            throw new WPException("セッションアイテム定義ファイルが存在しないか、ファイルではありません: " + defineFile.getAbsolutePath());
        }
        try {
            FileSource source = new LocalFileSource(defineFile.getAbsolutePath());
            SessionManagementContent sessionManagementContent = ParseExcelUtils.parseSessionManagementExcel(source);
            List<SessionManagementSystemField> list = sessionManagementContent.getSessionManagement().get("セッション項目一覧");
            sessionMap = new HashMap<String, String>();
            for (SessionManagementSystemField field : list) {
                sessionMap.put(field.getSessionLogicName(), field.getSessionKey());
            }
        } catch (Exception e) {
            throw new WPException("セッションアイテム定義ファイルの読み込みに失敗しました: " + defineFile.getAbsolutePath(), e);
        }
    }
}
