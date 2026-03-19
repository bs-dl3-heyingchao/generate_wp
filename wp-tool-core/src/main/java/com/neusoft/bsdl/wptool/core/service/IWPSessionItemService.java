package com.neusoft.bsdl.wptool.core.service;

public interface IWPSessionItemService {

    String findSessionKeyByName(String sessionName);

    String findSessionNameByKey(String sessionKey);

}