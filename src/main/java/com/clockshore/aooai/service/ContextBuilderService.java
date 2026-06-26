package com.clockshore.aooai.service;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContextBuilderService {
    public String build(
            List<Document> docs
    ) {

        StringBuilder context =
                new StringBuilder();

        for (Document doc : docs) {

            context.append("""
                    [参考资料]
                    """);

            if (doc.getMetadata()
                    .containsKey("pageNo")) {

                context.append(
                        "页码："
                ).append(
                        doc.getMetadata()
                                .get("pageNo")
                ).append("\n");
            }

            context.append(
                    doc.getText()
            );

            context.append("\n\n");
        }

        return context.toString();
    }
}
