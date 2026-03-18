package com.neusoft.bsdl.wptool.core.service.impl;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.neusoft.bsdl.wptool.core.exception.WPException;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.SessionManagementContent;
import com.neusoft.bsdl.wptool.core.model.SessionManagementSystemField;
import com.neusoft.bsdl.wptool.core.reader.WPTableBeanReader;
import com.neusoft.bsdl.wptool.core.service.ConfigService;
import com.neusoft.bsdl.wptool.core.service.IWPSessionItemLoaderService;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;

import cbai.util.FileUtil;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.reader.ITableBeanReader;

/**
 * TODO：メッセージ情報ロードツールクラス
 */
public class WPSessionItemLoaderService implements IWPSessionItemLoaderService {
    private Map<String, String> sessionMap = new LinkedHashMap<String, String>();

    @Override
    public String findSessionKeyByName(String sessionName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String findSessionNameByKey(String sessionKey) {
        // TODO Auto-generated method stub
        return null;
    }

    public synchronized void initialize() throws Exception {
        File defineFile = ConfigService.getSvnSessionItemDefineFile();
        if (!defineFile.exists() || !defineFile.isFile()) {
            throw new WPException("セッションアイテム定義ファイルが存在しないか、ファイルではありません: " + defineFile.getAbsolutePath());
        }
        FileSource source = new LocalFileSource(defineFile.getAbsolutePath());
        SessionManagementContent sessionManagementContent = ParseExcelUtils.parseSessionManagementExcel(source);
        List<SessionManagementSystemField> list = sessionManagementContent.getSessionManagement().get("セッション項目一覧");
        for (SessionManagementSystemField field : list) {
            sessionMap.put(field.getSessionLogicName(), field.getSessionKey());
        }
    }
}
