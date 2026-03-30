package com.emailagent.dto.response.dashboard;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WeeklySummaryResponse extends BaseResponse {

    @JsonProperty("date_range")
    private DateRange dateRange;

    private List<CategoryStat> categories;

    @Getter
    @Builder
    public static class DateRange {
        private String start;
        private String end;
    }

    @Getter
    @Builder
    public static class CategoryStat {

        @JsonProperty("category_name")
        private String categoryName;

        private long count;
        private String color;
    }
}
