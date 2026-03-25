package com.neusoft.bsdl.wptool.core.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.neusoft.bsdl.wptool.core.service.IWPTableSearchService;

import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;

public class WPCombinedTableSearchService implements IWPTableSearchService {
    private List<IWPTableSearchService> tableSearchServices;

    public WPCombinedTableSearchService(List<IWPTableSearchService> tableSearchServices) {
        this.tableSearchServices = tableSearchServices == null ? Collections.emptyList() : tableSearchServices;
    }

    @Override
    public List<TableBean> listAll() {
        List<TableBean> tables = new ArrayList<>();
        for (IWPTableSearchService service : tableSearchServices) {
            if (service == null) {
                continue;
            }
            List<TableBean> serviceTables = service.listAll();
            if (serviceTables != null && !serviceTables.isEmpty()) {
                tables.addAll(serviceTables);
            }
        }
        return tables;
    }

    @Override
    public TableBean findTableByFullName(String tableFullName) {
        for (IWPTableSearchService service : tableSearchServices) {
            if (service == null) {
                continue;
            }
            TableBean table = service.findTableByFullName(tableFullName);
            if (table != null) {
                return table;
            }
        }
        return null;
    }

    @Override
    public TableBean findTableByName(String tableName) {
        for (IWPTableSearchService service : tableSearchServices) {
            if (service == null) {
                continue;
            }
            TableBean table = service.findTableByName(tableName);
            if (table != null) {
                return table;
            }
        }
        return null;
    }

    @Override
    public FieldBean findFieldByFullName(String tableFullName, String fieldFullName) {
        for (IWPTableSearchService service : tableSearchServices) {
            if (service == null) {
                continue;
            }
            FieldBean field = service.findFieldByFullName(tableFullName, fieldFullName);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    @Override
    public FieldBean findFieldByName(String tableName, String fieldName) {
        for (IWPTableSearchService service : tableSearchServices) {
            if (service == null) {
                continue;
            }
            FieldBean field = service.findFieldByName(tableName, fieldName);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

}