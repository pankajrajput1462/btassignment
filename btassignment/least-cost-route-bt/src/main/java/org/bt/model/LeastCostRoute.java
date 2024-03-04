package org.bt.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeastCostRoute {

    private String route;
    private Long price;
}
