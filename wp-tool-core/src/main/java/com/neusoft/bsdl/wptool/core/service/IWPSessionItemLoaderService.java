package com.neusoft.bsdl.wptool.core.service;

public interface IWPSessionItemLoaderService {

    String findSessionKeyByName(String sessionName);

    String findSessionNameByKey(String sessionKey);

}