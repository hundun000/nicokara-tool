package com.github.dnbn.submerge.api.subtitle.common;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexText {

    List<ComplexTextNode> nodes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplexTextNode {
        String text;



    }


    public static ComplexText fromText(String text) {
        return ComplexText.builder()
                .nodes(List.of(
                        ComplexTextNode.builder()
                                .text(text)
                                .build()
                ))
                .build();
    }

}
