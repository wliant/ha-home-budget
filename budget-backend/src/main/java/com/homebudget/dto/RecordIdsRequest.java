package com.homebudget.dto;

import java.util.List;

public class RecordIdsRequest {

    private List<Long> recordIds;

    public List<Long> getRecordIds() {
        return recordIds;
    }

    public void setRecordIds(List<Long> recordIds) {
        this.recordIds = recordIds;
    }
}
