package org.bt.model;


import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NetworkElement {
    @CsvBindByName(column = "Name")
    private String name;

    @CsvBindByName(column = "Processing Time (in ms)")
    private Long processingTime;

    @CsvBindByName(column = "Exchange")
    private String exchange;

}
